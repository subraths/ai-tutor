"""Abstract Factory — build the configured provider set from Settings."""
from app.core.config import Settings
from app.providers.base import LLMProvider, TTSProvider
from app.providers.mock import MockLLMProvider, MockTTSProvider


class ProviderFactory:
    def __init__(self, settings: Settings):
        self._settings = settings

    def create_llm(self) -> LLMProvider:
        provider = self._settings.llm_provider
        if provider == "mock":
            return MockLLMProvider(self._settings)
        if provider == "gemini":
            # Local import so mock-only deployments don't require google-genai.
            from app.providers.gemini import GeminiLLMProvider

            return GeminiLLMProvider(self._settings)
        raise ValueError(f"Unknown llm_provider: {provider!r}")

    def create_tts(self) -> TTSProvider:
        provider = self._settings.tts_provider
        if provider == "mock":
            return MockTTSProvider(self._settings)
        if provider == "gemini":
            from app.providers.gemini import GeminiTTSProvider

            return GeminiTTSProvider(self._settings)
        raise ValueError(f"Unknown tts_provider: {provider!r}")
