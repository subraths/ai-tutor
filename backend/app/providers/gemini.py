"""Gemini adapters implementing the LLM/TTS ports (Phase 4).

Uses the google-genai SDK (async client). The SDK client can be injected for
testing, so the suite exercises our request/response handling without a key or
network. Real verification still needs a key + a live smoke test (and possibly a
model-id tweak — confirm with `python -m app.tools.list_models`).
"""
import asyncio
import base64
import json
import logging
import re

from google import genai
from google.genai import types
from pydantic import BaseModel

from app.core.audio import pcm_duration_ms, pcm_to_wav
from app.core.config import Settings
from app.core.errors import GenerationFailedError, ProviderUnavailableError
from app.domain.generation import ConceptPlan, SegmentDraft, SvgCritique
from app.providers import prompts
from app.providers.base import AudioClip, LLMProvider, TTSProvider

log = logging.getLogger("tutor.gemini")

PREBUILT_VOICES = {
    "Puck", "Charon", "Kore", "Fenrir", "Aoede", "Leda", "Orus", "Zephyr",
}

_SVG_RE = re.compile(r"<svg\b.*?</svg>", re.DOTALL | re.IGNORECASE)
_TRANSIENT_CODES = {429, 500, 502, 503, 504}
_TRANSIENT_MARKERS = ("UNAVAILABLE", "RESOURCE_EXHAUSTED", "INTERNAL", "DEADLINE")


def _is_transient(exc: Exception) -> bool:
    code = getattr(exc, "code", None) or getattr(exc, "status_code", None)
    if isinstance(code, int) and code in _TRANSIENT_CODES:
        return True
    text = str(exc).upper()
    return any(str(c) in text for c in _TRANSIENT_CODES) or any(
        m in text for m in _TRANSIENT_MARKERS
    )


async def _generate_with_retry(client, settings: Settings, **kwargs):
    """Call generate_content, retrying transient 429/503 with exponential backoff."""
    attempts = max(1, settings.gemini_max_retries)
    delay = settings.gemini_retry_base_delay
    for attempt in range(attempts):
        try:
            return await client.aio.models.generate_content(**kwargs)
        except Exception as exc:
            if attempt == attempts - 1 or not _is_transient(exc):
                raise ProviderUnavailableError(f"Gemini request failed: {exc}") from exc
            log.warning(
                "transient Gemini error (attempt %d/%d): %s", attempt + 1, attempts, exc
            )
            await asyncio.sleep(delay)
            delay *= 2


# --- structured-output response schemas ---
class NarrationResponse(BaseModel):
    segments: list[SegmentDraft]


class RepairResponse(BaseModel):
    svg: str
    segments: list[SegmentDraft]


def _extract_svg(text: str) -> str:
    if not text:
        raise GenerationFailedError("Model returned empty SVG output")
    match = _SVG_RE.search(text)
    if not match:
        raise GenerationFailedError("No <svg> element found in model output")
    return match.group(0).strip()


def _require_key(settings: Settings) -> None:
    if not settings.gemini_api_key:
        raise ProviderUnavailableError(
            "TUTOR_GEMINI_API_KEY is required for the gemini provider"
        )


class GeminiLLMProvider(LLMProvider):
    def __init__(self, settings: Settings, client: genai.Client | None = None) -> None:
        self._settings = settings
        self._model = settings.gemini_model
        # A (optionally higher-quality) model dedicated to the SVG work.
        self._svg_model = settings.gemini_svg_model or settings.gemini_model
        if client is None:
            _require_key(settings)
            client = genai.Client(api_key=settings.gemini_api_key)
        self._client = client

    async def _generate_json(
        self, prompt: str, schema: type[BaseModel], model: str | None = None
    ) -> BaseModel:
        resp = await _generate_with_retry(
            self._client,
            self._settings,
            model=model or self._model,
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                response_schema=schema,
            ),
        )

        parsed = getattr(resp, "parsed", None)
        if isinstance(parsed, schema):
            return parsed

        text = getattr(resp, "text", None)
        if not text:
            raise GenerationFailedError("Gemini returned no parseable content")
        try:
            return schema.model_validate(json.loads(text))
        except (json.JSONDecodeError, ValueError) as exc:
            raise GenerationFailedError(f"Gemini returned invalid JSON: {exc}") from exc

    async def _generate_text(self, prompt: str, model: str | None = None) -> str:
        resp = await _generate_with_retry(
            self._client, self._settings, model=model or self._model, contents=prompt
        )
        return getattr(resp, "text", "") or ""

    async def plan_concepts(self, topic: str, language: str = "en") -> ConceptPlan:
        result = await self._generate_json(
            prompts.plan_prompt(topic, language), ConceptPlan
        )
        return result  # type: ignore[return-value]

    async def generate_svg(self, topic: str, plan: ConceptPlan) -> str:
        text = await self._generate_text(prompts.svg_prompt(topic, plan), model=self._svg_model)
        return _extract_svg(text)

    async def critique_svg(
        self, topic: str, plan: ConceptPlan, svg: str
    ) -> SvgCritique:
        result = await self._generate_json(
            prompts.critique_prompt(topic, plan, svg), SvgCritique, model=self._svg_model
        )
        return result  # type: ignore[return-value]

    async def refine_svg(
        self, topic: str, plan: ConceptPlan, svg: str, critique: SvgCritique
    ) -> str:
        text = await self._generate_text(
            prompts.refine_prompt(topic, plan, svg, critique), model=self._svg_model
        )
        return _extract_svg(text)

    async def generate_narration(
        self, topic: str, plan: ConceptPlan, svg: str
    ) -> list[SegmentDraft]:
        result = await self._generate_json(
            prompts.narration_prompt(topic, plan, svg), NarrationResponse
        )
        return result.segments  # type: ignore[attr-defined]

    async def repair(
        self, svg: str, segments: list[SegmentDraft], errors: list[str]
    ) -> tuple[str, list[SegmentDraft]]:
        result = await self._generate_json(
            prompts.repair_prompt(svg, segments, errors), RepairResponse
        )
        return _extract_svg(result.svg), result.segments  # type: ignore[attr-defined]


class GeminiTTSProvider(TTSProvider):
    def __init__(self, settings: Settings, client: genai.Client | None = None) -> None:
        self._settings = settings
        if client is None:
            _require_key(settings)
            client = genai.Client(api_key=settings.gemini_api_key)
        self._client = client

    async def synthesize(self, text: str, voice: str = "Kore") -> AudioClip:
        voice_name = voice if voice in PREBUILT_VOICES else self._settings.gemini_voice
        resp = await _generate_with_retry(
            self._client,
            self._settings,
            model=self._settings.gemini_tts_model,
            contents=text,
            config=types.GenerateContentConfig(
                response_modalities=["AUDIO"],
                speech_config=types.SpeechConfig(
                    voice_config=types.VoiceConfig(
                        prebuilt_voice_config=types.PrebuiltVoiceConfig(
                            voice_name=voice_name
                        )
                    )
                ),
            ),
        )

        try:
            pcm = resp.candidates[0].content.parts[0].inline_data.data
        except (AttributeError, IndexError, TypeError) as exc:
            raise GenerationFailedError("Gemini TTS returned no audio data") from exc

        if isinstance(pcm, str):  # some transports base64-encode inline data
            pcm = base64.b64decode(pcm)

        sr = self._settings.gemini_tts_sample_rate
        return AudioClip(
            data=pcm_to_wav(pcm, sr),
            content_type="audio/wav",
            duration_ms=pcm_duration_ms(pcm, sr),
        )
