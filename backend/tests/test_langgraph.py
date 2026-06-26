"""Phase 3 — LangGraph engine: happy path, repair loop, and failure gate."""
import pytest

from app.agents.langgraph_graph import LangGraphGenerationGraph
from app.agents.validator import validate
from app.core.config import Settings
from app.core.errors import GenerationFailedError
from app.domain.generation import (
    ConceptPlan,
    GenerationOptions,
    SegmentDraft,
    SvgCritique,
)
from app.providers.base import LLMProvider
from app.providers.mock import MockLLMProvider, MockTTSProvider
from app.repositories.memory import InMemoryAssetRepository

GOOD_SVG = (
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10">'
    '<rect id="a"/><rect id="b"/></svg>'
)


def _graph(llm, settings=None):
    settings = settings or Settings()
    assets = InMemoryAssetRepository(settings.asset_base_url)
    return (
        LangGraphGenerationGraph(llm, MockTTSProvider(settings), assets, settings),
        assets,
    )


class _BadThenGoodLLM(LLMProvider):
    """Emits narration referencing a missing id once, then repairs to a valid id."""

    def __init__(self) -> None:
        self.repair_calls = 0

    async def plan_concepts(self, topic, language="en"):
        return ConceptPlan(title="T", concepts=["x", "y", "z"])

    async def generate_svg(self, topic, plan):
        return GOOD_SVG

    async def generate_narration(self, topic, plan, svg):
        return [SegmentDraft(text="intro", svg_element_ids=["ghost"])]  # missing id

    async def repair(self, svg, segments, errors):
        self.repair_calls += 1
        return svg, [SegmentDraft(text="fixed", svg_element_ids=["a"])]


class _AlwaysBadLLM(_BadThenGoodLLM):
    async def repair(self, svg, segments, errors):
        self.repair_calls += 1
        return svg, segments  # never fixes it


class _LowQualitySvgLLM(MockLLMProvider):
    """Critique scores low until it has been refined `good_after` times."""

    def __init__(self, settings, good_after: int = 1) -> None:
        super().__init__(settings)
        self.good_after = good_after
        self.critiques = 0
        self.refines = 0

    async def critique_svg(self, topic, plan, svg):
        self.critiques += 1
        if self.refines >= self.good_after:
            return SvgCritique(score=9, issues=[])
        return SvgCritique(score=3, issues=["text overlaps the box"])

    async def refine_svg(self, topic, plan, svg, critique):
        self.refines += 1
        return svg


async def test_happy_path_produces_valid_lesson():
    settings = Settings()
    graph, assets = _graph(MockLLMProvider(settings), settings)

    lesson = await graph.generate("Photosynthesis", "photosynthesis|en", GenerationOptions())

    drafts = [
        SegmentDraft(text=s.text, svg_element_ids=s.svg_element_ids)
        for s in lesson.segments
    ]
    assert validate(lesson.svg, drafts).ok
    assert lesson.total_duration_ms == sum(s.duration_ms for s in lesson.segments)
    assert await assets.get(lesson.svg_asset_id) is not None


async def test_repair_loop_recovers():
    llm = _BadThenGoodLLM()
    graph, _ = _graph(llm)

    lesson = await graph.generate("Topic", "topic|en", GenerationOptions())

    assert llm.repair_calls == 1, "repair should have run exactly once"
    assert lesson.segments[0].svg_element_ids == ["a"]
    drafts = [SegmentDraft(text=s.text, svg_element_ids=s.svg_element_ids) for s in lesson.segments]
    assert validate(lesson.svg, drafts).ok


async def test_exhausted_retries_fail_the_gate():
    settings = Settings()  # max_repair_retries = 2
    llm = _AlwaysBadLLM()
    graph, _ = _graph(llm, settings)

    with pytest.raises(GenerationFailedError):
        await graph.generate("Topic", "topic|en", GenerationOptions())

    assert llm.repair_calls == settings.max_repair_retries


async def test_svg_critique_loop_refines_then_accepts():
    settings = Settings()  # svg_refine_iterations=1, threshold=8
    llm = _LowQualitySvgLLM(settings, good_after=1)
    graph, _ = _graph(llm, settings)

    lesson = await graph.generate("Topic", "topic|en", GenerationOptions())

    assert llm.refines == 1, "a low-scoring SVG should be refined once"
    assert llm.critiques == 2, "critique runs before and after the refine"
    drafts = [SegmentDraft(text=s.text, svg_element_ids=s.svg_element_ids) for s in lesson.segments]
    assert validate(lesson.svg, drafts).ok


async def test_svg_critique_loop_is_bounded():
    settings = Settings(svg_refine_iterations=2)
    llm = _LowQualitySvgLLM(settings, good_after=99)  # never satisfied
    graph, _ = _graph(llm, settings)

    lesson = await graph.generate("Topic", "topic|en", GenerationOptions())

    assert llm.refines == 2, "refinement is capped at svg_refine_iterations"
    # Still produces a valid lesson — it proceeds with the best-effort SVG.
    drafts = [SegmentDraft(text=s.text, svg_element_ids=s.svg_element_ids) for s in lesson.segments]
    assert validate(lesson.svg, drafts).ok


async def test_progress_reaches_100():
    settings = Settings()
    graph, _ = _graph(MockLLMProvider(settings), settings)
    seen: list[tuple[str, int]] = []

    async def progress(stage: str, pct: int) -> None:
        seen.append((stage, pct))

    await graph.generate("Gravity", "gravity|en", GenerationOptions(), progress)

    assert seen[-1][1] == 100
    stages = {s for s, _ in seen}
    assert {"planning", "svg", "narration", "validating", "tts_rendering", "assembling"} <= stages


async def test_api_e2e_with_langgraph_engine():
    """Full HTTP flow with the engine explicitly set to langgraph."""
    from httpx import ASGITransport, AsyncClient

    from app.main import create_app
    from tests.helpers import wait_for_job

    app = create_app(Settings(generation_engine="langgraph"))
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        r = await client.post("/api/v1/lessons", json={"topic": "Photosynthesis"})
        assert r.status_code == 202
        job = await wait_for_job(client, r.json()["job_id"])
        assert job["status"] == "succeeded"
        manifest = (await client.get(f"/api/v1/lessons/{job['lesson_id']}")).json()
        assert manifest["segments"]
