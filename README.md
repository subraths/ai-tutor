# TutorAI — Interactive Visual Tutor

> Type a topic. Get an AI-generated SVG diagram, a spoken explanation, and **synchronized highlighting** of the diagram as the narration talks through it — replayable offline from your device.

## What it does

1. User enters a topic on the Android app (e.g. *Photosynthesis*).
2. The backend's **agentic AI pipeline** (LangChain + LangGraph + Gemini) generates:
   - an **SVG** diagram whose parts have stable, semantic IDs,
   - a **segmented narration script**, where each segment names the SVG element IDs it talks about,
   - a **TTS audio clip per segment**, with measured duration.
3. The device downloads this **lesson package**, stores it locally, and plays it back: for each segment it highlights the mapped SVG parts while the matching audio plays.
4. Every lesson is saved in **on-device history** and replayable **fully offline**.

## Locked architecture decisions

| Decision | Choice | Rationale |
|---|---|---|
| Audio ↔ highlight sync | **Pre-rendered timed segments** | Deterministic sync; works offline after a single download |
| Backend persistence | **Stateless + shared lesson cache** | No accounts; popular topics served instantly & cheaply |
| Diagram format | **Mermaid in Markdown** | Renders inline in VS Code / GitHub, easy to version |

## Tech stack

- **Frontend:** Android, Kotlin, Jetpack Compose, MVVM + Clean Architecture, Room, ExoPlayer, SVG rendering.
- **Backend:** Python, FastAPI, Pydantic, LangChain, LangGraph.
- **AI:** Gemini (LLM) + a TTS service, both behind swappable provider adapters.
- **Storage:** PostgreSQL (lesson cache + jobs) + object storage (SVG/audio assets).

## Design docs (Phase 1)

| Doc | Contents |
|---|---|
| [01 — Vision & Requirements](docs/01-vision-and-requirements.md) | Problem, users, user stories, FR/NFR, scope, glossary |
| [02 — Architecture](docs/02-architecture.md) | Component + deployment diagrams, stack, cross-cutting concerns |
| [03 — Domain Model](docs/03-domain-model.md) | Class diagrams, ER diagrams, the lesson manifest schema |
| [04 — AI Agent Design](docs/04-ai-agent-design.md) | LangGraph graph, agent state, node specs, design patterns |
| [05 — Sequence Diagrams](docs/05-sequence-diagrams.md) | Generation, cache hit, download, offline playback |
| [06 — API Contract](docs/06-api-contract.md) | Endpoints, schemas, job model, errors, versioning |
| [07 — Android Architecture](docs/07-android-architecture.md) | Clean-arch layers, modules, player state machine |
| [08 — Roadmap & SDLC](docs/08-roadmap-and-sdlc.md) | Phased plan, exit criteria, testing, risks |

## Repository layout (planned)

```
tutor/
├── README.md
├── docs/                 # Phase 1 design (this set)
├── backend/              # FastAPI + LangGraph (Phase 2+)
│   ├── app/
│   │   ├── api/          # routers, dependencies
│   │   ├── core/         # config, logging, errors
│   │   ├── domain/       # entities, value objects, schemas
│   │   ├── agents/       # LangGraph nodes, graph, prompts
│   │   ├── providers/    # LLM + TTS adapters (Strategy/Factory)
│   │   ├── services/     # generation facade, assembler
│   │   └── repositories/ # cache + asset store
│   └── tests/
└── android/              # Kotlin app (Phase 5+)
    └── app/src/main/...
```

## Status

**Phase 1 — Architecture & Design (in progress).** No code yet; we design first.
See [08 — Roadmap & SDLC](docs/08-roadmap-and-sdlc.md) for the phase plan.
