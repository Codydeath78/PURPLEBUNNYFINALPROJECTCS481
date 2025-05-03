package com.example.purplebunnyteam.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.bumptech.glide.Glide
import com.example.purplebunnyteam.LoginActivity
import com.example.purplebunnyteam.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    companion object {
        var cachedProfileResId: Int? = null
    }

    private lateinit var profileButton: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //This will inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val themeButton = view.findViewById<ImageButton>(R.id.themechangebtn)
        profileButton = view.findViewById(R.id.avatarbtn)
        val sharedPrefs = requireContext().getSharedPreferences("UserPreferences", 0)
        val darkModeEnabled = sharedPrefs.getBoolean("dark_mode_enabled", false)

        //This will set correct icon on startup
        themeButton.setImageResource(
            if (darkModeEnabled) R.drawable.dark_mode else R.drawable.light_mode
        )

        themeButton.setOnClickListener {
            val newDarkMode = !darkModeEnabled
            sharedPrefs.edit { putBoolean("dark_mode_enabled", newDarkMode) }
            //This will set appropriate mode.
            val mode = if (newDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
            //This will recreate the activity to apply theme
            activity?.recreate()
        }

        profileButton.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                startActivity(Intent(requireContext(), LoginActivity::class.java))
            } else {
                //This will replace current fragment with AccountFragment
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fContainer, AccountFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        loadProfileImageFromCacheOrFirestore()

        //This will listen for avatar updates from AccountFragment
        parentFragmentManager.setFragmentResultListener("avatar_update", viewLifecycleOwner) { _, bundle ->
            val imageName = bundle.getString("profileImageName")
            if (!imageName.isNullOrEmpty()) {
                val resId = resources.getIdentifier(imageName, "drawable", requireContext().packageName)
                if (resId != 0) {
                    cachedProfileResId = resId
                    sharedPrefs.edit {
                        putString("profileImageName", imageName)
                    }
                    Glide.with(this)
                        .load(resId)
                        .placeholder(R.drawable.avatar)
                        .error(R.drawable.avatar)
                        .into(profileButton)
                }
            }
        }

        return view
    }

    private fun loadProfileImageFromCacheOrFirestore() {
        // Instantly load profile image from memory cache or SharedPreferences
        val sharedPrefs = requireContext().getSharedPreferences("UserPreferences", 0)
        val storedImageName = sharedPrefs.getString("profileImageName", null)
        if (cachedProfileResId != null && isAdded) {
            // Load from memory cache
            Glide.with(this)
                .load(cachedProfileResId)
                .placeholder(R.drawable.avatar)
                .error(R.drawable.avatar)
                .into(profileButton)
        } else if (!storedImageName.isNullOrEmpty()) {
            // Load from SharedPreferences cache
            val resId = resources.getIdentifier(storedImageName, "drawable", requireContext().packageName)
            if (resId != 0 && isAdded) {
                cachedProfileResId = resId
                Glide.with(this)
                    .load(resId)
                    .placeholder(R.drawable.avatar)
                    .error(R.drawable.avatar)
                    .into(profileButton)
            }
        } else {
            // Load from Firestore
            val user = FirebaseAuth.getInstance().currentUser
            user?.uid?.let { uid ->
                FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        val imageName = doc.getString("profileImageName")
                        if (!imageName.isNullOrEmpty() && isAdded) {
                            val resId = resources.getIdentifier(imageName, "drawable", requireContext().packageName)
                            if (resId != 0) {
                                cachedProfileResId = resId //This will cache it
                                sharedPrefs.edit {
                                    putString("profileImageName", imageName)
                                }
                                Glide.with(this)
                                    .load(resId)
                                    .placeholder(R.drawable.avatar)
                                    .error(R.drawable.avatar)
                                    .into(profileButton)
                            }
                        }
                    }
            }
        }
    }
}