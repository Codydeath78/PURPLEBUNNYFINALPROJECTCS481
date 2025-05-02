package com.example.purplebunnyteam.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.telephony.PhoneNumberUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.purplebunnyteam.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.util.*
import android.app.AlertDialog

class AccountFragment : Fragment() {

    private lateinit var imageProfile: ImageView
    private lateinit var btnChangePicture: Button
    private lateinit var editUsername: EditText
    private lateinit var editPassword: EditText
    private lateinit var editName: EditText
    private lateinit var editAddress: EditText
    private lateinit var editPhone: EditText
    private lateinit var editBio: EditText
    private lateinit var btnSave: Button

    private val auth: FirebaseAuth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()
    private val userId get() = auth.currentUser?.uid

    private var selectedProfileImage: Int = R.drawable.avatar // Default fallback

    private val defaultImages = listOf(
        R.drawable.default_1,
        R.drawable.default_2,
        R.drawable.default_3,
        R.drawable.default_4
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        imageProfile = view.findViewById(R.id.imageProfile)
        btnChangePicture = view.findViewById(R.id.btnChangePicture)
        editUsername = view.findViewById(R.id.editUsername)
        editPassword = view.findViewById(R.id.editPassword)
        editName = view.findViewById(R.id.editName)
        editAddress = view.findViewById(R.id.editAddress)
        editPhone = view.findViewById(R.id.editPhone)
        editBio = view.findViewById(R.id.editBio)
        btnSave = view.findViewById(R.id.btnSave)

        btnChangePicture.setOnClickListener { showImageSelectionDialog() }
        btnSave.setOnClickListener { saveChanges() }

        loadUserData()

        return view
    }

    private fun showImageSelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_selection, null)
        val imageContainer = dialogView.findViewById<LinearLayout>(R.id.imageContainer)

        defaultImages.forEach { resId ->
            val img = ImageView(requireContext())
            img.setImageResource(resId)
            img.setPadding(16, 16, 16, 16)
            img.layoutParams = LinearLayout.LayoutParams(200, 200)
            img.setOnClickListener {
                selectedProfileImage = resId
                imageProfile.setImageResource(resId)
            }
            imageContainer.addView(img)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Choose a profile picture")
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun loadUserData() {
        userId?.let { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    editUsername.setText(doc.getString("username"))
                    editName.setText(doc.getString("name"))
                    editAddress.setText(doc.getString("address"))
                    editPhone.setText(doc.getString("phone"))
                    editBio.setText(doc.getString("bio"))

                    val imageName = doc.getString("profileImageName")
                    imageName?.let {
                        val resId = resources.getIdentifier(it, "drawable", requireContext().packageName)
                        if (resId != 0) {
                            selectedProfileImage = resId
                            Glide.with(this).load(resId).into(imageProfile)
                        }
                    }

                    //This will check email verification status.
                    auth.currentUser?.reload()?.addOnSuccessListener {
                        val isVerified = auth.currentUser?.isEmailVerified == true
                        if (!isVerified) {
                            Toast.makeText(requireContext(), "Your email is not verified. Limited access.", Toast.LENGTH_LONG).show()
                            btnSave.isEnabled = false
                            btnSave.text = "Verify Email to Save"
                            auth.currentUser?.sendEmailVerification()
                        } else {
                            btnSave.isEnabled = true
                            btnSave.text = "Save Changes"
                        }
                    }
                }
            }
        }
    }

    private fun saveChanges() {
        val username = editUsername.text.toString().trim()
        val password = editPassword.text.toString().trim()
        val name = editName.text.toString().trim()
        val address = editAddress.text.toString().trim()
        val phone = PhoneNumberUtils.formatNumber(editPhone.text.toString().trim(), Locale.getDefault().country)
        val bio = editBio.text.toString().trim()

        if (username.isEmpty() || password.isEmpty() || name.isEmpty() || address.isEmpty() || phone.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidSocials(bio)) {
            Toast.makeText(requireContext(), "Bio contains invalid social links", Toast.LENGTH_SHORT).show()
            return
        }

        userId?.let { uid ->
            //This will check for username uniqueness.
            db.collection("users").whereEqualTo("username", username).get().addOnSuccessListener { snapshot ->
                val isUsernameTaken = snapshot.documents.any { it.id != uid }
                if (isUsernameTaken) {
                    Toast.makeText(requireContext(), "Username already taken", Toast.LENGTH_SHORT).show()
                } else {
                    val imageName = resources.getResourceEntryName(selectedProfileImage)
                    val userData = hashMapOf(
                        "username" to username,
                        "name" to name,
                        "address" to address,
                        "phone" to phone,
                        "bio" to bio,
                        "profileImageName" to imageName
                    )
                    db.collection("users").document(uid).update(userData as Map<String, Any>).addOnSuccessListener {
                        if (password.isNotEmpty()) {
                            auth.currentUser?.updatePassword(password)
                        }
                        //This will send email verification if not verified.
                        if (auth.currentUser?.isEmailVerified == false) {
                            auth.currentUser?.sendEmailVerification()?.addOnSuccessListener {
                                Toast.makeText(requireContext(), "Verification email sent.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isValidSocials(bio: String): Boolean {
        val urls = Regex("https?://[\\w./-]+")
        val foundUrls = urls.findAll(bio).map { it.value }.toList()
        return foundUrls.all { it.startsWith("https://") || it.startsWith("http://") }
    }
}