"""Value objects used during generation, plus topic normalization.

These are the intermediate, pre-audio artifacts the agent graph passes around
(distinct from the final persisted Lesson in models.py).
"""
from pydantic import BaseModel, Field

from app.core.errors import InvalidTopicError

DEFAULT_VOICE = "en-US-neutral"
MAX_TOPIC_LEN = 200


class GenerationOptions(BaseModel):
    language: str = "en"
    voice: str = DEFAULT_VOICE


class ConceptPlan(BaseModel):
    """Output of the planner node: title + the ordered concepts to visualize."""
    title: str
    concepts: list[str]


class SegmentDraft(BaseModel):
    """A narration segment before TTS — text + the SVG ids it should highlight."""
    text: str
    svg_element_ids: list[str] = Field(default_factory=list)


class SvgCritique(BaseModel):
    """A design review of a generated SVG (the output of the critique model).

    Drives the agentic critique -> refine loop: while the score is below the
    configured threshold (and the iteration budget remains), the SVG is refined.
    """
    score: int = Field(ge=0, le=10, description="overall visual quality, 0-10")
    issues: list[str] = Field(
        default_factory=list, description="concrete, specific problems to fix"
    )

    def acceptable(self, threshold: int) -> bool:
        return self.score >= threshold


def normalize_topic(topic: str, language: str = "en") -> str:
    """Return the cache/idempotency key for a topic, or raise InvalidTopicError."""
    key = " ".join((topic or "").strip().lower().split())
    if not key:
        raise InvalidTopicError("Topic must not be empty.")
    if len(key) > MAX_TOPIC_LEN:
        raise InvalidTopicError("Topic is too long.")
    return f"{key}|{language}"
