"""Lesson + job endpoints."""
from fastapi import APIRouter, Depends, Request
from fastapi.responses import JSONResponse

from app.api.deps import get_asset_repo, get_facade, get_job_manager, get_lesson_repo
from app.core.errors import LessonNotFoundError
from app.domain.generation import DEFAULT_VOICE, GenerationOptions, normalize_topic
from app.domain.mappers import to_manifest
from app.domain.schemas import (
    GenerateLessonRequest,
    JobAccepted,
    JobStatusOut,
    LessonManifest,
)

router = APIRouter(prefix="/lessons", tags=["lessons"])


# Declared before "/{lesson_id}" so "jobs" is never parsed as a lesson id.
@router.get("/jobs/{job_id}", response_model=JobStatusOut)
async def get_job(job_id: str, jobs=Depends(get_job_manager)) -> JobStatusOut:
    job = await jobs.get(job_id)
    return JobStatusOut(
        job_id=job.id,
        status=job.status,
        progress=job.progress,
        stage=job.stage,
        lesson_id=job.lesson_id,
        error=job.error,
    )


@router.post("")
async def create_lesson(
    req: GenerateLessonRequest,
    request: Request,
    facade=Depends(get_facade),
    lessons=Depends(get_lesson_repo),
    assets=Depends(get_asset_repo),
    jobs=Depends(get_job_manager),
) -> JSONResponse:
    topic_key = normalize_topic(req.topic, req.language)  # raises InvalidTopicError

    cached = await facade.get_cached(topic_key)
    if cached is not None:
        await lessons.increment_hits(topic_key)
        manifest = to_manifest(cached, assets)
        return JSONResponse(status_code=200, content=manifest.model_dump(mode="json"))

    options = GenerationOptions(language=req.language, voice=req.voice or DEFAULT_VOICE)
    job, _created = await jobs.submit(req.topic.strip(), topic_key, options)
    accepted = JobAccepted(job_id=job.id, status=job.status, topic_key=topic_key)
    return JSONResponse(status_code=202, content=accepted.model_dump(mode="json"))


@router.get("/{lesson_id}", response_model=LessonManifest)
async def get_lesson(
    lesson_id: str,
    lessons=Depends(get_lesson_repo),
    assets=Depends(get_asset_repo),
) -> LessonManifest:
    lesson = await lessons.get(lesson_id)
    if lesson is None:
        raise LessonNotFoundError(f"Lesson {lesson_id} not found")
    return to_manifest(lesson, assets)
