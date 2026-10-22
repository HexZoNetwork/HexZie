# HexZie — Voice-First On-Device AI Agent for Android

HexZie is a hands-free, voice-first, on-device-controlling AI agent for Android, comparable to Google Assistant but powered by user-chosen LLMs and an OpenCode-style tool-calling agent loop.

![HexZie Icon](app/src/main/res/drawable/hexzie_icon_1792627242421.jpg)

---

## Features

- **Hands-Free Activation**: Wake phrases ("Hey Hex", "Hexzie", "Hey Hexzie"), push-to-talk animated orb, persistent foreground notification, and default digital assistant integration.
- **Luminous Animated Orb**: Fluid Compose Canvas state machine with breathing pulse, microphone waveform rings, rotating gradient thinking sweeps, and an instant red snap kill-switch.
- **Custom AI Gateways**: Direct support for OpenAI-compatible proxies, Anthropic Messages API, Ollama (local network), and Gemini with high-thinking mode. Endpoint model scanner lists available models dynamically.
- **30+ Device Tools**: Screen reading & UI inspection via AccessibilityService, tap/swipe gestures, app opening, camera, media volume, flashlight, alarms, contacts, SMS, calendar, web fetch, and local file storage.
- **In-App Permission Center**: 3-state control per capability group (`Off` / `Ask every time` / `Always allow`), Master Safe Mode toggle, and emergency Revoke All action.
- **Prompt-Injection Defense**: Untrusted text from screens, SMS, web pages, and notifications is wrapped in `<untrusted_data>` fences. Malicious instructions are automatically blocked.
- **Audit Logging**: Every tool execution is recorded in a local Room database with credentials redacted and viewable in-app.
- **Keystore Encryption**: All gateway API keys are stored encrypted via Android KeyStore (AES-256 GCM).

---

## Architecture & Project Structure

- `app/src/main/java/com/example/`
  - `accessibility/` — `HexzieAccessibilityService` (screen inspection, gestures, screenshots).
  - `agent/` — `HexzieAgentLoop`, `SafetyPolicy` (prompt injection defense & hard blocks).
  - `data/` — Room database (`HexzieDatabase`), entities, DAOs, KeyStore `SecurityVault`, and DataStore preferences.
  - `llm/` — Custom Gateways (OpenAI, Anthropic, Ollama, Gemini), SSE streaming client, and `ToolProtocolParser`.
  - `tools/` — 30+ tools implementing the `Tool` interface with JSON schema validation.
  - `voice/` — Streaming SpeechRecognizer STT, TextToSpeech with sentence streaming and barge-in, foreground service, and assistant tile.
  - `ui/` — Cybernetic dark-first Material 3 UI (`HexzieOrb`, `ToolCallCard`, `SegmentedPermissionControl`, Screens, and ViewModel).

---

## Documentation

- [Architecture & Loop Specification](docs/ARCHITECTURE.md)
- [Permission Architecture & Tiers](docs/PERMISSIONS.md)
- [Android Platform Constraints & Limitations](docs/LIMITATIONS.md)
- [Cross-Platform & Desktop Bridge](docs/DESKTOP_BRIDGE.md)
