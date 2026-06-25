"""List the Gemini models available to your API key.

Use this to CONFIRM the exact model ids before pinning them in settings
(TUTOR_GEMINI_MODEL / TUTOR_GEMINI_TTS_MODEL) — model names change over time.

Run:
    TUTOR_GEMINI_API_KEY=... python -m app.tools.list_models
"""
import sys

from app.core.config import get_settings


def main() -> int:
    settings = get_settings()
    if not settings.gemini_api_key:
        print("Set TUTOR_GEMINI_API_KEY to list models.", file=sys.stderr)
        return 1

    from google import genai

    client = genai.Client(api_key=settings.gemini_api_key)
    print(f"Configured text model : {settings.gemini_model}")
    print(f"Configured TTS model  : {settings.gemini_tts_model}\n")
    print("Available models:")
    for model in client.models.list():
        actions = ", ".join(getattr(model, "supported_actions", []) or [])
        print(f"  {model.name:45s} {actions}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
