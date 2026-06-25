"""The reliability gate (no LLM, fully deterministic).

A lesson is never cached unless it passes here. The critical invariant: every SVG
element id referenced by a narration segment must actually exist in the SVG — that
is what makes highlighting trustworthy on the device.
"""
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field

from app.domain.generation import SegmentDraft


@dataclass
class ValidationResult:
    ok: bool
    errors: list[str] = field(default_factory=list)


def extract_ids(svg: str) -> set[str]:
    try:
        root = ET.fromstring(svg)
    except ET.ParseError:
        return set()
    return {el.get("id") for el in root.iter() if el.get("id")}


def validate_svg(svg: str) -> list[str]:
    try:
        root = ET.fromstring(svg)
    except ET.ParseError as exc:
        return [f"SVG is not well-formed XML: {exc}"]
    errors: list[str] = []
    tag = root.tag.split("}")[-1]
    if tag != "svg":
        errors.append(f"Root element is <{tag}>, expected <svg>")
    if "viewBox" not in root.attrib:
        errors.append("SVG root is missing a viewBox")
    return errors


def validate(svg: str, segments: list[SegmentDraft]) -> ValidationResult:
    errors = validate_svg(svg)
    ids = extract_ids(svg)

    if not segments:
        errors.append("Lesson has no narration segments")

    for i, seg in enumerate(segments):
        if not seg.text.strip():
            errors.append(f"Segment {i} has empty text")
        for ref in seg.svg_element_ids:
            if ref not in ids:
                errors.append(f"Segment {i} references missing SVG id '{ref}'")

    return ValidationResult(ok=not errors, errors=errors)
