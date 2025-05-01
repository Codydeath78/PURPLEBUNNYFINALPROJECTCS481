package com.example.purplebunnyteam

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.purplebunnyteam.fragments.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val darkModeEnabled = prefs.getBoolean("dark_mode_enabled", false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val homeFragment = HomeFragment()
        val searchFragment = SearchFragment()
        val chatFragment = ChatFragment()
        val settingsFragment = SettingsFragment()
        val bookmarkFragment = BookmarkFragment()
        changeFragment(homeFragment)
        findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId = R.id.ic_home
        findViewById<BottomNavigationView>(R.id.bottom_nav).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.ic_home -> {
                    changeFragment(homeFragment)
                    true
                }
                R.id.ic_chat_bubble -> {
                    changeFragment(chatFragment)
                    true
                }
                R.id.ic_bookmark -> {
                    changeFragment(bookmarkFragment)
                    true
                }
                R.id.ic_search -> {
                    changeFragment(searchFragment)
                    true
                }
                R.id.ic_settings -> {
                    changeFragment(settingsFragment)
                    true
                }
                else -> false
            }
        }
    }
    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fContainer,fragment)
            commit()
        }
    }
}