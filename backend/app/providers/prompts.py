"""Prompt templates for the Gemini LLM adapter.

Kept separate from the adapter so prompt wording can be iterated without touching
SDK/orchestration code. The two hard contracts the rest of the system relies on:
  * every meaningful SVG element gets a stable, semantic id;
  * narration may only reference ids that exist in the SVG.
"""
import json

from app.domain.generation import ConceptPlan, SegmentDraft

_PLAN = """You are an expert tutor and educational illustrator.
Plan a short, visual lesson for the topic below.

Topic: {topic}
Language: {language}

Return a concise lesson title and an ordered list of 3 to 6 key concepts. Each
concept should be something that can be drawn as a single labeled element in a
simple diagram, in the order they should be explained."""

_SVG = """Create a single, self-contained SVG diagram for this lesson.

Topic: {topic}
Title: {title}
Concepts to depict, in order:
{concepts}

Hard requirements:
- Output ONLY the SVG markup (starting with <svg ...> and ending with </svg>).
- Include a viewBox. Do NOT include <script> or external references.
- Give EVERY meaningful visual element a stable, semantic id in lower-kebab-case
  (e.g. id="sun", id="leaf", id="arrow-1"). These ids will be referenced by the
  narration, so they must be descriptive and stable.
- Keep it clean and legible; label important parts with <text> elements."""

_NARRATION = """Write the spoken narration for this lesson as an ordered list of
short segments.

Topic: {topic}
SVG (with element ids):
{svg}

Rules:
- 4 to 8 segments. Each segment is 1-2 sentences of natural spoken explanation.
- For each segment, list the svg element ids it is talking about.
- CRITICAL: only use ids that actually appear in the SVG above. Never invent ids.
- The segments together should walk the learner through the whole diagram."""

_REPAIR = """A generated lesson failed validation. Fix it with MINIMAL changes.

Validation errors:
{errors}

Current SVG:
{svg}

Current segments (JSON):
{segments}

Return the corrected SVG and the corrected segments. Ensure the SVG is well-formed
with a viewBox, and that every id referenced by a segment exists in the SVG."""


def plan_prompt(topic: str, language: str) -> str:
    return _PLAN.format(topic=topic, language=language)


def svg_prompt(topic: str, plan: ConceptPlan) -> str:
    concepts = "\n".join(f"  {i + 1}. {c}" for i, c in enumerate(plan.concepts))
    return _SVG.format(topic=topic, title=plan.title, concepts=concepts)


def narration_prompt(topic: str, plan: ConceptPlan, svg: str) -> str:
    return _NARRATION.format(topic=topic, svg=svg)


def repair_prompt(svg: str, segments: list[SegmentDraft], errors: list[str]) -> str:
    seg_json = json.dumps([s.model_dump() for s in segments], indent=2)
    err_text = "\n".join(f"- {e}" for e in errors)
    return _REPAIR.format(errors=err_text, svg=svg, segments=seg_json)
