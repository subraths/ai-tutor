# 03 — Domain Model

## 3.1 Core entities (backend)

```mermaid
classDiagram
    class Lesson {
      +str id
      +str topic
      +str topic_key
      +str title
      +str language
      +str voice
      +str svg
      +List~Segment~ segments
      +int total_duration_ms
      +datetime created_at
    }
    class Segment {
      +int index
      +str text
      +List~str~ svg_element_ids
      +str audio_asset_id
      +int duration_ms
    }
    class AssetRef {
      +str id
      +str kind
      +str content_type
      +str url
    }
    Lesson "1" *-- "1..*" Segment
    Segment "1" --> "1" AssetRef : audio
```

**Invariants**

- `Lesson.svg` is well-formed XML with a `viewBox`.
- Every `Segment.svg_element_ids` value exists as an `id` in `Lesson.svg`.
- `total_duration_ms == sum(segment.duration_ms)`.
- Segments are contiguous and ordered by `index` starting at 0.

## 3.2 Backend service & provider classes

```mermaid
classDiagram
    class LessonGenerationFacade {
      +generate(topic, opts) Lesson
      +get_or_generate(topic, opts) Lesson
    }
    class GenerationGraph {
      +invoke(state) GenerationState
    }
    class LLMProvider {
      <<interface>>
      +generate_text(prompt) str
      +generate_structured(prompt, schema) dict
    }
    class TTSProvider {
      <<interface>>
      +synthesize(text, voice) AudioClip
    }
    class GeminiLLMProvider
    class GeminiTTSProvider
    class ProviderFactory {
      +create_llm() LLMProvider
      +create_tts() TTSProvider
    }
    class LessonRepository {
      <<interface>>
      +get_by_topic_key(key) Lesson
      +save(lesson) void
    }
    class AssetRepository {
      <<interface>>
      +put(bytes, content_type) AssetRef
      +url_for(id) str
    }
    class LessonBuilder {
      +with_svg(svg) LessonBuilder
      +add_segment(seg) LessonBuilder
      +build() Lesson
    }

    LLMProvider <|.. GeminiLLMProvider
    TTSProvider <|.. GeminiTTSProvider
    ProviderFactory --> LLMProvider
    ProviderFactory --> TTSProvider
    LessonGenerationFacade --> GenerationGraph
    LessonGenerationFacade --> LessonRepository
    GenerationGraph --> LLMProvider
    GenerationGraph --> TTSProvider
    GenerationGraph --> AssetRepository
    GenerationGraph --> LessonBuilder
```

See [04 — AI Agent Design](04-ai-agent-design.md) for which design pattern each class implements.

## 3.3 Backend persistence (ER)

```mermaid
erDiagram
    CACHED_LESSON {
      string topic_key PK
      string lesson_id
      json   manifest
      int    hits
      datetime created_at
    }
    GENERATION_JOB {
      string id PK
      string topic_key
      string status
      int    progress
      string lesson_id
      string error
      datetime created_at
    }
    ASSET {
      string id PK
      string lesson_id
      string kind
      string content_type
      string storage_path
    }
    CACHED_LESSON ||--o{ ASSET : "owns"
    GENERATION_JOB ||--o| CACHED_LESSON : "produces"
```

- `topic_key` = normalized topic (lowercased, trimmed, collapsed whitespace,
  optionally language-suffixed) — the cache + idempotency key.
- `GENERATION_JOB.status` ∈ `{queued, running, succeeded, failed}`.
- `ASSET.kind` ∈ `{svg, audio}`.

## 3.4 On-device persistence (Room ER)

```mermaid
erDiagram
    LESSON_ENTITY {
      string id PK
      string topic
      string title
      int    total_duration_ms
      string svg_path
      string thumbnail_path
      long   created_at
    }
    SEGMENT_ENTITY {
      string id PK
      string lesson_id FK
      int    idx
      string text
      string svg_ids_json
      string audio_path
      int    duration_ms
    }
    LESSON_ENTITY ||--o{ SEGMENT_ENTITY : contains
```

- `svg_path`, `audio_path` point at files in app-private storage.
- `svg_ids_json` is the JSON-encoded `List<String>` of element IDs for the segment.

## 3.5 Lesson manifest (the wire/storage contract)

The manifest is what the API returns and what the device persists. Asset URLs are
resolvable for download; after download the device rewrites them to local paths.

```json
{
  "id": "les_8f3a...",
  "topic": "Photosynthesis",
  "title": "How Photosynthesis Works",
  "language": "en",
  "voice": "en-US-neutral",
  "total_duration_ms": 41200,
  "created_at": "2026-06-25T10:00:00Z",
  "svg": {
    "asset_id": "ast_svg_01",
    "url": "https://.../assets/ast_svg_01"
  },
  "segments": [
    {
      "index": 0,
      "text": "Sunlight strikes the leaf and is captured by the plant.",
      "svg_element_ids": ["sun", "leaf"],
      "audio": { "asset_id": "ast_aud_00", "url": "https://.../assets/ast_aud_00" },
      "duration_ms": 4200
    },
    {
      "index": 1,
      "text": "Chlorophyll inside the chloroplasts absorbs that light energy.",
      "svg_element_ids": ["chloroplast", "chlorophyll"],
      "audio": { "asset_id": "ast_aud_01", "url": "https://.../assets/ast_aud_01" },
      "duration_ms": 3100
    }
  ]
}
```

> The SVG may be inlined as a string instead of an asset URL for small diagrams; the
> manifest supports either. Audio is always a downloadable asset.
