# TutorAI Backend

FastAPI backend for TutorAI. **Phase 2 skeleton:** runs end-to-end with **mock**
providers — no AI keys required. The real LangGraph agent (Phase 3) and Gemini/TTS
adapters (Phase 4) drop in behind the existing interfaces without touching call sites.

## Layout

```
app/
├── main.py            # composition root + app factory
├── core/              # config, logging, typed errors, id helpers
├── domain/            # entities, DTO schemas, generation value objects, mappers
├── providers/         # LLM + TTS ports (Strategy) + mock adapters + factory
├── repositories/      # lesson/asset/job ports + in-memory implementations
├── agents/            # generation graph port, deterministic validator, mock graph
├── services/          # LessonBuilder, generation facade, async job manager
└── api/               # routers, DI deps, error envelope
tests/                 # health, validator, generation, lessons API
```

Design patterns: Strategy, Abstract Factory, Adapter, Builder, Repository, Facade,
DTO, Dependency Injection — see [../docs/04-ai-agent-design.md](../docs/04-ai-agent-design.md).

## Setup & run

```bash
# from the backend/ directory
uv venv
uv pip install -r requirements-dev.txt
PYTHONPATH=. uv run uvicorn app.main:app --reload
```

## Test

```bash
PYTHONPATH=. uv run pytest
```

## Try it

```bash
# start generation (cache miss → 202 + job_id)
curl -s -X POST localhost:8000/api/v1/lessons \
  -H 'content-type: application/json' -d '{"topic":"Photosynthesis"}'

# poll the job
curl -s localhost:8000/api/v1/lessons/jobs/<job_id>

# fetch the manifest, then download assets
curl -s localhost:8000/api/v1/lessons/<lesson_id>
```

Interactive docs at `http://localhost:8000/docs`.
