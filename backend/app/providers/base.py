"""Provider ports (Strategy pattern).

These are the seams that isolate the rest of the app from any specific AI vendor.
The Gemini adapters in Phase 4 implement the same interfaces; prompt construction
lives inside each adapter, so the agent graph only orchestrates calls + validation.
"""
from abc import ABC, abstractmethod
from dataclasses import dataclass

from app.domain.generation import ConceptPlan, SegmentDraft, SvgCritique


@dataclass(frozen=True)
class AudioClip:
    data: bytes
    content_type: str
    duration_ms: int


class LLMProvider(ABC):
    """Task-oriented port over the language model."""

    @abstractmethod
    async def plan_concepts(self, topic: str, language: str = "en") -> ConceptPlan:
        ...

    @abstractmethod
    async def generate_svg(self, topic: str, plan: ConceptPlan) -> str:
        ...

    @abstractmethod
    async def generate_narration(
        self, topic: str, plan: ConceptPlan, svg: str
    ) -> list[SegmentDraft]:
        ...

    @abstractmethod
    async def repair(
        self, svg: str, segments: list[SegmentDraft], errors: list[str]
    ) -> tuple[str, list[SegmentDraft]]:
        """Given validation errors, return corrected (svg, segments)."""
        ...

    # --- SVG quality (agentic critique -> refine loop) ---
    # Concrete no-op defaults so a provider opts in only when it has a critique
    # model. Providers that support it (Gemini) override both.

    async def critique_svg(
        self, topic: str, plan: ConceptPlan, svg: str
    ) -> SvgCritique:
        """Review an SVG's visual quality. Default: accept as-is (no loop)."""
        return SvgCritique(score=10, issues=[])

    async def refine_svg(
        self, topic: str, plan: ConceptPlan, svg: str, critique: SvgCritique
    ) -> str:
        """Return an improved SVG addressing the critique. Default: unchanged."""
        return svg


class TTSProvider(ABC):
    """Port over text-to-speech."""

    @abstractmethod
    async def synthesize(self, text: str, voice: str = "en-US-neutral") -> AudioClip:
        ...
