"""Request-id middleware + exception handlers producing the shared error envelope."""
import uuid

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.core.errors import DomainError
from app.domain.schemas import ErrorBody, ErrorResponse


def _envelope(request: Request, status: int, code: str, message: str, retryable: bool) -> JSONResponse:
    body = ErrorResponse(
        error=ErrorBody(
            code=code,
            message=message,
            retryable=retryable,
            request_id=getattr(request.state, "request_id", None),
        )
    )
    return JSONResponse(status_code=status, content=body.model_dump())


def install_request_id_middleware(app: FastAPI) -> None:
    @app.middleware("http")
    async def _add_request_id(request: Request, call_next):
        request.state.request_id = "req_" + uuid.uuid4().hex[:12]
        response = await call_next(request)
        response.headers["X-Request-ID"] = request.state.request_id
        return response


def install_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(DomainError)
    async def _domain(request: Request, exc: DomainError):
        return _envelope(request, exc.http_status, exc.code, exc.message, exc.retryable)

    @app.exception_handler(RequestValidationError)
    async def _validation(request: Request, exc: RequestValidationError):
        return _envelope(
            request, 422, "VALIDATION_ERROR", "Request failed validation.", False
        )
