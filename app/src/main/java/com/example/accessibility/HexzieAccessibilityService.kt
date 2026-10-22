package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class HexzieAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceConnected = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can be observed if needed
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
            serviceConnected = false
        }
    }

    suspend fun getCompactScreenTree(): String = withContext(Dispatchers.Default) {
        val root = rootInActiveWindow ?: return@withContext "Empty screen or window content unavailable."
        val sb = StringBuilder()
        sb.append("Current Screen Hierarchy:\n")
        var nodeCount = 0

        fun walk(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || nodeCount > 150) return
            val rect = Rect()
            node.getBoundsInScreen(rect)

            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            val viewId = node.viewIdResourceName?.substringAfterLast('/')

            val isClickable = node.isClickable
            val isEditable = node.isEditable
            val isScrollable = node.isScrollable

            val hasContent = !text.isNullOrEmpty() || !desc.isNullOrEmpty() || isClickable || isEditable

            if (hasContent) {
                nodeCount++
                val indent = "  ".repeat(depth)
                val type = node.className?.toString()?.substringAfterLast('.') ?: "View"
                sb.append("$indent- [$type] bounds=(${rect.left},${rect.top},${rect.right},${rect.bottom})")
                if (!viewId.isNullOrEmpty()) sb.append(" id='$viewId'")
                if (!text.isNullOrEmpty()) sb.append(" text=\"$text\"")
                if (!desc.isNullOrEmpty()) sb.append(" desc=\"$desc\"")
                if (isClickable) sb.append(" [clickable]")
                if (isEditable) sb.append(" [editable]")
                if (isScrollable) sb.append(" [scrollable]")
                sb.append("\n")
            }

            for (i in 0 until node.childCount) {
                walk(node.getChild(i), depth + 1)
            }
        }

        walk(root, 0)
        if (sb.length > 2500) {
            sb.setLength(2500)
            sb.append("\n... [Truncated]")
        }
        sb.toString()
    }

    suspend fun findElement(query: String): List<AccessibilityNodeInfo> = withContext(Dispatchers.Default) {
        val root = rootInActiveWindow ?: return@withContext emptyList()
        val results = mutableListOf<AccessibilityNodeInfo>()
        val byText = root.findAccessibilityNodeInfosByText(query)
        if (!byText.isNullOrEmpty()) results.addAll(byText)
        results
    }

    suspend fun performTap(x: Float, y: Float): Boolean = withContext(Dispatchers.Main) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        val deferred = CompletableDeferred<Boolean>()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                deferred.complete(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                deferred.complete(false)
            }
        }, null)
        deferred.await()
    }

    suspend fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300): Boolean = withContext(Dispatchers.Main) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        val deferred = CompletableDeferred<Boolean>()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                deferred.complete(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                deferred.complete(false)
            }
        }, null)
        deferred.await()
    }

    suspend fun typeTextIntoFocusedNode(text: String): Boolean = withContext(Dispatchers.Default) {
        val root = rootInActiveWindow ?: return@withContext false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        if (focused != null && focused.isEditable) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return@withContext focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        false
    }

    suspend fun captureScreenshot(): Bitmap? = withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val deferred = CompletableDeferred<Bitmap?>()
            takeScreenshot(
                android.view.Display.DEFAULT_DISPLAY,
                Executors.newSingleThreadExecutor(),
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: ScreenshotResult) {
                        val hardwareBuffer = screenshotResult.hardwareBuffer
                        val colorSpace = screenshotResult.colorSpace
                        val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)?.copy(Bitmap.Config.ARGB_8888, false)
                        hardwareBuffer.close()
                        deferred.complete(bitmap)
                    }

                    override fun onFailure(errorCode: Int) {
                        deferred.complete(null)
                    }
                }
            )
            deferred.await()
        } else {
            null
        }
    }

    companion object {
        var instance: HexzieAccessibilityService? = null
            private set
        var serviceConnected: Boolean = false
            private set
    }
}
