"""Typed state that flows through the generation graph (see docs/04 section 4.4)."""
from dataclasses import dataclass
from typing import TypedDict

from app.agents.validator import ValidationResult
from app.domain.generation import ConceptPlan, GenerationOptions, SegmentDraft, SvgCritique
from app.domain.models import Lesson


@dataclass
class RenderedSegment:
    index: int
    text: str
    svg_element_ids: list[str]
    audio_asset_id: str
    duration_ms: int


class GenerationState(TypedDict, total=False):
    # inputs
    topic: str
    topic_key: str
    options: GenerationOptions
    # produced along the way
    plan: ConceptPlan
    svg: str
    critique: SvgCritique
    svg_refines: int
    drafts: list[SegmentDraft]
    validation: ValidationResult
    retries: int
    rendered: list[RenderedSegment]
    # terminal
    lesson: Lesson
    error: str
