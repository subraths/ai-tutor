"""Composition root + FastAPI app factory.

This is the one place objects are wired together (manual dependency injection).
Swapping providers/engine in Phase 3/4 is a change here + in Settings only.
"""
from fastapi import FastAPI

from app.agents.base import GenerationGraph
from app.agents.mock_graph import MockGenerationGraph
from app.api.errors import install_error_handlers, install_request_id_middleware
from app.api.v1.health import router as health_router
from app.api.v1.router import api_router
from app.core.config import Settings, get_settings
from app.core.logging import configure_logging
from app.providers.base import LLMProvider, TTSProvider
from app.providers.factory import ProviderFactory
from app.repositories.base import AssetRepository, LessonRepository
from app.repositories.memory import (
    InMemoryAssetRepository,
    InMemoryJobRepository,
    InMemoryLessonRepository,
)
from app.services.generation import LessonGenerationFacade
from app.services.jobs import JobManager


def _build_graph(
    settings: Settings,
    llm: LLMProvider,
    tts: TTSProvider,
    assets: AssetRepository,
) -> GenerationGraph:
    if settings.generation_engine == "mock":
        return MockGenerationGraph(llm, tts, assets, settings)
    if settings.generation_engine == "langgraph":
        # Local import so a mock-only deployment doesn't require langgraph.
        from app.agents.langgraph_graph import LangGraphGenerationGraph

        return LangGraphGenerationGraph(llm, tts, assets, settings)
    raise ValueError(f"Unknown generation_engine: {settings.generation_engine!r}")


def _build_storage(settings: Settings) -> tuple[LessonRepository, AssetRepository]:
    if settings.storage_backend == "memory":
        return (
            InMemoryLessonRepository(),
            InMemoryAssetRepository(settings.asset_base_url),
        )
    if settings.storage_backend == "filesystem":
        from app.repositories.filesystem import (
            FilesystemAssetRepository,
            FilesystemLessonRepository,
        )

        return (
            FilesystemLessonRepository(settings.data_dir),
            FilesystemAssetRepository(settings.asset_base_url, settings.data_dir),
        )
    raise ValueError(f"Unknown storage_backend: {settings.storage_backend!r}")


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or get_settings()
    configure_logging()

    app = FastAPI(title=settings.app_name, version="0.1.0")

    # --- wire the object graph (composition root) ---
    factory = ProviderFactory(settings)
    llm = factory.create_llm()
    tts = factory.create_tts()

    lesson_repo, asset_repo = _build_storage(settings)
    job_repo = InMemoryJobRepository()

    graph = _build_graph(settings, llm, tts, asset_repo)
    facade = LessonGenerationFacade(graph, lesson_repo)
    job_manager = JobManager(job_repo, facade)

    app.state.settings = settings
    app.state.lesson_repo = lesson_repo
    app.state.asset_repo = asset_repo
    app.state.job_repo = job_repo
    app.state.facade = facade
    app.state.job_manager = job_manager

    # --- middleware, error envelope, routes ---
    install_request_id_middleware(app)
    install_error_handlers(app)
    app.include_router(health_router)
    app.include_router(api_router, prefix=settings.api_v1_prefix)

    return app


app = create_app()
