import os

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient

from app.core import config
from app.main import create_app


@pytest.fixture(autouse=True, scope="session")
def _hermetic_settings():
    """Keep the suite key-free and deterministic.

    A developer's local ``backend/.env`` (real Gemini key, ``provider=gemini``)
    must never leak into tests, or the mock-based suite would hit the live API —
    making it slow, flaky, and dependent on a key. Disable ``.env`` loading and
    strip any ``TUTOR_*`` vars for the session so ``Settings()`` yields the code
    defaults (mock providers, in-memory storage).
    """
    saved = {k: os.environ.pop(k) for k in list(os.environ) if k.startswith("TUTOR_")}
    original_env_file = config.Settings.model_config.get("env_file")
    config.Settings.model_config["env_file"] = None
    config.get_settings.cache_clear()
    try:
        yield
    finally:
        config.Settings.model_config["env_file"] = original_env_file
        os.environ.update(saved)
        config.get_settings.cache_clear()


@pytest.fixture
def app():
    # Fresh app (and fresh in-memory repos) per test for isolation.
    return create_app()


@pytest_asyncio.fixture
async def client(app):
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as c:
        yield c
