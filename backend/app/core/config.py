"""Typed application settings (12-factor: all config via environment).

Everything that varies between environments — provider choice, model id, tuning —
lives here and is read from ``TUTOR_*`` environment variables (or a local .env).
"""
from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="TUTOR_",
        env_file=".env",
        extra="ignore",
    )

    app_name: str = "TutorAI Backend"
    api_v1_prefix: str = "/api/v1"
    environment: str = "dev"  # dev | prod | test

    # --- Provider / engine selection (Strategy + Abstract Factory) ---
    llm_provider: str = "mock"        # mock | gemini (gemini lands in Phase 4)
    tts_provider: str = "mock"        # mock | gemini
    generation_engine: str = "langgraph"  # langgraph | mock (mock = lightweight fallback)

    # --- Gemini config (used when *_provider == "gemini") ---
    # NOTE: model ids are CONFIG, not code. Confirm against the live API with
    # `python -m app.tools.list_models` and pin here. gemini-2.5-flash is the
    # safe GA default; gemini-3.1-flash is the newer option.
    gemini_api_key: str | None = None
    gemini_model: str = "gemini-2.5-flash"                    # text + structured output
    gemini_tts_model: str = "gemini-3.1-flash-tts-preview"    # text-to-speech
    gemini_voice: str = "Kore"                         # prebuilt voice name
    gemini_tts_sample_rate: int = 24000                # PCM sample rate from TTS
    gemini_max_retries: int = 3                        # retries on transient 429/503
    gemini_retry_base_delay: float = 1.0               # seconds, exponential backoff

    # --- Generation tuning ---
    max_repair_retries: int = 2
    tts_ms_per_word: int = 400  # mock TTS duration heuristic (ms per word)

    # --- Persistence ---
    storage_backend: str = "memory"  # memory | filesystem
    data_dir: str = "./data"          # used when storage_backend == "filesystem"

    # Base path used to build asset URLs in manifests.
    asset_base_url: str = "/api/v1/assets"


@lru_cache
def get_settings() -> Settings:
    return Settings()
