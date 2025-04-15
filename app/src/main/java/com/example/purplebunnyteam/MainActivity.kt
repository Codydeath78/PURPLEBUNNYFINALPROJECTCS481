package com.example.purplebunnyteam

import android.content.Intent
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.findNavController
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.purplebunnyteam.fragments.BookmarkFragment
import com.example.purplebunnyteam.fragments.ChatFragment
import com.example.purplebunnyteam.fragments.HomeFragment
import com.example.purplebunnyteam.fragments.SearchFragment
import com.example.purplebunnyteam.fragments.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        /////////////////////////////////
        //if (FirebaseAuth.getInstance().currentUser == null) {
            // Redirect to LoginActivity
            //val intent = Intent(this, LoginActivity::class.java)
            //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            //startActivity(intent)
            //finish()
            //return
        //}
        ///////////////////////////// --finish later...

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