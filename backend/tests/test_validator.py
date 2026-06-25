"""The reliability gate — pure, no app needed."""
from app.agents.validator import extract_ids, validate, validate_svg
from app.domain.generation import SegmentDraft

GOOD_SVG = (
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10">'
    '<rect id="a"/><rect id="b"/></svg>'
)


def test_extract_ids():
    assert extract_ids(GOOD_SVG) == {"a", "b"}


def test_validate_ok():
    segs = [SegmentDraft(text="hello", svg_element_ids=["a", "b"])]
    assert validate(GOOD_SVG, segs).ok


def test_validate_rejects_missing_id():
    segs = [SegmentDraft(text="hello", svg_element_ids=["nope"])]
    res = validate(GOOD_SVG, segs)
    assert not res.ok
    assert any("nope" in e for e in res.errors)


def test_validate_rejects_malformed_svg():
    res = validate("<svg><rect></svg", [SegmentDraft(text="x", svg_element_ids=[])])
    assert not res.ok


def test_validate_requires_viewbox():
    svg = '<svg xmlns="http://www.w3.org/2000/svg"><rect id="a"/></svg>'
    res = validate(svg, [SegmentDraft(text="x", svg_element_ids=["a"])])
    assert not res.ok
    assert any("viewBox" in e for e in res.errors)


def test_validate_rejects_empty_segments():
    assert not validate(GOOD_SVG, []).ok
