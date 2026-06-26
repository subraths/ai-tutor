"""Prompt templates for the Gemini LLM adapter.

Kept separate from the adapter so prompt wording can be iterated without touching
SDK/orchestration code. The two hard contracts the rest of the system relies on:
  * every meaningful SVG element gets a stable, semantic id;
  * narration may only reference ids that exist in the SVG.
"""
import json

from app.domain.generation import ConceptPlan, SegmentDraft, SvgCritique

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
- Include a viewBox sized generously to the content (e.g. "0 0 1600 900", a
  ~16:9 canvas) and keep a clear margin of at least 40 units inside all edges.
- Do NOT include <script> or external references (no external images or fonts).
- Give EVERY meaningful visual element a stable, semantic id in lower-kebab-case
  (e.g. id="sun", id="leaf", id="arrow-1"). These ids will be referenced by the
  narration, so they must be descriptive and stable.

Layout rules (CRITICAL — the diagram must be uncluttered; NOTHING may overlap that
is not meant to):
- Give each concept its OWN rectangular region. Regions must not overlap; leave a
  gap of at least 40 units between neighbouring regions.
- Arrange regions in a clean reading order — left-to-right (wrap to a new row when
  needed) or top-to-bottom — with consistent spacing.
- TEXT must never overlap other text, and must not sit on top of another element's
  content:
  * Put each label INSIDE its own box (centered, with padding) or directly
    above/below it — never across a neighbouring element.
  * Keep labels short. If a label is long, lower its font-size or split it over
    several lines with <tspan> (using dy) so it fits within its region's width.
  * Estimate text width as ~0.6 * font-size * (number of characters), and choose
    font-size and x/y so each text box stays fully inside its region and clear of
    every other text and shape. Leave >= 8 units between separate lines/labels.
- Step/section titles go ABOVE their region with clear spacing; a title must not
  overlap any box, arrow, or other title.
- Route connectors (arrows/lines) through the empty gaps BETWEEN regions, not over
  labels or boxes. Define arrowheads with a <marker> in <defs> if useful.
- Every element must lie fully INSIDE the viewBox — nothing clipped or spilling out.
- Prefer whitespace over density: a larger canvas with room to breathe is better
  than cramming elements together.
- Use legible colours with good contrast on a light/neutral background."""

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

Return the corrected SVG and the corrected segments. Requirements:
- The SVG must be well-formed and include a viewBox, and every id referenced by a
  segment must exist in the SVG (preserve existing ids wherever possible).
- Also clean up the LAYOUT so the diagram is readable: no overlapping text, no text
  sitting on top of another element, no shapes overlapping that should not, and
  nothing spilling outside the viewBox. Keep clear gaps between regions and put each
  label inside or directly beside its own element. Enlarge the viewBox or reposition
  elements as needed, but keep the ids and the meaning intact."""


_CRITIQUE = """You are a meticulous design reviewer for educational diagrams.
Critically evaluate the SVG diagram for this lesson.

Topic: {topic}
Concepts that must ALL be depicted, in order:
{concepts}

SVG:
{svg}

Score the diagram 0-10 for overall quality, judging:
- Completeness: every concept above is clearly represented.
- Layout: NO overlapping text or shapes; nothing clipped or outside the viewBox;
  generous spacing; a clear reading order.
- Legibility: readable labels, good contrast, sensible font sizes.
- Clarity: arrows/connectors make the relationships obvious.

List the specific, concrete problems to fix (an empty list if there are none). Be
strict: any overlap, clipping, or unreadable label means a score of 6 or below."""

_REFINE = """Improve this SVG diagram to fix the reviewer's issues. Produce a
genuinely BETTER diagram (not a minimal patch), keeping the same concepts and
stable, semantic ids.

Topic: {topic}
Concepts to depict, in order:
{concepts}

Reviewer score: {score}/10
Issues to fix:
{issues}

Current SVG:
{svg}

Output ONLY the improved SVG (starting with <svg ...> and ending with </svg>).
Keep a viewBox; no <script> or external references. Apply all layout rules: no
overlapping text or shapes, nothing clipped or outside the viewBox, generous
spacing, legible labels, every concept clearly shown, and a stable, semantic id
on every meaningful element."""


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


def critique_prompt(topic: str, plan: ConceptPlan, svg: str) -> str:
    concepts = "\n".join(f"  {i + 1}. {c}" for i, c in enumerate(plan.concepts))
    return _CRITIQUE.format(topic=topic, concepts=concepts, svg=svg)


def refine_prompt(topic: str, plan: ConceptPlan, svg: str, critique: SvgCritique) -> str:
    concepts = "\n".join(f"  {i + 1}. {c}" for i, c in enumerate(plan.concepts))
    issues = "\n".join(f"- {x}" for x in critique.issues) or "- (raise overall quality)"
    return _REFINE.format(
        topic=topic, concepts=concepts, score=critique.score, issues=issues, svg=svg
    )
