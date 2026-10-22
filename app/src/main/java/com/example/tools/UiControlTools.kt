package com.example.tools

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.accessibility.HexzieAccessibilityService
import com.example.agent.SafetyPolicy
import com.example.data.model.RiskLevel

class ScreenReadTool : Tool {
    override val name = "screen_read"
    override val description = "Reads the visible on-screen element hierarchy (labels, text, bounds, clickable and editable states) using accessibility."
    override val parametersJsonSchema = """{"type": "object", "properties": {}}"""
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissionKey = "screen_read"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val service = HexzieAccessibilityService.instance
            ?: return ToolResult.failure("Accessibility Service is not enabled. Please enable HexZie in Android Accessibility Settings.")
        val tree = service.getCompactScreenTree()
        return ToolResult.success(SafetyPolicy.wrapUntrustedData("screen", tree))
    }
}

class ScreenTapTool : Tool {
    override val name = "screen_tap"
    override val description = "Simulates a tap gesture at the specified screen coordinates (x, y)."
    override val parametersJsonSchema = """{"type": "object", "properties": {"x": {"type": "number", "description": "X coordinate in pixels"}, "y": {"type": "number", "description": "Y coordinate in pixels"}}, "required": ["x", "y"]}"""
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissionKey = "screen_control"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val service = HexzieAccessibilityService.instance
            ?: return ToolResult.failure("Accessibility Service is not enabled.")
        val x = (arguments["x"] as? Number)?.toFloat() ?: return ToolResult.failure("Missing x coordinate")
        val y = (arguments["y"] as? Number)?.toFloat() ?: return ToolResult.failure("Missing y coordinate")

        val success = service.performTap(x, y)
        return if (success) ToolResult.success("Tapped at ($x, $y)") else ToolResult.failure("Failed to tap gesture")
    }
}

class ScreenSwipeTool : Tool {
    override val name = "screen_swipe"
    override val description = "Performs a swipe gesture between start and end coordinates."
    override val parametersJsonSchema = """{"type": "object", "properties": {"startX": {"type": "number"}, "startY": {"type": "number"}, "endX": {"type": "number"}, "endY": {"type": "number"}, "durationMs": {"type": "integer", "default": 300}}, "required": ["startX", "startY", "endX", "endY"]}"""
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissionKey = "screen_control"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val service = HexzieAccessibilityService.instance
            ?: return ToolResult.failure("Accessibility Service is not enabled.")
        val startX = (arguments["startX"] as? Number)?.toFloat() ?: return ToolResult.failure("Missing startX")
        val startY = (arguments["startY"] as? Number)?.toFloat() ?: return ToolResult.failure("Missing startY")
        val endX = (arguments["endX"] as? Number)?.toFloat() ?: return ToolResult.failure("Missing endX")
        val endY = (arguments["endY"] as? Number)?.toFloat() ?: return ToolResult.failure("Missing endY")
        val duration = (arguments["durationMs"] as? Number)?.toLong() ?: 300L

        val success = service.performSwipe(startX, startY, endX, endY, duration)
        return if (success) ToolResult.success("Swiped from ($startX, $startY) to ($endX, $endY)") else ToolResult.failure("Swipe gesture failed")
    }
}

class ScreenScrollTool : Tool {
    override val name = "screen_scroll"
    override val description = "Scrolls the screen 'up' or 'down'."
    override val parametersJsonSchema = """{"type": "object", "properties": {"direction": {"type": "string", "enum": ["up", "down"], "default": "down"}}}"""
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissionKey = "screen_control"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val service = HexzieAccessibilityService.instance ?: return ToolResult.failure("Accessibility Service not enabled.")
        val dir = arguments["direction"]?.toString() ?: "down"
        val startY = if (dir == "down") 1200f else 600f
        val endY = if (dir == "down") 500f else 1300f
        val success = service.performSwipe(540f, startY, 540f, endY, 350)
        return if (success) ToolResult.success("Scrolled $dir") else ToolResult.failure("Scroll failed")
    }
}

class ScreenTypeTextTool : Tool {
    override val name = "screen_type_text"
    override val description = "Types text into the currently focused editable input field."
    override val parametersJsonSchema = """{"type": "object", "properties": {"text": {"type": "string", "description": "Text to type"}}, "required": ["text"]}"""
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissionKey = "screen_control"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val service = HexzieAccessibilityService.instance ?: return ToolResult.failure("Accessibility Service not enabled.")
        val text = arguments["text"]?.toString() ?: return ToolResult.failure("Missing text")
        val success = service.typeTextIntoFocusedNode(text)
        return if (success) ToolResult.success("Typed: '$text'") else ToolResult.failure("No focused editable field found.")
    }
}

class GlobalActionTool : Tool {
    override val name = "global_action"
    override val description = "Performs an Android system action: 'back', 'home', 'recents', 'notifications', 'quick_settings', 'lock_screen', 'screenshot', 'power_dialog'."
    override val parametersJsonSchema = """{"type": "object", "properties": {"action": {"type": "string", "enum": ["back", "home", "recents", "notifications", "quick_settings", "lock_screen", "screenshot", "power_dialog"]}}, "required": ["action"]}"""
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissionKey = "screen_control"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val service = HexzieAccessibilityService.instance ?: return ToolResult.failure("Accessibility Service not enabled.")
        val action = arguments["action"]?.toString()?.lowercase() ?: return ToolResult.failure("Missing action")
        val code = when (action) {
            "back" -> AccessibilityService.GLOBAL_ACTION_BACK
            "home" -> AccessibilityService.GLOBAL_ACTION_HOME
            "recents" -> AccessibilityService.GLOBAL_ACTION_RECENTS
            "notifications" -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
            "quick_settings" -> AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
            "power_dialog" -> AccessibilityService.GLOBAL_ACTION_POWER_DIALOG
            "lock_screen" -> AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
            "screenshot" -> AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
            else -> return ToolResult.failure("Unknown action: $action")
        }
        val success = service.performGlobalAction(code)
        return if (success) ToolResult.success("Executed global action '$action'") else ToolResult.failure("Failed to execute global action '$action'")
    }
}

class OpenAppTool : Tool {
    override val name = "open_app"
    override val description = "Launches an application by its package name or common app name (e.g. 'youtube', 'maps', 'camera', 'chrome')."
    override val parametersJsonSchema = """{"type": "object", "properties": {"app_name": {"type": "string", "description": "Package name or common app name"}}, "required": ["app_name"]}"""
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissionKey = "apps"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val query = arguments["app_name"]?.toString()?.trim() ?: return ToolResult.failure("Missing app_name")
        val pm = context.packageManager

        // Direct package launch
        var launchIntent = pm.getLaunchIntentForPackage(query)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return ToolResult.success("Opened $query")
        }

        // Fuzzy match installed packages
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installed) {
            val label = pm.getApplicationLabel(app).toString()
            if (label.contains(query, ignoreCase = true) || app.packageName.contains(query, ignoreCase = true)) {
                val intent = pm.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return ToolResult.success("Opened $label (${app.packageName})")
                }
            }
        }

        return ToolResult.failure("Could not find installed application matching '$query'")
    }
}

class ListAppsTool : Tool {
    override val name = "list_apps"
    override val description = "Lists installed user-launchable applications with names and package IDs."
    override val parametersJsonSchema = """{"type": "object", "properties": {}}"""
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissionKey = "apps"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveList = pm.queryIntentActivities(mainIntent, 0)
        val sb = StringBuilder()
        sb.append("Installed Apps (${resolveList.size}):\n")
        for (info in resolveList.take(60)) {
            val label = info.loadLabel(pm)
            sb.append("- $label [${info.activityInfo.packageName}]\n")
        }
        return ToolResult.success(sb.toString())
    }
}
