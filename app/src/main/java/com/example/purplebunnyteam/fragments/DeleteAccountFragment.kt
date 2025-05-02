package com.example.purplebunnyteam.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.purplebunnyteam.LoginActivity
import com.example.purplebunnyteam.MainActivity
import com.example.purplebunnyteam.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore

class DeleteAccountFragment : Fragment() {

    private lateinit var yesButton: Button
    private lateinit var noButton: Button
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_delete_account, container, false)

        yesButton = view.findViewById(R.id.yesbtn)
        noButton = view.findViewById(R.id.nobtn)

        yesButton.setOnClickListener {
            Toast.makeText(requireContext(), "Trying to delete account...", Toast.LENGTH_SHORT).show()
            showConfirmationDialog()
        }

        noButton.setOnClickListener {
            //This will redirect to MainActivity.
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            return@setOnClickListener
        }
        return view
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("This action is permanent. Are you sure you want to delete your account?")
            .setPositiveButton("Yes") { _, _ -> deleteAccount() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteAccount() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid
        val db = FirebaseFirestore.getInstance()
        if (currentUser == null || userId == null) {
            Toast.makeText(requireContext(), "No user signed in", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(requireContext(), "Attempting to delete user...", Toast.LENGTH_SHORT).show()

        //This will delete Firestore document
        db.collection("users").document(userId).delete().addOnSuccessListener {

            // Then it will delete the Auth user
            currentUser.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
                    showSuccessDialog()
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthRecentLoginRequiredException) {
                        Toast.makeText(requireContext(), "Needs re-auth", Toast.LENGTH_SHORT).show()
                        promptReauthentication()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Error: ${exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun promptReauthentication() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reauth, null)
        val emailField = dialogView.findViewById<EditText>(R.id.emailInput)
        val passwordField = dialogView.findViewById<EditText>(R.id.passwordInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Re-authenticate")
            .setMessage("Please re-enter your email and password to delete your account.")
            .setView(dialogView)
            .setPositiveButton("Confirm") { _, _ ->
                val email = emailField.text.toString()
                val password = passwordField.text.toString()

                val credential = EmailAuthProvider.getCredential(email, password)
                auth.currentUser?.reauthenticate(credential)?.addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        deleteAccount() //This will retry after successful re-auth.
                    } else {
                        Toast.makeText(requireContext(), "Re-authentication failed.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Account Deleted")
            .setMessage("You have successfully deleted your account.")
            .setPositiveButton("OK") { _, _ ->
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setCancelable(false)
            .show()
    }
}