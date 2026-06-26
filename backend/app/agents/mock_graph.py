"""Mock generation pipeline.

Runs the real staged shape from docs/04 — plan -> svg -> narration -> validate
(+ bounded repair loop) -> tts render -> assemble — but with mock providers, so it
needs no AI keys. Phase 3 replaces this orchestration with a LangGraph StateGraph
that calls the same providers and validator.
"""
import asyncio

from app.agents.base import GenerationGraph, ProgressCb
from app.agents.validator import validate
from app.core.config import Settings
from app.core.errors import GenerationFailedError
from app.domain.generation import GenerationOptions
from app.domain.models import Lesson
from app.providers.base import LLMProvider, TTSProvider
from app.repositories.base import AssetRepository
from app.services.lesson_builder import LessonBuilder


async def _report(progress: ProgressCb, stage: str, pct: int) -> None:
    if progress is not None:
        await progress(stage, pct)


class MockGenerationGraph(GenerationGraph):
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

    async def generate(
        self,
        topic: str,
        topic_key: str,
        options: GenerationOptions,
        progress: ProgressCb = None,
    ) -> Lesson:
        # 1. plan
        await _report(progress, "planning", 10)
        plan = await self._llm.plan_concepts(topic, options.language)
        await asyncio.sleep(0)

        # 2. svg
        await _report(progress, "svg", 35)
        svg = await self._llm.generate_svg(topic, plan)

        # 2b. critique -> refine loop (bounded) to raise SVG quality
        await _report(progress, "critiquing", 45)
        critique = await self._llm.critique_svg(topic, plan, svg)
        refines = 0
        while (
            not critique.acceptable(self._settings.svg_quality_threshold)
            and refines < self._settings.svg_refine_iterations
        ):
            refines += 1
            await _report(progress, "refining_svg", 48)
            svg = await self._llm.refine_svg(topic, plan, svg, critique)
            critique = await self._llm.critique_svg(topic, plan, svg)

        # 3. narration
        await _report(progress, "narration", 55)
        drafts = await self._llm.generate_narration(topic, plan, svg)

        # 4. validate (+ bounded repair loop) — the reliability gate
        await _report(progress, "validating", 60)
        result = validate(svg, drafts)
        retries = 0
        while not result.ok and retries < self._settings.max_repair_retries:
            retries += 1
            svg, drafts = await self._llm.repair(svg, drafts, result.errors)
            result = validate(svg, drafts)
        if not result.ok:
            raise GenerationFailedError(
                "Validation failed: " + "; ".join(result.errors[:3])
            )

        # 5. tts render per segment
        builder = LessonBuilder(
            topic=topic,
            topic_key=topic_key,
            language=options.language,
            voice=options.voice,
        ).with_title(plan.title)

        total = len(drafts)
        for i, draft in enumerate(drafts):
            clip = await self._tts.synthesize(draft.text, options.voice)
            asset = await self._assets.put(clip.data, clip.content_type, kind="audio")
            builder.add_segment(
                index=i,
                text=draft.text,
                svg_element_ids=draft.svg_element_ids,
                audio_asset_id=asset.id,
                duration_ms=clip.duration_ms,
            )
            await _report(progress, "tts_rendering", 70 + int(20 * (i + 1) / total))

        # 6. assemble: store the svg as an asset and build the lesson
        await _report(progress, "assembling", 95)
        svg_asset = await self._assets.put(
            svg.encode("utf-8"), "image/svg+xml", kind="svg"
        )
        lesson = builder.with_svg(svg, svg_asset.id).build()
        await _report(progress, "assembling", 100)
        return lesson
