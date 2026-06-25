import pytest

from app.core.config import Settings
from app.core.errors import ProviderUnavailableError
from app.providers.factory import ProviderFactory
from app.providers.mock import MockLLMProvider, MockTTSProvider


def test_factory_builds_mock_providers():
    f = ProviderFactory(Settings())
    assert isinstance(f.create_llm(), MockLLMProvider)
    assert isinstance(f.create_tts(), MockTTSProvider)


def test_factory_gemini_requires_api_key():
    f = ProviderFactory(Settings(llm_provider="gemini", tts_provider="gemini"))
    with pytest.raises(ProviderUnavailableError):
        f.create_llm()
    with pytest.raises(ProviderUnavailableError):
        f.create_tts()


def test_factory_unknown_provider():
    with pytest.raises(ValueError):
        ProviderFactory(Settings(llm_provider="bogus")).create_llm()
