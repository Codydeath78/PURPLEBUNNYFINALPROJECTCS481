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
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.purplebunnyteam.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.util.*

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
    private val storage = FirebaseStorage.getInstance()
    private val userId get() = auth.currentUser?.uid

    private var imageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
            imageProfile.setImageURI(imageUri)
        }
    }

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

        btnChangePicture.setOnClickListener { openGallery() }
        btnSave.setOnClickListener { saveChanges() }

        loadUserData()

        return view
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
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

                    val imageUrl = doc.getString("profileImage")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this).load(imageUrl).into(imageProfile)
                    }

                    // Check email verification status
                    val isVerified = auth.currentUser?.isEmailVerified == true
                    if (!isVerified) {
                        Toast.makeText(requireContext(), "Your email is not verified. Limited access.", Toast.LENGTH_LONG).show()
                        btnSave.isEnabled = false
                        btnSave.text = "Verify Email to Save"
                        auth.currentUser?.sendEmailVerification()
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
            // Check for username uniqueness
            db.collection("users").whereEqualTo("username", username).get().addOnSuccessListener { snapshot ->
                val isUsernameTaken = snapshot.documents.any { it.id != uid }
                if (isUsernameTaken) {
                    Toast.makeText(requireContext(), "Username already taken", Toast.LENGTH_SHORT).show()
                } else {
                    val userData = hashMapOf(
                        "username" to username,
                        "name" to name,
                        "address" to address,
                        "phone" to phone,
                        "bio" to bio
                    )

                    db.collection("users").document(uid).update(userData as Map<String, Any>).addOnSuccessListener {
                        if (password.isNotEmpty()) {
                            auth.currentUser?.updatePassword(password)
                        }

                        imageUri?.let { uri ->
                            val ref = storage.reference.child("profileImages/$uid.jpg")
                            ref.putFile(uri).addOnSuccessListener {
                                ref.downloadUrl.addOnSuccessListener { url ->
                                    db.collection("users").document(uid).update("profileImage", url.toString())
                                }
                            }
                        }

                        // Send email verification if not verified
                        if (auth.currentUser?.isEmailVerified == false) {
                            auth.currentUser?.sendEmailVerification()?.addOnSuccessListener {
                                Toast.makeText(requireContext(), "Verification email sent.", Toast.LENGTH_SHORT).show()
                            }
                        }

                        Toast.makeText(requireContext(), "Changes saved", Toast.LENGTH_SHORT).show()
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