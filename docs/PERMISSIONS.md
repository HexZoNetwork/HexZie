# HexZie Permission Architecture & Capability Tiers

HexZie operates on a transparent, revocable, three-state capability model: **Off**, **Ask every time**, and **Always allow**.

---

## 1. Permission Tiers

| State | Behavior | Indicator Color |
|---|---|---|
| **Off** | Tool is completely disabled. The model is informed that this capability is disabled by the user. | Gray / Muted |
| **Ask** | Agent pauses before execution and displays an on-screen confirmation card with exact parameters. | Amber (`#F59E0B`) |
| **Always** | Tool executes autonomously without stopping the agent loop. | Green (`#22C55E`) |

> **Safe Mode**: When enabled in the In-App Permission Center, Safe Mode overrides all settings and forces "Ask every time" for every tool.

---

## 2. Capability Groups (17 Groups)

1. `screen_read` — Reads visible accessibility node hierarchy (text, IDs, bounds).
2. `screen_control` — Dispatches tap, swipe, scroll, and text typing gestures.
3. `screen_capture` — Captures screenshots for multimodal reasoning.
4. `camera` — Captures photos via camera.
5. `microphone` — Streaming STT and wake-word listening.
6. `apps` — Queries installed apps and launches packages.
7. `device_settings` — Controls volume, flashlight, alarms, and timers.
8. `phone_calls` — Initiates phone calls (HIGH risk).
9. `sms` — Sends and reads SMS text messages (HIGH risk).
10. `contacts` — Searches device contact book.
11. `calendar` — Reads upcoming calendar events.
12. `files` — Reads and writes authorized app storage files.
13. `clipboard` — Inspects and writes to system clipboard.
14. `location` — Retrieves GPS / coarse location coordinates.
15. `web_access` — Performs HTTP web fetches.
16. `notifications` — Reads and manages active notifications.
17. `agent_utilities` — Clarifying questions, scratchpad todos, and pauses.

---

## 3. Special Android Access Requirements

- **Accessibility Service**: Enables screen reading and UI control gestures.
- **Microphone**: Foreground service with persistent notification.
- **Notification Access**: `NotificationListenerService` for incoming alert analysis.
- **Default Digital Assistant**: Registered via `VoiceInteractionService`.
