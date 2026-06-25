"""Core domain entities: Lesson, Segment, AssetRef, Job.

Invariants (enforced at build/validation time, see services/lesson_builder.py and
agents/validator.py):
  * Lesson.svg is well-formed XML with a viewBox.
  * Every Segment.svg_element_ids value exists as an id in Lesson.svg.
  * total_duration_ms == sum(segment.duration_ms).
  * Segments are contiguous and ordered by index starting at 0.
"""
from datetime import datetime, timezone

from pydantic import BaseModel, Field


def utcnow() -> datetime:
    return datetime.now(timezone.utc)


class AssetRef(BaseModel):
    id: str
    kind: str  # "svg" | "audio"
    content_type: str
    url: str


class Segment(BaseModel):
    index: int
    text: str
    svg_element_ids: list[str]
    audio_asset_id: str
    duration_ms: int


class Lesson(BaseModel):
    id: str
    topic: str
    topic_key: str
    title: str
    language: str = "en"
    voice: str = "en-US-neutral"
    svg: str
    svg_asset_id: str
    segments: list[Segment]
    total_duration_ms: int
    created_at: datetime = Field(default_factory=utcnow)


class Job(BaseModel):
    id: str
    topic_key: str
    status: str = "queued"  # queued | running | succeeded | failed
    progress: int = 0
    stage: str | None = None
    lesson_id: str | None = None
    error: str | None = None
    created_at: datetime = Field(default_factory=utcnow)
