"""Graph-level tests (no HTTP)."""
from app.agents.mock_graph import MockGenerationGraph
from app.agents.validator import validate
from app.core.config import Settings
from app.domain.generation import GenerationOptions, SegmentDraft
from app.providers.mock import MockLLMProvider, MockTTSProvider
from app.repositories.memory import InMemoryAssetRepository


async def test_mock_graph_produces_valid_lesson():
    settings = Settings()
    assets = InMemoryAssetRepository(settings.asset_base_url)
    graph = MockGenerationGraph(
        MockLLMProvider(settings), MockTTSProvider(settings), assets, settings
    )

    lesson = await graph.generate(
        "Photosynthesis", "photosynthesis|en", GenerationOptions()
    )

    assert lesson.segments, "lesson should have segments"
    # The core invariant: the assembled lesson passes the validator gate.
    drafts = [
        SegmentDraft(text=s.text, svg_element_ids=s.svg_element_ids)
        for s in lesson.segments
    ]
    assert validate(lesson.svg, drafts).ok

    # total duration is the sum of segment durations
    assert lesson.total_duration_ms == sum(s.duration_ms for s in lesson.segments)

    # every audio asset (and the svg asset) is retrievable
    assert await assets.get(lesson.svg_asset_id) is not None
    for seg in lesson.segments:
        assert await assets.get(seg.audio_asset_id) is not None


async def test_progress_callback_reaches_100():
    settings = Settings()
    assets = InMemoryAssetRepository(settings.asset_base_url)
    graph = MockGenerationGraph(
        MockLLMProvider(settings), MockTTSProvider(settings), assets, settings
    )

    seen: list[tuple[str, int]] = []

    async def progress(stage: str, pct: int) -> None:
        seen.append((stage, pct))

    await graph.generate("Gravity", "gravity|en", GenerationOptions(), progress)

    assert seen, "progress should be reported"
    assert seen[-1][1] == 100
    assert {"planning", "svg", "narration", "validating", "tts_rendering"} <= {
        s for s, _ in seen
    }
