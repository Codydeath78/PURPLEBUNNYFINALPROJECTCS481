package com.example.purplebunnyteam.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
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

        val announcementTextView = view.findViewById<TextView>(R.id.announcementsmsg)

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

        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { userDoc ->
                    val favoritesRefs = userDoc.get("favorites") as? List<*> ?: emptyList<Any>()
                    val formattedPlaceIds = favoritesRefs.mapNotNull { ref ->
                        if (ref is com.google.firebase.firestore.DocumentReference) ref.id else null
                    }

                    val announcementTexts = mutableListOf<String>()

                    if (formattedPlaceIds.isNotEmpty()) {
                        val chunks = formattedPlaceIds.chunked(10)

                        for (chunk in chunks) {
                            db.collection("announcements")
                                .whereIn("placeId", chunk)
                                .get()
                                .addOnSuccessListener { announcementDocs ->
                                    for (doc in announcementDocs) {
                                        val title = doc.getString("title") ?: "No Title"
                                        val message = doc.getString("message") ?: "No Message"
                                        announcementTexts.add("📢 $title:\n$message")
                                    }
                                    if (announcementTexts.isNotEmpty()) {
                                        announcementTextView.text = announcementTexts.joinToString("\n\n")
                                    } else {
                                        announcementTextView.text = "No recent announcements."
                                    }
                                    Log.d("HomeFragment", "Loaded ${announcementTexts.size} announcements.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("HomeFragment", "Failed to load announcements: ${e.message}")
                                    announcementTextView.text = "Failed to load announcements."
                                }
                        }
                    } else {
                        announcementTextView.text = "No favorites found."
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("HomeFragment", "Failed to load user doc: ${e.message}")
                    announcementTextView.text = "Failed to load bookmarks."
                }
        } else {
            announcementTextView.text = "Please log in to see announcements."
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