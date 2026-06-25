"""Typed domain errors.

Each maps to an HTTP status and the shared error envelope (see app/api/errors.py
and docs/06-api-contract.md). Raising these anywhere in the app yields a consistent
client-facing error.
"""


class DomainError(Exception):
    code: str = "DOMAIN_ERROR"
    http_status: int = 500
    retryable: bool = False

    def __init__(self, message: str | None = None):
        self.message = message or self.code
        super().__init__(self.message)


class InvalidTopicError(DomainError):
    code = "INVALID_TOPIC"
    http_status = 400


class LessonNotFoundError(DomainError):
    code = "LESSON_NOT_FOUND"
    http_status = 404


class JobNotFoundError(DomainError):
    code = "JOB_NOT_FOUND"
    http_status = 404


class AssetNotFoundError(DomainError):
    code = "ASSET_NOT_FOUND"
    http_status = 404


class GenerationFailedError(DomainError):
    code = "GENERATION_FAILED"
    http_status = 500
    retryable = True


class ProviderUnavailableError(DomainError):
    code = "PROVIDER_UNAVAILABLE"
    http_status = 503
    retryable = True
