package com.example.purplebunnyteam.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.purplebunnyteam.LoginActivity
import com.example.purplebunnyteam.MainActivity
import com.example.purplebunnyteam.R
import com.google.firebase.auth.FirebaseAuth

class LogOutFragment : Fragment() {


    private lateinit var yesButton: Button
    private lateinit var noButton: Button
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //return inflater.inflate(R.layout.fragment_log_out, container, false)
        val view = inflater.inflate(R.layout.fragment_log_out, container, false)

        yesButton = view.findViewById(R.id.button)
        noButton = view.findViewById(R.id.button2)

        yesButton.setOnClickListener {
            showConfirmationDialog()
        }

        noButton.setOnClickListener {
            // Redirect to MainActivity
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            return@setOnClickListener
        }
        return view
    }


    private fun showConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out of your account?")
            .setPositiveButton("Yes") { _, _ ->
                //auth.signOut()
                FirebaseAuth.getInstance().signOut()
                showSuccessDialog()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logged Out")
            .setMessage("You have successfully logged out. Take care!")
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