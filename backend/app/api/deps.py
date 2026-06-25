"""Dependency-injection accessors.

The object graph is wired once in the composition root (app/main.py) and stored on
app.state; these read it back for route handlers via FastAPI's Depends.
"""
from fastapi import Request

from app.core.config import Settings
from app.repositories.base import AssetRepository, LessonRepository
from app.services.generation import LessonGenerationFacade
from app.services.jobs import JobManager


def get_settings(request: Request) -> Settings:
    return request.app.state.settings


def get_lesson_repo(request: Request) -> LessonRepository:
    return request.app.state.lesson_repo


def get_asset_repo(request: Request) -> AssetRepository:
    return request.app.state.asset_repo


def get_facade(request: Request) -> LessonGenerationFacade:
    return request.app.state.facade


def get_job_manager(request: Request) -> JobManager:
    return request.app.state.job_manager
