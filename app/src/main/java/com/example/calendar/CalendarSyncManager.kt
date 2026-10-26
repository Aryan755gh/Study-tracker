package com.example.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.model.AcademicDay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Handles syncing academic study sessions, deadlines, and Pomodoro blocks
 * with Google Calendar on the Android device.
 */
object CalendarSyncManager {

    /**
     * Checks if calendar permissions are granted.
     */
    fun hasCalendarPermissions(context: Context): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    /**
     * Finds the primary Google Calendar ID on the device.
     */
    fun getPrimaryGoogleCalendarId(context: Context): Long? {
        if (!hasCalendarPermissions(context)) return null

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY
        )

        val uri = CalendarContract.Calendars.CONTENT_URI
        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                null
            )
            cursor?.use {
                var fallbackId: Long? = null
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val accountType = it.getString(2) ?: ""
                    val isPrimary = it.getInt(3) == 1

                    // Prefer Google account calendar
                    if (accountType.contains("google", ignoreCase = true) && isPrimary) {
                        return id
                    }
                    if (accountType.contains("google", ignoreCase = true) && fallbackId == null) {
                        fallbackId = id
                    }
                }
                return fallbackId ?: 1L
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Inserts an academic study event directly into Google Calendar via ContentProvider,
     * including automatic 15-minute and 30-minute reminder notifications.
     */
    fun addEventDirectly(
        context: Context,
        calendarId: Long,
        title: String,
        description: String,
        startMillis: Long,
        endMillis: Long
    ): Uri? {
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, "StudyOS: $title")
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }

        return try {
            val eventUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (eventUri != null) {
                val eventId = eventUri.lastPathSegment?.toLongOrNull()
                if (eventId != null) {
                    // Add 15 min reminder
                    val reminder15 = ContentValues().apply {
                        put(CalendarContract.Reminders.EVENT_ID, eventId)
                        put(CalendarContract.Reminders.MINUTES, 15)
                        put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                    }
                    context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminder15)

                    // Add 30 min reminder
                    val reminder30 = ContentValues().apply {
                        put(CalendarContract.Reminders.EVENT_ID, eventId)
                        put(CalendarContract.Reminders.MINUTES, 30)
                        put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                    }
                    context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminder30)
                }
            }
            eventUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Intent fallback that opens Google Calendar directly with pre-filled details.
     * Guaranteed to work even without runtime permissions.
     */
    fun openAddEventIntent(
        context: Context,
        title: String,
        description: String,
        startMillis: Long,
        durationMinutes: Int
    ) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "StudyOS: $title")
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(
                CalendarContract.EXTRA_EVENT_END_TIME,
                startMillis + durationMinutes * 60 * 1000L
            )
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No Calendar app found", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generates iCalendar (.ics) format string containing the entire academic plan
     * for seamless import into Google Calendar Web/App.
     */
    fun generateIcsExport(days: List<AcademicDay>): String {
        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\n")
        sb.append("VERSION:2.0\n")
        sb.append("PRODID:-//StudyOS Academic Tracker//EN\n")
        sb.append("CALSCALE:GREGORIAN\n")
        sb.append("METHOD:PUBLISH\n")

        val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val baseCal = Calendar.getInstance()
        baseCal.set(Calendar.HOUR_OF_DAY, 14) // 2:00 PM study block
        baseCal.set(Calendar.MINUTE, 0)
        baseCal.set(Calendar.SECOND, 0)

        days.forEachIndexed { index, day ->
            val eventCal = (baseCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, index)
            }
            val startStr = sdf.format(eventCal.time)
            eventCal.add(Calendar.HOUR_OF_DAY, 3)
            val endStr = sdf.format(eventCal.time)

            sb.append("BEGIN:VEVENT\n")
            sb.append("UID:studyos-${day.id}-${System.currentTimeMillis()}@studyos.app\n")
            sb.append("DTSTAMP:$startStr\n")
            sb.append("DTSTART:$startStr\n")
            sb.append("DTEND:$endStr\n")
            sb.append("SUMMARY:StudyOS — ${day.title} (${day.tag})\n")
            val desc = day.groups.joinToString("\\n") { g ->
                "${g.groupTitle}: " + g.tasks.joinToString("; ")
            }
            sb.append("DESCRIPTION:$desc\n")
            sb.append("STATUS:CONFIRMED\n")
            sb.append("BEGIN:VALARM\n")
            sb.append("TRIGGER:-PT30M\n")
            sb.append("ACTION:DISPLAY\n")
            sb.append("DESCRIPTION:Reminder: Study session starts in 30 minutes\n")
            sb.append("END:VALARM\n")
            sb.append("END:VEVENT\n")
        }

        sb.append("END:VCALENDAR\n")
        return sb.toString()
    }
}
