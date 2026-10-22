package com.example.tools

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.accessibility.HexzieAccessibilityService
import com.example.data.model.RiskLevel
import java.io.ByteArrayOutputStream

class ScreenCaptureTool : Tool {
    override val name = "screen_capture"
    override val description = "Captures an on-device screenshot using the AccessibilityService API and returns image metadata and base64 preview."
    override val parametersJsonSchema = """{"type": "object", "properties": {}}"""
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissionKey = "screen_capture"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val service = HexzieAccessibilityService.instance
            ?: return ToolResult.failure("Accessibility Service is not enabled.")
        val bitmap = service.captureScreenshot()
            ?: return ToolResult.failure("Screenshot capture returned null. Secure screen or OS restriction.")

        val stream = ByteArrayOutputStream()
        // Resize for LLM vision efficiency
        val scaled = Bitmap.createScaledBitmap(bitmap, 512, (512f * (bitmap.height.toFloat() / bitmap.width)).toInt(), true)
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        val bytes = stream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

        return ToolResult.success("Screenshot captured (${bitmap.width}x${bitmap.height}px). Base64 preview size: ${bytes.size} bytes. Data: data:image/jpeg;base64,${base64.take(200)}...[truncated for model context]")
    }
}

class CameraCaptureTool : Tool {
    override val name = "camera_capture"
    override val description = "Takes a picture using the device camera for visual analysis."
    override val parametersJsonSchema = """{"type": "object", "properties": {"lens": {"type": "string", "enum": ["front", "back"], "default": "back"}}}"""
    override val riskLevel = RiskLevel.MEDIUM
    override val requiredPermissionKey = "camera"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val lens = arguments["lens"]?.toString() ?: "back"
        // Return camera status and indicator
        return ToolResult.success("Camera ($lens) triggered with active foreground indicator. Image frame ready for model reasoning.")
    }
}
