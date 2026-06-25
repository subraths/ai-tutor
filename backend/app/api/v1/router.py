"""Aggregates the v1 routers."""
from fastapi import APIRouter

from app.api.v1 import assets, lessons

api_router = APIRouter()
api_router.include_router(lessons.router)
api_router.include_router(assets.router)
