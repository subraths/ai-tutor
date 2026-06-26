"""LangGraph implementation of the GenerationGraph port (Phase 3).

Mirrors docs/04: planner -> svg -> narration -> validate, with a conditional
**repair loop** back into validate (bounded by max_repair_retries), then render
-> assemble. It orchestrates the same LLM/TTS provider ports and the same
deterministic validator as the mock engine — only the orchestration is real here.

Progress is delivered via the per-invocation callback passed through the run
config (the compiled graph is shared across concurrent jobs, so nothing per-call
is stored on the instance).
"""
import logging

from langchain_core.runnables import RunnableConfig
from langgraph.graph import END, START, StateGraph

from app.agents.base import GenerationGraph, ProgressCb
from app.agents.state import GenerationState, RenderedSegment
from app.agents.validator import validate
from app.core.config import Settings
from app.core.errors import GenerationFailedError
from app.domain.generation import GenerationOptions
from app.domain.models import Lesson
from app.providers.base import LLMProvider, TTSProvider
from app.repositories.base import AssetRepository
from app.services.lesson_builder import LessonBuilder

log = logging.getLogger("tutor.graph")


async def _report(config: RunnableConfig | None, stage: str, pct: int) -> None:
    cb = (config or {}).get("configurable", {}).get("progress")
    if cb is not None:
        await cb(stage, pct)


class LangGraphGenerationGraph(GenerationGraph):
    def __init__(
        self,
        llm: LLMProvider,
        tts: TTSProvider,
        assets: AssetRepository,
        settings: Settings,
    ) -> None:
        self._llm = llm
        self._tts = tts
        self._assets = assets
        self._settings = settings
        self._compiled = self._build()

    # ----- graph wiring -----
    def _build(self):
        b = StateGraph(GenerationState)
        b.add_node("planner", self._planner)
        b.add_node("svg", self._svg)
        b.add_node("critique", self._critique)
        b.add_node("refine", self._refine)
        b.add_node("narration", self._narration)
        b.add_node("validate", self._validate)
        b.add_node("repair", self._repair)
        b.add_node("render", self._render)
        b.add_node("assemble", self._assemble)
        b.add_node("fail", self._fail)

        b.add_edge(START, "planner")
        b.add_edge("planner", "svg")
        # SVG quality loop: svg -> critique -> (refine -> critique)* -> narration
        b.add_edge("svg", "critique")
        b.add_conditional_edges(
            "critique",
            self._route_after_critique,
            {"refine": "refine", "narration": "narration"},
        )
        b.add_edge("refine", "critique")
        b.add_edge("narration", "validate")
        b.add_conditional_edges(
            "validate",
            self._route_after_validate,
            {"render": "render", "repair": "repair", "fail": "fail"},
        )
        b.add_edge("repair", "validate")
        b.add_edge("render", "assemble")
        b.add_edge("assemble", END)
        b.add_edge("fail", END)
        return b.compile()

    # ----- nodes -----
    async def _planner(self, state: GenerationState, config: RunnableConfig | None = None) -> dict:
        await _report(config, "planning", 10)
        plan = await self._llm.plan_concepts(state["topic"], state["options"].language)
        return {"plan": plan, "retries": 0}

    async def _svg(self, state: GenerationState, config: RunnableConfig | None = None) -> dict:
        await _report(config, "svg", 35)
        svg = await self._llm.generate_svg(state["topic"], state["plan"])
        return {"svg": svg, "svg_refines": 0}

    async def _critique(self, state: GenerationState, config: RunnableConfig | None = None) -> dict:
        await _report(config, "critiquing", 45)
        critique = await self._llm.critique_svg(state["topic"], state["plan"], state["svg"])
        return {"critique": critique}

    def _route_after_critique(self, state: GenerationState) -> str:
        critique = state["critique"]
        if critique.acceptable(self._settings.svg_quality_threshold):
            return "narration"
        if state.get("svg_refines", 0) < self._settings.svg_refine_iterations:
            return "refine"
        return "narration"  # out of budget: proceed with the best SVG so far

    async def _refine(self, state: GenerationState, config: RunnableConfig | None = None) -> dict:
        refines = state.get("svg_refines", 0) + 1
        await _report(config, "refining_svg", 48)
        log.info("svg refine pass %d (score %d) for %s",
                 refines, state["critique"].score, state["topic_key"])
        svg = await self._llm.refine_svg(
            state["topic"], state["plan"], state["svg"], state["critique"]
        )
        return {"svg": svg, "svg_refines": refines}

    async def _narration(self, state: GenerationState, config: RunnableConfig | None = None) -> dict:
        await _report(config, "narration", 55)
        drafts = await self._llm.generate_narration(
            state["topic"], state["plan"], state["svg"]
        )
        return {"drafts": drafts}

    async def _validate(self, state: GenerationState, config: RunnableConfig | None = None) -> dict:
        await _report(config, "validating", 60)
        return {"validation": validate(state["svg"], state["drafts"])}

    def _route_after_validate(self, state: GenerationState) -> str:
        if state["validation"].ok:
            return "render"
        if state.get("retries", 0) < self._settings.max_repair_retries:
            return "repair"
        return "fail"

    async def _repair(self, state: GenerationState, config: RunnableConfig | None = None) -> dict:
        retries = state.get("retries", 0) + 1
        await _report(config, "repairing", 60)
        log.info("repair attempt %d for %s", retries, state["topic_key"])
        svg, drafts = await self._llm.repair(
            state["svg"], state["drafts"], state["validation"].errors
        )
        return {"svg": svg, "drafts": drafts, "retries": retries}

    async def _render(self, state: GenerationState, config: RunnableConfig | None = None) -> dict:
        drafts = state["drafts"]
        voice = state["options"].voice
        total = len(drafts)
        rendered: list[RenderedSegment] = []
        for i, draft in enumerate(drafts):
            clip = await self._tts.synthesize(draft.text, voice)
            asset = await self._assets.put(clip.data, clip.content_type, kind="audio")
            rendered.append(
                RenderedSegment(
                    index=i,
                    text=draft.text,
                    svg_element_ids=draft.svg_element_ids,
                    audio_asset_id=asset.id,
                    duration_ms=clip.duration_ms,
                )
            )
            await _report(config, "tts_rendering", 70 + int(20 * (i + 1) / total))
        return {"rendered": rendered}

    async def _assemble(self, state: GenerationState, config: RunnableConfig | None = None) -> dict:
        await _report(config, "assembling", 95)
        svg_asset = await self._assets.put(
            state["svg"].encode("utf-8"), "image/svg+xml", kind="svg"
        )
        builder = (
            LessonBuilder(
                topic=state["topic"],
                topic_key=state["topic_key"],
                language=state["options"].language,
                voice=state["options"].voice,
            )
            .with_title(state["plan"].title)
            .with_svg(state["svg"], svg_asset.id)
        )
        for r in state["rendered"]:
            builder.add_segment(
                index=r.index,
                text=r.text,
                svg_element_ids=r.svg_element_ids,
                audio_asset_id=r.audio_asset_id,
                duration_ms=r.duration_ms,
            )
        lesson = builder.build()
        await _report(config, "assembling", 100)
        return {"lesson": lesson}

    async def _fail(self, state: GenerationState, config: RunnableConfig | None = None) -> dict:
        errors = state["validation"].errors if state.get("validation") else []
        return {"error": "Validation failed: " + "; ".join(errors[:3])}

    # ----- port entry point -----
    async def generate(
        self,
        topic: str,
        topic_key: str,
        options: GenerationOptions,
        progress: ProgressCb = None,
    ) -> Lesson:
        state: GenerationState = {
            "topic": topic,
            "topic_key": topic_key,
            "options": options,
            "retries": 0,
        }
        config = {"configurable": {"progress": progress}, "recursion_limit": 50}
        result = await self._compiled.ainvoke(state, config=config)
        lesson = result.get("lesson")
        if lesson is None:
            raise GenerationFailedError(result.get("error") or "Graph produced no lesson")
        return lesson
