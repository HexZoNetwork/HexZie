# HexZie Desktop & Windows Bridge

This document describes how HexZie connects and integrates across desktop operating systems (Windows, macOS, Linux) with the Android agent.

---

## 1. Overview
As confirmed in the project specification, HexZie's primary execution engine runs natively on Android (`.apk`). For cross-platform desktop control (Windows `.exe`), HexZie supports two primary bridge topologies:

### Architecture A: Local Network Gateway (Recommended)
HexZie on Android communicates with desktop LLM runtimes running on Windows:
- **Ollama on Windows**: `http://<your-pc-ip>:11434`
- **LM Studio on Windows**: `http://<your-pc-ip>:1234/v1`
- **LocalAI on Windows**: `http://<your-pc-ip>:8080/v1`

In HexZie's **Gateways** screen, enter your desktop machine's local IP and port, tap **Scan Models**, and HexZie will execute queries directly against your local desktop GPU.

---

### Architecture B: Windows ADB Bridge Companion
Power users can control Windows applications from HexZie using an ADB reverse socket or desktop companion daemon:

1. **Enable Wireless Debugging / ADB on Android**:
   ```bash
   adb pair <ip>:<port>
   adb connect <ip>:<port>
   ```
2. **Reverse Port Forwarding**:
   ```bash
   adb reverse tcp:11434 tcp:11434
   ```
   This routes Ollama or local LLM requests on Android directly to `http://127.0.0.1:11434` on Windows.

3. **Desktop Script Execution**:
   HexZie's `web_fetch` and custom webhook tools can trigger local automation scripts running on Windows via tools like AutoHotkey or PowerShell web listeners.
