package com.example.purplebunnyteam.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.purplebunnyteam.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.purplebunnyteam.SettingsAdapter
import com.example.purplebunnyteam.SettingsItem

class SettingsFragment : Fragment() {

    private lateinit var settingsRecyclerView: RecyclerView
    private lateinit var settingsAdapter: SettingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        settingsRecyclerView = view.findViewById(R.id.settingsRecyclerView)
        settingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val settingsList = listOf(
            SettingsItem("Notification Settings", "Change your notification settings"),
            SettingsItem("Account Settings", "Change your account information"),
            SettingsItem("Location Settings", "Change your location settings"),
            SettingsItem("Delete Account", "Delete your existing account"),
            SettingsItem("Log Out", "Log out of your existing account")
        )

        settingsAdapter = SettingsAdapter(settingsList)
        settingsRecyclerView.adapter = settingsAdapter

        return view
    }
}
