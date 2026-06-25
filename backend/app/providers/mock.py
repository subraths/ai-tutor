"""Deterministic mock providers — let the whole pipeline run with no AI keys.

The mock LLM emits a generic 3-stage process diagram with stable ids
(title, stage-1..3, arrow-1..2) and narration that references only those ids, so
the validator gate always passes on the happy path.
"""
from xml.sax.saxutils import escape

from app.core.config import Settings
from app.domain.generation import ConceptPlan, SegmentDraft
from app.providers.base import AudioClip, LLMProvider, TTSProvider


def _build_svg(title: str, c1: str, c2: str, c3: str) -> str:
    t, a, b, c = (escape(s) for s in (title, c1, c2, c3))
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 320" '
        'role="img" aria-label="' + t + '">'
        '<title id="title">' + t + '</title>'
        '<rect id="bg" x="0" y="0" width="600" height="320" fill="#f7f9fc"/>'
        '<rect id="stage-1" x="20" y="130" width="150" height="70" rx="10" '
        'fill="#cfe3ff" stroke="#3b6fb0"/>'
        '<text id="label-1" x="95" y="170" text-anchor="middle">' + a + '</text>'
        '<line id="arrow-1" x1="170" y1="165" x2="225" y2="165" '
        'stroke="#333" stroke-width="3"/>'
        '<rect id="stage-2" x="225" y="130" width="150" height="70" rx="10" '
        'fill="#d6f5d6" stroke="#3b9b5a"/>'
        '<text id="label-2" x="300" y="170" text-anchor="middle">' + b + '</text>'
        '<line id="arrow-2" x1="375" y1="165" x2="430" y2="165" '
        'stroke="#333" stroke-width="3"/>'
        '<rect id="stage-3" x="430" y="130" width="150" height="70" rx="10" '
        'fill="#ffe6cc" stroke="#c97a2b"/>'
        '<text id="label-3" x="505" y="170" text-anchor="middle">' + c + '</text>'
        '</svg>'
    )


class MockLLMProvider(LLMProvider):
    def __init__(self, settings: Settings):
        self._settings = settings

    async def plan_concepts(self, topic: str, language: str = "en") -> ConceptPlan:
        return ConceptPlan(
            title=f"How {topic} Works",
            concepts=[
                f"{topic}: the starting point",
                f"{topic}: the process",
                f"{topic}: the result",
            ],
        )

    async def generate_svg(self, topic: str, plan: ConceptPlan) -> str:
        c = plan.concepts
        return _build_svg(plan.title, c[0], c[1], c[2])

    async def generate_narration(
        self, topic: str, plan: ConceptPlan, svg: str
    ) -> list[SegmentDraft]:
        c = plan.concepts
        return [
            SegmentDraft(text=f"Let's explore {topic}.", svg_element_ids=["title"]),
            SegmentDraft(text=f"First, {c[0]}.", svg_element_ids=["stage-1", "label-1"]),
            SegmentDraft(
                text=f"Next, {c[1]}.",
                svg_element_ids=["stage-1", "arrow-1", "stage-2", "label-2"],
            ),
            SegmentDraft(
                text=f"Finally, {c[2]}.",
                svg_element_ids=["stage-2", "arrow-2", "stage-3", "label-3"],
            ),
            SegmentDraft(
                text="And that's the whole picture.", svg_element_ids=["stage-3"]
            ),
        ]

    async def repair(
        self, svg: str, segments: list[SegmentDraft], errors: list[str]
    ) -> tuple[str, list[SegmentDraft]]:
        # The mock is always valid; a real adapter would re-prompt the model with
        # the error list. Echo inputs back unchanged.
        return svg, segments


class MockTTSProvider(TTSProvider):
    def __init__(self, settings: Settings):
        self._settings = settings

    async def synthesize(self, text: str, voice: str = "en-US-neutral") -> AudioClip:
        words = max(1, len(text.split()))
        duration_ms = max(800, words * self._settings.tts_ms_per_word)
        # Placeholder bytes; a real adapter returns encoded audio.
        data = b"MOCKAUDIO:" + text.encode("utf-8")
        return AudioClip(data=data, content_type="audio/mpeg", duration_ms=duration_ms)
