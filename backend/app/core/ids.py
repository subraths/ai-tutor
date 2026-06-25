"""Prefixed id helpers (les_, job_, ast_)."""
import uuid


def _new(prefix: str) -> str:
    return f"{prefix}_{uuid.uuid4().hex[:12]}"


def new_lesson_id() -> str:
    return _new("les")


def new_job_id() -> str:
    return _new("job")


def new_asset_id() -> str:
    return _new("ast")
