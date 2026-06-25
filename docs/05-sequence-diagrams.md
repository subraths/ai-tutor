# 05 — Sequence Diagrams

## 5.1 Lesson generation (cache miss)

```mermaid
sequenceDiagram
    actor U as User
    participant A as Android App
    participant API as FastAPI
    participant C as Cache Repo
    participant G as LangGraph Agent
    participant L as Gemini LLM
    participant T as TTS
    participant S as Asset Store

    U->>A: Enter topic "Photosynthesis"
    A->>API: POST /lessons {topic}
    API->>C: get_by_topic_key(key)
    C-->>API: miss
    API-->>A: 202 {job_id}
    Note over API,G: generation runs async (background/worker)
    API->>G: invoke(topic, options)
    G->>L: plan concepts
    L-->>G: concept plan
    G->>L: generate SVG (stable ids)
    L-->>G: svg
    G->>L: generate narration segments -> svg ids
    L-->>G: segments
    G->>G: validate (well-formed + ids exist)
    loop until valid or max retries
        G->>L: repair(offending ids / errors)
        L-->>G: fixed svg / segments
        G->>G: re-validate
    end
    loop each segment
        G->>T: synthesize(text, voice)
        T-->>G: audio bytes + duration
        G->>S: store(audio)
        S-->>G: asset ref
    end
    G->>S: store(svg)
    G->>C: save(manifest)
    A->>API: GET /lessons/jobs/{job_id}  (poll)
    API-->>A: {status: succeeded, lesson_id}
    A->>API: GET /lessons/{lesson_id}
    API-->>A: manifest + asset urls
    A->>S: download svg + audio assets
    A->>A: persist locally (Room + files), rewrite urls -> paths
    A-->>U: Lesson ready to play
```

## 5.2 Lesson generation (cache hit)

```mermaid
sequenceDiagram
    actor U as User
    participant A as Android App
    participant API as FastAPI
    participant C as Cache Repo
    participant S as Asset Store

    U->>A: Enter topic "Photosynthesis"
    A->>API: POST /lessons {topic}
    API->>C: get_by_topic_key(key)
    C-->>API: hit (manifest)
    API->>C: increment hits
    API-->>A: 200 manifest + asset urls
    A->>S: download assets
    A->>A: persist locally
    A-->>U: Lesson ready to play
```

## 5.3 Offline playback with highlight sync

```mermaid
sequenceDiagram
    actor U as User
    participant H as History Screen
    participant P as Player ViewModel
    participant R as Local Repo (Room + Files)
    participant SVG as SVG Renderer + Highlight Engine
    participant AU as Audio Player (ExoPlayer)

    U->>H: Tap a saved lesson
    H->>R: load(lesson_id)
    R-->>H: lesson + segments (local paths)
    H->>P: start(lesson)
    P->>SVG: render(svg)
    loop each segment (index order)
        P->>SVG: highlight(segment.svg_element_ids)
        P->>AU: play(segment.audio_path)
        AU-->>P: onComplete
        P->>SVG: clear highlight
    end
    P-->>U: Lesson finished (offer replay)
```

## 5.4 Pause / resume / seek

```mermaid
sequenceDiagram
    actor U as User
    participant P as Player ViewModel
    participant AU as Audio Player
    participant SVG as Highlight Engine

    U->>P: pause()
    P->>AU: pause()
    Note over P: state = Paused (highlight retained)
    U->>P: resume()
    P->>AU: resume()
    U->>P: seek(index = k)
    P->>AU: stop()
    P->>SVG: clear + highlight(segment[k].svg_element_ids)
    P->>AU: play(segment[k].audio_path)
```

## 5.5 Coalesced concurrent requests (same topic)

```mermaid
sequenceDiagram
    participant A1 as Device A
    participant A2 as Device B
    participant API as FastAPI
    participant J as Job Registry

    A1->>API: POST /lessons {topic=X}
    API->>J: get_or_create_job(key=X)
    J-->>API: job_id=J1 (new, running)
    API-->>A1: 202 {job_id: J1}
    A2->>API: POST /lessons {topic=X}
    API->>J: get_or_create_job(key=X)
    J-->>API: job_id=J1 (existing)
    API-->>A2: 202 {job_id: J1}
    Note over API,J: one generation serves both devices
```
