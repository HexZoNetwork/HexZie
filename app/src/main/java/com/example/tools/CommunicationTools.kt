package com.example.tools

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.telephony.SmsManager
import com.example.agent.SafetyPolicy
import com.example.data.model.RiskLevel

class CallMakeTool : Tool {
    override val name = "call_make"
    override val description = "Initiates a phone call to the specified phone number. HIGH risk: initiates outbound connection."
    override val parametersJsonSchema = """{"type": "object", "properties": {"phone_number": {"type": "string"}}, "required": ["phone_number"]}"""
    override val riskLevel = RiskLevel.HIGH
    override val requiredPermissionKey = "phone_calls"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val number = arguments["phone_number"]?.toString() ?: return ToolResult.failure("Missing phone_number")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ToolResult.success("Opened dialer for $number")
        } catch (e: Exception) {
            ToolResult.failure("Call error: ${e.message}")
        }
    }
}

class SmsSendTool : Tool {
    override val name = "sms_send"
    override val description = "Sends an SMS text message to the recipient phone number. HIGH risk: sends real message."
    override val parametersJsonSchema = """{"type": "object", "properties": {"recipient": {"type": "string"}, "message": {"type": "string"}}, "required": ["recipient", "message"]}"""
    override val riskLevel = RiskLevel.HIGH
    override val requiredPermissionKey = "sms"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val recipient = arguments["recipient"]?.toString() ?: return ToolResult.failure("Missing recipient")
        val message = arguments["message"]?.toString() ?: return ToolResult.failure("Missing message")

        return try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(recipient, null, message, null, null)
            ToolResult.success("SMS sent to $recipient: \"$message\"")
        } catch (e: Exception) {
            // Fallback to SMS Intent if SEND_SMS permission wasn't granted at runtime
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$recipient")).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                ToolResult.success("Opened SMS composer to $recipient")
            } catch (err: Exception) {
                ToolResult.failure("Could not send SMS: ${e.message}")
            }
        }
    }
}

class ContactsSearchTool : Tool {
    override val name = "contacts_search"
    override val description = "Searches device contacts by name and returns matching names and phone numbers."
    override val parametersJsonSchema = """{"type": "object", "properties": {"query": {"type": "string"}}, "required": ["query"]}"""
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissionKey = "contacts"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val query = arguments["query"]?.toString() ?: return ToolResult.failure("Missing query")
        val resolver: ContentResolver = context.contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$query%"),
            null
        ) ?: return ToolResult.failure("Contacts permission or content resolver unavailable")

        val sb = StringBuilder()
        sb.append("Contacts matching '$query':\n")
        var count = 0
        cursor.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext() && count < 10) {
                val name = it.getString(nameIdx)
                val number = it.getString(numIdx)
                sb.append("- $name: $number\n")
                count++
            }
        }
        return if (count > 0) {
            ToolResult.success(SafetyPolicy.wrapUntrustedData("contacts", sb.toString()))
        } else {
            ToolResult.success("No contacts found matching '$query'.")
        }
    }
}

class CalendarReadTool : Tool {
    override val name = "calendar_read"
    override val description = "Reads upcoming events from the Android device calendar."
    override val parametersJsonSchema = """{"type": "object", "properties": {"days_ahead": {"type": "integer", "default": 7}}}"""
    override val riskLevel = RiskLevel.LOW
    override val requiredPermissionKey = "calendar"

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val days = (arguments["days_ahead"] as? Number)?.toInt() ?: 7
        val now = System.currentTimeMillis()
        val end = now + (days * 24L * 60L * 60L * 1000L)

        val uri = CalendarContract.Events.CONTENT_URI
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART),
            "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?",
            arrayOf(now.toString(), end.toString()),
            "${CalendarContract.Events.DTSTART} ASC"
        ) ?: return ToolResult.failure("Calendar access denied or unavailable.")

        val sb = StringBuilder()
        sb.append("Upcoming Events next $days days:\n")
        var count = 0
        cursor.use {
            val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE)
            val startIdx = it.getColumnIndex(CalendarContract.Events.DTSTART)
            while (it.moveToNext() && count < 10) {
                val title = it.getString(titleIdx)
                val dtStart = it.getLong(startIdx)
                sb.append("- $title (timestamp: $dtStart)\n")
                count++
            }
        }
        return if (count > 0) {
            ToolResult.success(SafetyPolicy.wrapUntrustedData("calendar", sb.toString()))
        } else {
            ToolResult.success("No calendar events found in the next $days days.")
        }
    }
}
