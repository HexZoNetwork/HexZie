# HexZie — Voice-First On-Device AI Agent for Android

HexZie is a hands-free, voice-first, on-device-controlling AI agent for Android, featuring an OpenCode-style tool-calling loop, user-configurable AI gateways (OpenAI-compatible, Anthropic, Ollama, Gemini), fine-grained permission tiers, local Room persistence, and a responsive visual feedback orb.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were aligned during Phase 1 clarification:
> - **Platform Strategy**: Built as a native Android application (minSdk 24, targetSdk 36) with comprehensive desktop/Windows bridge documentation in `docs/DESKTOP_BRIDGE.md` and `docs/LIMITATIONS.md`.
> - **Primary AI Provider**: Custom Gateway first (OpenAI-compatible, Anthropic, Ollama, Local LM Studio) with user-provided endpoints and API keys, plus optional Gemini high-thinking engine.
> - **Voice & Activation Mode**: Default activation via interactive tap-to-talk animated orb with Android SpeechRecognizer / mic streaming, with pluggable support for Porcupine wake-word ("Hey Hex", "Hexzie") when user enters an access key.

---

## 1. Overview & Core Concept

### What It Does
HexZie acts as an on-device digital assistant and autonomous agent:
- Speaks and listens naturally via streaming Speech-to-Text and Android Text-to-Speech with barge-in support.
- Runs an OpenCode-style agentic tool-execution loop: evaluates the user's intent, generates structured tool calls, checks the In-App Permission Center, executes Android platform operations, and speaks the result.
- Provides real device capabilities: screen inspection, UI control gestures via AccessibilityService, app launching, notifications, volume/device settings, contacts, SMS, calendar, and camera capture.
- Enforces strict safety: HIGH-risk tools require explicit voice or on-screen confirmation; untrusted screen and notification text is isolated from system prompts; and a one-tap red kill-switch immediately cancels all tasks.

### Target Audience & Persona
Power users, developers, and hands-free smartphone users who desire an open, private, auditable, and model-agnostic assistant that runs on their own hardware and accounts.

### Key Value
Full control over device actions without lock-in to proprietary cloud assistant silos, with complete audit logs and granular permission controls.

---

## 2. User Experience & Visual Design

### Aesthetic Direction
- **Vibe**: Cybernetic, utilitarian, dark-first luxury (#0A0A0B base) with an electric blue accent (#3B82F6) and semantic state cues.
- **Visual Focus**: The central pulsating Orb is the interactive focal point:
  - *Idle*: Subtle breathing pulse (blue glow).
  - *Listening*: Rapid spring scale-up and audio waveform ring.
  - *Thinking*: Continuous rotating gradient sweep ring.
  - *Acting*: Orbiting progress ring showing active tool execution.
  - *Speaking*: Radiant green waveform synced with speech synthesis.
  - *Safety Kill-Switch / High-Risk Alert*: Immediate snap to red (#EF4444).

### Key Screens & Navigation
1. **Home / Live Screen**:
   - Central animated visual orb reacting to speech and agent states.
   - Live transcript fade-in stream below the orb.
   - Quick-action chips (e.g., "Read Screen", "Open YouTube", "Battery & Volume", "Check Notifications").
   - Status indicators for active LLM Gateway, STT engine, and TTS status.
2. **Chat & Agent Loop Screen**:
   - Conversational history with rich tool-call cards.
   - Tool cards show color-coded risk bars: Gray (Low), Blue (Medium), Red (High), expandable arguments, execution timing, and return payloads.
   - Streaming text tokens with a blinking blue block cursor.
3. **Models & Gateways Screen**:
   - Add/edit multiple gateways (OpenAI-compatible endpoints, Anthropic, Ollama, Gemini).
   - "Scan Models" action to query `/v1/models` and display context window & tool-calling capabilities.
   - Role assignments: Chat Model, Vision Model, Quick Wake Model.
   - Latency tester and connection diagnostics.
4. **Voice & Wake Word Screen**:
   - Wake phrase settings ("Hey Hex", "Hexzie", "Hey Hexzie").
   - Porcupine AccessKey input, sensitivity slider, and offline STT/TTS voice selection.
5. **Agent Permission Center**:
   - Segmented 3-state controls for each capability: `Off` (Gray) / `Ask Every Time` (Amber) / `Always Allow` (Green).
   - Master Safe Mode toggle & Red "Revoke All" emergency action.
   - Per-app allowlist/denylist (financial and password apps denylisted by default).
6. **Audit Log Screen**:
   - Chronological, exportable event ledger tracking every tool invocation, timestamp, parameters (redacted secrets), user authorization source, and output status.
7. **Onboarding & Permissions Setup**:
   - Step-by-step permission primer explaining Accessibility, Microphone, Notifications, and Overlay permissions with direct deep-links.

---

## 3. Key Product Decisions & Trade-Offs

| Decision | Chosen Approach | Rationale |
|---|---|---|
| **Architecture** | Single-Activity MVVM + Clean Architecture | Jetpack Compose recommended pattern; low overhead, robust testability, smooth animated transitions. |
| **Agent Execution Loop** | Coroutine-driven State Machine with sliding-window history | Enables instant cancellation via `Job.cancel()`, parallel tool dispatch where independent, and streaming updates. |
| **Persistence** | Room DB + Encrypted DataStore | Room handles multi-table relations (Sessions, Messages, AuditLog) with reactive `Flow`; DataStore stores settings with Keystore-backed API keys. |
| **Tool Execution** | Android AccessibilityService + System APIs | Provides legitimate non-root accessibility gestures (tap, scroll, back, home) and system queries while adhering to Android platform security. |
| **Kill Switch** | Hardware/Software broadcast interrupt | Volume chord, persistent notification action, and overlay button trigger immediate state-machine termination within one frame. |

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           HexZie UI (Jetpack Compose)                       │
│   ┌───────────────┐  ┌───────────────┐  ┌───────────────┐  ┌────────────┐   │
│   │   Home / Orb  │  │    Chat UI    │  │   Gateways    │  │ Permission │   │
│   │  (Voice Live) │  │  (Tool Cards) │  │   Manager     │  │   Center   │   │
│   └───────┬───────┘  └───────┬───────┘  └───────┬───────┘  └─────┬──────┘   │
└───────────┼──────────────────┼──────────────────┼────────────────┼──────────┘
            │                  │                  │                │
┌───────────▼──────────────────▼──────────────────▼────────────────▼──────────┐
│                             ViewModels & State                              │
│         HexzieViewModel / AgentState / PermissionState / VoiceState         │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
┌──────────────────────────────────────▼──────────────────────────────────────┐
│                              HexZie Core Agent                              │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ Agent Loop: Prompt Builder → Gateway Client → Tool Dispatcher       │   │
│   │ Context Manager (Sliding Window & Token Capper)                     │   │
│   │ Prompt-Injection Sanitizer (Untrusted Context Wrapper)              │   │
│   └───────────────────┬─────────────────────────────┬───────────────────┘   │
│                       │                             │                       │
│        ┌──────────────▼─────────────┐ ┌─────────────▼──────────────┐        │
│        │    Tool Registry & Policy  │ │     Voice & Audio Engine   │        │
│        │  - Permission Gating       │ │  - Android STT / Whisper   │        │
│        │  - HIGH-Risk Confirmation  │ │  - Android TTS Engine      │        │
│        │  - Audit Log Writer        │ │  - Porcupine Wake / PTT    │        │
│        └──────────────┬─────────────┘ └─────────────┬──────────────┘        │
└───────────────────────┼─────────────────────────────┼───────────────────────┘
                        │                             │
┌───────────────────────▼─────────────────────────────▼───────────────────────┐
│                       Android Platform Adapters                             │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────┐   │
│  │ AccessibilityService │  │ Foreground Service   │  │ CameraX / Media  │   │
│  │ (Tap, Scroll, Read)  │  │ (Mic / Notification) │  │ Projection       │   │
│  └──────────────────────┘  └──────────────────────┘  └──────────────────┘   │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────┐   │
│  │ Room Database        │  │ Encrypted DataStore  │  │ System APIs      │   │
│  │ (Sessions/Logs)      │  │ (API Keys & Config)  │  │ (SMS/Call/Apps)  │   │
│  └──────────────────────┘  └──────────────────────┘  └──────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Core Entities & Persistence
- `SessionEntity`: `id`, `title`, `createdAt`, `updatedAt`, `modelName`, `gatewayId`.
- `MessageEntity`: `id`, `sessionId`, `role` (user/assistant/tool/system), `content`, `toolCallsJson`, `toolResultJson`, `timestamp`.
- `AuditLogEntity`: `id`, `timestamp`, `toolName`, `riskLevel`, `argumentsSanitized`, `resultStatus`, `approvalSource` (auto / user_confirmed / denied).
- `GatewayConfig`: `id`, `name`, `baseUrl`, `apiKeyEncrypted`, `providerType` (OpenAI / Anthropic / Ollama / Gemini), `defaultModel`, `isEnabled`.

### Tool Registry Specification (30+ Built-in Tools)
- **UI Control**: `screen_read`, `screen_tap`, `screen_long_press`, `screen_swipe`, `screen_scroll`, `screen_type_text`, `screen_find_element`, `global_action` (back, home, recents, notifications, quick settings, power), `open_app`, `list_apps`, `current_app`.
- **Vision**: `screen_capture`, `camera_capture`, `camera_live_vision`.
- **Device & System**: `set_volume`, `set_brightness`, `toggle_flashlight`, `toggle_wifi`, `toggle_bluetooth`, `set_alarm`, `set_timer`, `clipboard_read`, `clipboard_write`, `notification_read`, `notification_reply`.
- **Communications**: `call_make`, `sms_send`, `sms_read`, `contacts_search`, `calendar_read`, `calendar_create`.
- **Files & Web**: `file_read`, `file_write`, `file_list`, `web_fetch`, `web_search`, `location_get`.
- **Agent Utilities**: `ask_user`, `todo_write`, `wait`, `finish`.

---

## 5. Implementation Roadmap & Milestones

1. **Milestone 1 — Core Foundation**:
   - Update `metadata.json`, `strings.xml`, `build.gradle.kts` with required dependencies (Room, Navigation, CameraX, Moshi, OkHttp, DataStore).
   - Set up M3 Theme (`Color.kt`, `Theme.kt`, `Type.kt`) matching the #0A0A0B cybernetic dark palette.
2. **Milestone 2 — Persistence & Security**:
   - Room Database: `HexzieDatabase`, DAOs, Entities for Sessions, Messages, and AuditLog.
   - Encrypted Preferences / KeyStore storage for Gateway API keys.
3. **Milestone 3 — Gateway & LLM Engine**:
   - `LlmGatewayClient`: OpenAI/Anthropic/Ollama/Gemini SSE streaming client with tool calling and `/v1/models` scanner.
4. **Milestone 4 — Tool Registry & Safety Guard**:
   - Tool definition interface with JSON schemas, Risk Tiers (LOW/MEDIUM/HIGH), Permission check, and sanitized audit logging.
   - Prompt-injection defense wrapping untrusted data.
5. **Milestone 5 — Voice & Speech Pipeline**:
   - STT Engine (SpeechRecognizer & Audio Stream), TTS Engine (TextToSpeech with streaming utterance queues), Push-to-Talk & Porcupine Wake Engine stub.
   - Foreground Audio Service with persistent notification controls.
6. **Milestone 6 — Android Platform Integrations**:
   - `HexzieAccessibilityService` for screen reading and gestures.
   - Quick settings tile, notification listener, camera capture adapter.
7. **Milestone 7 — UI Screens & Orb Animation**:
   - Home Screen with reactive Orb state machine (Idle, Listening, Thinking, Acting, Speaking, Kill).
   - Chat Screen with expandable tool cards.
   - Gateway Settings, Permission Center (Off/Ask/Always segmented controls), Audit Log viewer, and Onboarding walkthrough.
8. **Milestone 8 — Documentation & Testing**:
   - Comprehensive `docs/LIMITATIONS.md`, `docs/PERMISSIONS.md`, `docs/ARCHITECTURE.md`, `docs/DESKTOP_BRIDGE.md`, and `README.md`.
   - Robolectric unit tests for Tool Registry, Agent Loop, and Safety Policies.
