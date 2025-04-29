package com.example.purplebunnyteam

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth


class LoginActivity : AppCompatActivity() {
    private lateinit var editUsername: EditText
    private lateinit var editPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonSignup: Button
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        editUsername = findViewById(R.id.editUsername)
        editPassword = findViewById(R.id.editPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonSignup = findViewById(R.id.buttonSignup)

        buttonLogin.setOnClickListener { loginUser() }

        buttonSignup.setOnClickListener {
            startActivity(
                Intent(
                    this@LoginActivity,
                    SignupActivity::class.java
                )
            )
        }
    }

    private fun loginUser() {
        val email = editUsername.text.toString()
        val password = editPassword.text.toString()

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(
                this
            ) { task: Task<AuthResult?> ->
                if (task.isSuccessful) {
                    mAuth.currentUser
                    Toast.makeText(
                        this@LoginActivity,
                        "Login successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Go to home page or dashboard here if you want
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Incorrect username/password.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
