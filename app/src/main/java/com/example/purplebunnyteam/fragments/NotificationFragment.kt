package com.example.purplebunnyteam.fragments

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.purplebunnyteam.R
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.*
import androidx.core.content.edit

class NotificationFragment : Fragment() {

    private lateinit var switchMute: MaterialSwitch
    private lateinit var btnSetSilentPeriod: Button
    private lateinit var textSilentPeriod: TextView
    private lateinit var checkboxPush: CheckBox
    private lateinit var checkboxSMS: CheckBox
    private lateinit var checkboxEmail: CheckBox

    private val prefs by lazy {
        requireContext().getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val silentCheckRunnable = object : Runnable {
        override fun run() {
            val start = prefs.getInt("silent_start", -1)
            val end = prefs.getInt("silent_end", -1)

            if (start != -1 && end != -1 && isSilentPeriodExpired(start, end)) {
                prefs.edit { remove("silent_start"); remove("silent_end") }
                textSilentPeriod.text = "Silent Period: Not set"
            }

            // Run again every 1 minute
            handler.postDelayed(this, 1 * 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)

        switchMute = view.findViewById(R.id.switchMuteNotifications)
        btnSetSilentPeriod = view.findViewById(R.id.btnSetSilentPeriod)
        textSilentPeriod = view.findViewById(R.id.textCurrentSilentPeriod)
        checkboxPush = view.findViewById(R.id.checkboxPush)
        checkboxSMS = view.findViewById(R.id.checkboxSMS)
        checkboxEmail = view.findViewById(R.id.checkboxEmail)

        setupListeners()
        return view
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
        handler.post(silentCheckRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(silentCheckRunnable)
    }

    private fun loadSettings() {
        switchMute.isChecked = prefs.getBoolean("mute", false)
        checkboxPush.isChecked = prefs.getBoolean("push", true)
        checkboxSMS.isChecked = prefs.getBoolean("sms", false)
        checkboxEmail.isChecked = prefs.getBoolean("email", false)

        val start = prefs.getInt("silent_start", -1)
        val end = prefs.getInt("silent_end", -1)

        if (start != -1 && end != -1 && !isSilentPeriodExpired(start, end)) {
            textSilentPeriod.text = "Silent Period: ${formatTime(start)} to ${formatTime(end)}"
        } else {
            prefs.edit { remove("silent_start"); remove("silent_end") }
            textSilentPeriod.text = "Silent Period: Not set"
        }

        updateNotificationControlsEnabled(!switchMute.isChecked)
    }

    private fun setupListeners() {
        switchMute.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("mute", isChecked) }
            updateNotificationControlsEnabled(!isChecked)
        }

        checkboxPush.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("push", isChecked) }
        }

        checkboxSMS.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("sms", isChecked) }
        }

        checkboxEmail.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit() { putBoolean("email", isChecked) }
        }

        btnSetSilentPeriod.setOnClickListener {
            setSilentPeriod()
        }
    }

    private fun updateNotificationControlsEnabled(enabled: Boolean) {
        checkboxPush.isEnabled = enabled
        checkboxSMS.isEnabled = enabled
        checkboxEmail.isEnabled = enabled
        btnSetSilentPeriod.isEnabled = enabled
    }

    private fun setSilentPeriod() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, startHour, startMin ->
            val startTime = startHour * 60 + startMin

            TimePickerDialog(requireContext(), { _, endHour, endMin ->
                val endTime = endHour * 60 + endMin
                prefs.edit {
                    putInt("silent_start", startTime)
                    putInt("silent_end", endTime)
                }
                textSilentPeriod.text = "Silent Period: ${formatTime(startTime)} to ${formatTime(endTime)}"
            }, hour, minute, true).show()

        }, hour, minute, true).show()
    }

    private fun formatTime(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return String.format("%02d:%02d", h, m)
    }

    private fun isSilentPeriodExpired(start: Int, end: Int): Boolean {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        return if (start < end) {
            currentMinutes > end || currentMinutes < start
        } else {
            // Overnight period (e.g., 23:00 to 07:00)
            currentMinutes > end && currentMinutes < start
        }
    }
}

