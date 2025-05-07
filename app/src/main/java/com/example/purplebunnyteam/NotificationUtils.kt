package com.example.purplebunnyteam

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.*


object NotificationUtils {

    private const val PREFS_NAME = "NotificationPrefs"

    private fun isMuted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("mute", false)
    }

    private fun isInSilentPeriod(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val start = prefs.getInt("silent_start", -1)
        val end = prefs.getInt("silent_end", -1)

        if (start == -1 || end == -1) return false

        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        return if (start < end) {
            currentMinutes in start until end
        } else {
            currentMinutes >= start || currentMinutes < end
        }
    }

    private fun shouldSuppress(context: Context): Boolean {
        return isMuted(context) || isInSilentPeriod(context)
    }

    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        if (!shouldSuppress(context)) {
            Toast.makeText(context, message, duration).show()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        text: String,
        smallIconRes: Int
    ) {
        if (shouldSuppress(context)) return

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconRes)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}