package com.example.purplebunnyteam

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {
    private lateinit var editUsername: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var editConfirmPassword: EditText
    private lateinit var buttonFinishSignup: Button
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        mAuth = FirebaseAuth.getInstance()

        editUsername = findViewById(R.id.editUsername)
        editEmail = findViewById(R.id.editEmail)
        editPassword = findViewById(R.id.editPassword)
        editConfirmPassword = findViewById(R.id.editConfirmPassword)
        buttonFinishSignup = findViewById(R.id.buttonFinishSignup)

        buttonFinishSignup.setOnClickListener { signupUser() }
    }

    private fun signupUser() {
        val username = editUsername.text.toString().trim()
        val email = editEmail.text.toString().trim()
        val password = editPassword.text.toString().trim()
        val confirmPassword = editConfirmPassword.text.toString().trim()

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) ||
            TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            //Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            NotificationUtils.showToast(this, "Please fill in all fields")
            return
        }

        if (password != confirmPassword) {
            //Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            NotificationUtils.showToast(this, "Passwords do not match")
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val usersRef = firestore.collection("users")

        // Check username availability
        usersRef.whereEqualTo("name", username).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    //Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show()
                    NotificationUtils.showToast(this, "Username already taken")
                } else {
                    createFirebaseAccount(email, password, username)
                }
            }
            .addOnFailureListener {
                //Toast.makeText(this, "Error checking username: ${it.message}", Toast.LENGTH_SHORT).show()
                NotificationUtils.showToast(this, "Error checking username: ${it.message}")
            }
    }

    private fun createFirebaseAccount(email: String, password: String, username: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = mAuth.currentUser
                    if (firebaseUser != null) {
                        saveUserToFirestore(firebaseUser.uid, username, email)
                    } else {
                        //Toast.makeText(this, "User authentication failed", Toast.LENGTH_SHORT).show()
                        NotificationUtils.showToast(this, "User authentication failed")
                    }
                } else {
                    //Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    NotificationUtils.showToast(this, "Signup failed: ${task.exception?.message}")
                }
            }
    }

    private fun saveUserToFirestore(userId: String, username: String, email: String) {
        val user = User(
            userId = userId,
            name = username,
            email = email
            // profilePicture defaults to empty string
            // joinedAt will be set automatically by Firestore
        )

        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .set(user)
            .addOnSuccessListener {
                //Toast.makeText(this, "Signup Successful!", Toast.LENGTH_SHORT).show()
                NotificationUtils.showToast(this, "Signup Successful!")
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                //Toast.makeText(this, "Failed to save user: ${it.message}", Toast.LENGTH_SHORT).show()
                NotificationUtils.showToast(this, "Failed to save user: ${it.message}")
            }
    }
}
