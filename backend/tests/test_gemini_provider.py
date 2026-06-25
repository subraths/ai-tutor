"""Gemini adapters tested against a faked SDK client — no key, no network.

This also validates that our google-genai request configs (GenerateContentConfig,
SpeechConfig, ...) construct against the installed SDK version.
"""
from types import SimpleNamespace

import pytest

from app.core.config import Settings
from app.core.errors import GenerationFailedError, ProviderUnavailableError
from app.domain.generation import ConceptPlan, SegmentDraft
from app.providers.gemini import (
    GeminiLLMProvider,
    GeminiTTSProvider,
    NarrationResponse,
    RepairResponse,
)

SVG = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10"><rect id="a"/></svg>'


class _Resp:
    def __init__(self, parsed=None, text=None, audio=None):
        self.parsed = parsed
        self.text = text
        self.candidates = None
        if audio is not None:
            part = SimpleNamespace(inline_data=SimpleNamespace(data=audio))
            self.candidates = [SimpleNamespace(content=SimpleNamespace(parts=[part]))]


class _Models:
    def __init__(self, resp=None, error=None):
        self._resp = resp
        self._error = error
        self.calls: list[dict] = []

    async def generate_content(self, **kwargs):
        self.calls.append(kwargs)
        if self._error:
            raise self._error
        return self._resp


def _client(resp=None, error=None):
    models = _Models(resp, error)
    return SimpleNamespace(aio=SimpleNamespace(models=models)), models


async def test_plan_concepts_returns_parsed():
    client, _ = _client(_Resp(parsed=ConceptPlan(title="T", concepts=["x", "y"])))
    plan = await GeminiLLMProvider(Settings(), client=client).plan_concepts("Topic")
    assert plan.title == "T" and plan.concepts == ["x", "y"]


async def test_generate_svg_extracts_from_code_fence():
    client, _ = _client(_Resp(text=f"Here you go:\n```svg\n{SVG}\n```"))
    svg = await GeminiLLMProvider(Settings(), client=client).generate_svg(
        "Topic", ConceptPlan(title="T", concepts=["x"])
    )
    assert svg.startswith("<svg") and svg.endswith("</svg>")


async def test_generate_svg_without_svg_fails():
    client, _ = _client(_Resp(text="sorry, no diagram"))
    with pytest.raises(GenerationFailedError):
        await GeminiLLMProvider(Settings(), client=client).generate_svg(
            "Topic", ConceptPlan(title="T", concepts=["x"])
        )


async def test_generate_narration_returns_segments():
    resp = _Resp(parsed=NarrationResponse(
        segments=[SegmentDraft(text="hi", svg_element_ids=["a"])]
    ))
    client, _ = _client(resp)
    segs = await GeminiLLMProvider(Settings(), client=client).generate_narration(
        "Topic", ConceptPlan(title="T", concepts=["x"]), SVG
    )
    assert segs[0].svg_element_ids == ["a"]


async def test_repair_extracts_svg_and_segments():
    resp = _Resp(parsed=RepairResponse(
        svg=SVG, segments=[SegmentDraft(text="fixed", svg_element_ids=["a"])]
    ))
    client, _ = _client(resp)
    svg, segs = await GeminiLLMProvider(Settings(), client=client).repair(
        "<bad>", [SegmentDraft(text="x", svg_element_ids=["ghost"])], ["bad id"]
    )
    assert svg.startswith("<svg") and segs[0].svg_element_ids == ["a"]


async def test_transport_error_maps_to_provider_unavailable():
    client, _ = _client(error=RuntimeError("network down"))
    with pytest.raises(ProviderUnavailableError):
        await GeminiLLMProvider(Settings(), client=client).plan_concepts("Topic")


async def test_tts_wraps_pcm_to_wav_with_exact_duration():
    pcm = b"\x00\x00" * 24000  # 1.0 s at 24 kHz, 16-bit mono
    client, models = _client(_Resp(audio=pcm))
    clip = await GeminiTTSProvider(Settings(), client=client).synthesize("hi", voice="Puck")
    assert clip.content_type == "audio/wav"
    assert clip.data[:4] == b"RIFF"
    assert clip.duration_ms == 1000
    cfg = models.calls[0]["config"]
    assert cfg.speech_config.voice_config.prebuilt_voice_config.voice_name == "Puck"


async def test_tts_unknown_voice_falls_back_to_default():
    client, models = _client(_Resp(audio=b"\x00\x00" * 100))
    await GeminiTTSProvider(Settings(), client=client).synthesize("hi", voice="en-US-neutral")
    cfg = models.calls[0]["config"]
    assert cfg.speech_config.voice_config.prebuilt_voice_config.voice_name == "Kore"


async def test_tts_missing_audio_fails():
    client, _ = _client(_Resp(text="no audio here"))
    with pytest.raises(GenerationFailedError):
        await GeminiTTSProvider(Settings(), client=client).synthesize("hi")


class _FlakyModels:
    """Raises a transient error `fail_times` times, then returns `resp`."""

    def __init__(self, fail_times: int, resp):
        self.fail_times = fail_times
        self.resp = resp
        self.calls = 0

    async def generate_content(self, **kwargs):
        self.calls += 1
        if self.calls <= self.fail_times:
            raise RuntimeError("503 UNAVAILABLE: model experiencing high demand")
        return self.resp


async def test_retries_transient_error_then_succeeds():
    models = _FlakyModels(2, _Resp(parsed=ConceptPlan(title="T", concepts=["x"])))
    client = SimpleNamespace(aio=SimpleNamespace(models=models))
    settings = Settings(gemini_max_retries=3, gemini_retry_base_delay=0.0)
    plan = await GeminiLLMProvider(settings, client=client).plan_concepts("Topic")
    assert plan.title == "T"
    assert models.calls == 3  # 2 transient failures + 1 success


async def test_gives_up_after_max_retries_on_persistent_transient():
    models = _FlakyModels(99, _Resp(parsed=ConceptPlan(title="T", concepts=["x"])))
    client = SimpleNamespace(aio=SimpleNamespace(models=models))
    settings = Settings(gemini_max_retries=3, gemini_retry_base_delay=0.0)
    with pytest.raises(ProviderUnavailableError):
        await GeminiLLMProvider(settings, client=client).plan_concepts("Topic")
    assert models.calls == 3  # exhausted all attempts
