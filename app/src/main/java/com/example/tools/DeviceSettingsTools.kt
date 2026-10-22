package com.example.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.provider.AlarmClock
import com.example.data.model.RiskLevel

class SetVolumeTool : Tool {
    override val name = "set_volume"
    override val description = "Sets device media or ring volume from 0 to 100 percent."
    override val parametersJsonSchema = """{"type": "object", "properties": {"level_percent": {"type": "integer", "minimum": 0, "maximum": 100}, "stream": {"type": "string", "enum": ["media", "ring", "alarm"], "default": "media"}}, "required": ["level_percent"]}"""
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissionKey = "device_settings"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val level = (arguments["level_percent"] as? Number)?.toInt() ?: return ToolResult.failure("Missing level_percent")
        val stream = when (arguments["stream"]?.toString()?.lowercase()) {
            "ring" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            else -> AudioManager.STREAM_MUSIC
        }
        val max = am.getStreamMaxVolume(stream)
        val target = ((level.coerceIn(0, 100) / 100.0) * max).toInt()
        am.setStreamVolume(stream, target, AudioManager.FLAG_SHOW_UI)
        return ToolResult.success("Volume set to $level% (index $target/$max)")
    }
}

class ToggleFlashlightTool : Tool {
    override val name = "toggle_flashlight"
    override val description = "Turns the device rear camera flashlight 'on' or 'off'."
    override val parametersJsonSchema = """{"type": "object", "properties": {"enabled": {"type": "boolean"}}, "required": ["enabled"]}"""
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissionKey = "device_settings"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val enabled = arguments["enabled"] as? Boolean ?: return ToolResult.failure("Missing enabled boolean")
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cm.cameraIdList.firstOrNull() ?: return ToolResult.failure("No camera available")
            cm.setTorchMode(cameraId, enabled)
            ToolResult.success("Flashlight turned ${if (enabled) "on" else "off"}")
        } catch (e: Exception) {
            ToolResult.failure("Flashlight error: ${e.message}")
        }
    }
}

class SetAlarmTool : Tool {
    override val name = "set_alarm"
    override val description = "Sets an alarm at the specified hour (0-23) and minute (0-59) with a message."
    override val parametersJsonSchema = """{"type": "object", "properties": {"hour": {"type": "integer"}, "minute": {"type": "integer"}, "message": {"type": "string", "default": "HexZie Alarm"}}, "required": ["hour", "minute"]}"""
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissionKey = "device_settings"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val hour = (arguments["hour"] as? Number)?.toInt() ?: return ToolResult.failure("Missing hour")
        val minute = (arguments["minute"] as? Number)?.toInt() ?: return ToolResult.failure("Missing minute")
        val msg = arguments["message"]?.toString() ?: "HexZie Alarm"

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, msg)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ToolResult.success("Alarm created for %02d:%02d ('$msg')".format(hour, minute))
        } catch (e: Exception) {
            ToolResult.failure("Could not create alarm: ${e.message}")
        }
    }
}

class ClipboardReadTool : Tool {
    override val name = "clipboard_read"
    override val description = "Reads current text from the Android system clipboard."
    override val parametersJsonSchema = """{"type": "object", "properties": {}}"""
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissionKey = "clipboard"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip
        val text = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).text?.toString() else null
        return if (!text.isNullOrEmpty()) {
            ToolResult.success("Clipboard: $text")
        } else {
            ToolResult.success("Clipboard is empty.")
        }
    }
}

class ClipboardWriteTool : Tool {
    override val name = "clipboard_write"
    override val description = "Copies the provided text to the Android clipboard."
    override val parametersJsonSchema = """{"type": "object", "properties": {"text": {"type": "string"}}, "required": ["text"]}"""
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissionKey = "clipboard"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val text = arguments["text"]?.toString() ?: return ToolResult.failure("Missing text")
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("HexZie", text))
        return ToolResult.success("Copied to clipboard.")
    }
}
