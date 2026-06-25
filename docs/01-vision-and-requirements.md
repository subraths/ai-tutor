# 01 — Vision & Requirements

## 1.1 Problem statement

Learners who want a quick, visual explanation of a concept have to stitch together
text, static diagrams, and separate videos. There is no lightweight tool where you
type a topic and immediately get a **purpose-built diagram that explains itself** —
narrated, with the relevant parts lighting up as they are discussed — and that you
can revisit later without a network connection.

## 1.2 Target users

- **Students** (school / university) revising a concept.
- **Self-learners** exploring an unfamiliar topic.
- **Educators** generating quick visual aids.

## 1.3 Core user stories

| # | As a… | I want to… | So that… |
|---|---|---|---|
| US-1 | learner | type a topic and get a narrated diagram | I understand it visually and aurally |
| US-2 | learner | see diagram parts highlight as they're explained | I know exactly what is being described |
| US-3 | learner | pause, resume, and replay any segment | I can learn at my own pace |
| US-4 | learner | find all my past topics in a history list | I can revisit lessons |
| US-5 | learner | replay a saved lesson with no internet | I can study offline |
| US-6 | learner | retry generation if a result is poor | I get a usable lesson |

## 1.4 Functional requirements

- **FR-1** Accept a free-text topic from the user.
- **FR-2** Generate an SVG diagram with stable, semantic element IDs.
- **FR-3** Generate an ordered narration script split into **segments**, each referencing the SVG element IDs it describes.
- **FR-4** Produce one TTS audio clip per segment with a measured duration.
- **FR-5** Assemble a self-contained **lesson manifest** (SVG + segments + audio refs + durations).
- **FR-6** Serve cached lessons for previously requested topics.
- **FR-7** On device: download and persist the full lesson (manifest + assets) locally.
- **FR-8** On device: play the lesson, highlighting each segment's SVG element IDs while its audio plays.
- **FR-9** On device: provide play / pause / resume / seek-by-segment / replay controls.
- **FR-10** On device: maintain a searchable history of saved lessons.
- **FR-11** On device: replay any saved lesson fully offline.
- **FR-12** Surface clear errors and allow regeneration on failure.

## 1.5 Non-functional requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-1 | Performance | Cache hit returns a manifest in < 500 ms; cold generation target < 30 s |
| NFR-2 | Offline | Saved lessons play with zero network calls |
| NFR-3 | Reliability | Generation validates SVG well-formedness and ID consistency before caching |
| NFR-4 | Cost | Identical topics are generated once and shared via cache |
| NFR-5 | Portability | LLM and TTS providers are swappable behind adapters |
| NFR-6 | Maintainability | Clear layering + documented design patterns; unit-testable core |
| NFR-7 | Privacy | No accounts; personal history never leaves the device |
| NFR-8 | Accessibility | Captions/transcript shown; adjustable playback; readable contrast |

## 1.6 Out of scope (v1)

- User accounts and cross-device cloud sync.
- Editing/annotating generated diagrams.
- Multi-language UI (lesson language is a parameter, but UI is single-language v1).
- Social / sharing features.
- Real-time word-level highlighting (we use **segment-level** sync in v1).

## 1.7 Glossary

| Term | Meaning |
|---|---|
| **Lesson** | Complete generated package for a topic: SVG + segments + audio + metadata |
| **Segment** | One narration unit: text + the SVG element IDs it highlights + its audio clip + duration |
| **Manifest** | JSON describing a lesson and where its assets live |
| **Highlight engine** | Android component that applies/clears visual emphasis on SVG element IDs |
| **Agent / Graph** | The LangGraph pipeline that generates and validates a lesson |
| **Provider** | A swappable adapter around an external AI service (LLM or TTS) |

## 1.8 Key decisions (resolved)

1. **Sync = pre-rendered timed segments.** Backend renders each segment's audio and measures duration; the device plays clips in order and highlights mapped IDs. Deterministic and offline-capable.
2. **Backend = stateless + shared cache.** No accounts; lessons cached by normalized topic key.
3. **Diagrams = Mermaid in Markdown.**
