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

class LoginActivity : AppCompatActivity() {
    private lateinit var editUsername: EditText
    private lateinit var editPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonSignup: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        editUsername = findViewById(R.id.editUsername)
        editPassword = findViewById(R.id.editPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonSignup = findViewById(R.id.buttonSignup)

        buttonLogin.setOnClickListener { loginUser() }
        buttonSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun loginUser() {
        val username = editUsername.text.toString().trim()
        val password = editPassword.text.toString().trim()

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // First check Firestore for username existence
        db.collection("users")
            .whereEqualTo("name", username)
            .get()
            .addOnSuccessListener { documents ->
                when {
                    documents.isEmpty -> {
                        Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        // Get the associated email from Firestore
                        val userEmail = documents.documents[0].getString("email") ?: ""
                        if (userEmail.isEmpty()) {
                            Toast.makeText(this, "Email not found for user", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        // Authenticate with the retrieved email
                        authenticateWithEmail(userEmail, password)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error checking username: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun authenticateWithEmail(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    verifyFirestoreUser()
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun verifyFirestoreUser() {
        val userId = mAuth.currentUser?.uid ?: ""
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "User data mismatch", Toast.LENGTH_SHORT).show()
                    mAuth.signOut()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error verifying user data", Toast.LENGTH_SHORT).show()
            }
    }
}