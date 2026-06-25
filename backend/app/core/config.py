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
    generation_engine: str = "mock"   # mock | langgraph (langgraph lands in Phase 3)

    # --- Gemini config (unused while providers are "mock") ---
    # NOTE: the model id is CONFIG, not code. Verify the current Flash-class model
    # against the live API in Phase 4 and pin it here.
    gemini_model: str = "gemini-flash-latest"
    gemini_api_key: str | None = None

    # --- Generation tuning ---
    max_repair_retries: int = 2
    tts_ms_per_word: int = 400  # mock TTS duration heuristic (ms per word)

    # Base path used to build asset URLs in manifests.
    asset_base_url: str = "/api/v1/assets"


@lru_cache
def get_settings() -> Settings:
    return Settings()
