"""API DTOs (request/response). The wire contract from docs/06-api-contract.md."""
from datetime import datetime

from pydantic import BaseModel


# --- requests ---
class GenerateLessonRequest(BaseModel):
    topic: str
    language: str = "en"
    voice: str | None = None


# --- responses ---
class AssetRefOut(BaseModel):
    asset_id: str
    url: str


class SegmentOut(BaseModel):
    index: int
    text: str
    svg_element_ids: list[str]
    audio: AssetRefOut
    duration_ms: int


class LessonManifest(BaseModel):
    id: str
    topic: str
    title: str
    language: str
    voice: str
    total_duration_ms: int
    created_at: datetime
    svg: AssetRefOut
    segments: list[SegmentOut]


class JobAccepted(BaseModel):
    job_id: str
    status: str
    topic_key: str


class JobStatusOut(BaseModel):
    job_id: str
    status: str
    progress: int
    stage: str | None = None
    lesson_id: str | None = None
    error: str | None = None


class ErrorBody(BaseModel):
    code: str
    message: str
    retryable: bool
    request_id: str | None = None


class ErrorResponse(BaseModel):
    error: ErrorBody
