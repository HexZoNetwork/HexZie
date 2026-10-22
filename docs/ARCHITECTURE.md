# HexZie System Architecture & Agent Loop

HexZie is designed around an autonomous, streaming, OpenCode-style tool execution state machine tailored for on-device Android environments.

---

## 1. System Components

```
┌─────────────────────────────────────────────────────────────┐
│                 HexZie UI (Jetpack Compose)                 │
│      Home (Orb) • Chat • Gateways • Permissions • Audit     │
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow & Actions
┌──────────────────────────────▼──────────────────────────────┐
│                      HexzieViewModel                        │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
┌──────────────▼─────────────┐   ┌─────────────▼──────────────┐
│       HexzieAgentLoop      │   │      Voice Subsystem       │
│  - Prompt & Sliding Window │   │  - Android STT / Recognizer│
│  - Gateway SSE Streaming   │   │  - Android TTS Synthesis   │
│  - Tool Protocol Parser    │   │  - Foreground Service Mic  │
│  - Safety & Injection Guard│   └────────────────────────────┘
└──────────────┬─────────────┘
               │ Tool Calls
┌──────────────▼─────────────┐
│        ToolRegistry        │
│  - 30+ Device Tools        │
│  - Permission Gating       │
│  - Audit Log Recording     │
└──────────────┬─────────────┘
               │
┌──────────────▼──────────────────────────────────────────────┐
│                   Android OS Integrations                   │
│   AccessibilityService • PackageManager • System Services   │
│   Room Database • KeyStore SecurityVault • DataStore        │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Agentic Reasoning Loop

1. **Context Preparation**: Assembles the user prompt, sliding window of conversation history (up to 20 messages), and registered tool JSON schemas.
2. **Model Call**: Streams chat completions from the active Gateway via Server-Sent Events (SSE).
3. **Tool Detection**: Detects native OpenAI function calls or fallback JSON-in-text tool commands.
4. **Safety & Permission Evaluation**:
   - Checks the app denylist and sensitive field restrictions.
   - Evaluates the user's permission tier (`OFF` halts execution; `ASK` halts for confirmation; `ALWAYS` proceeds).
   - Sanitizes parameters (redacting API keys, passwords, and tokens) before logging to Room.
5. **Tool Dispatch**: Executes the Android API or accessibility gesture.
6. **Result Ingestion**: Results are wrapped as untrusted data (`<untrusted_data source="...">`) to prevent prompt injection, and fed back into the model context for the next reasoning step.
7. **Completion & Speech**: Once the model produces a final text answer without further tool calls, HexZie synthesizes the spoken response via `AndroidTtsEngine`.
