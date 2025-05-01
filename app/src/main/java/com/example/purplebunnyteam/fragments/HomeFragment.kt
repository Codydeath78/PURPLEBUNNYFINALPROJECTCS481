package com.example.purplebunnyteam.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatDelegate
import com.example.purplebunnyteam.R
import androidx.core.content.edit
import com.example.purplebunnyteam.LoginActivity
import com.google.firebase.auth.FirebaseAuth

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //This will inflate the layout for this fragment.
        val view =  inflater.inflate(R.layout.fragment_home, container, false)

        val themeButton = view.findViewById<ImageButton>(R.id.themechangebtn)
        val profileButton = view.findViewById<ImageButton>(R.id.avatarbtn)
        val sharedPrefs = requireContext().getSharedPreferences("UserPreferences", 0)
        val darkModeEnabled = sharedPrefs.getBoolean("dark_mode_enabled", false)

        //This will set correct icon on startup.
        themeButton.setImageResource(
            if (darkModeEnabled) R.drawable.dark_mode else R.drawable.light_mode
        )

        themeButton.setOnClickListener {
            val newDarkMode = !darkModeEnabled
            sharedPrefs.edit { putBoolean("dark_mode_enabled", newDarkMode) }

            //This will set appropriate mode.
            val mode = if (newDarkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }

            AppCompatDelegate.setDefaultNightMode(mode)

            //This will recreate the activity to apply theme.
            activity?.recreate()
        }

        profileButton.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Replace current fragment with AccountFragment
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fContainer, AccountFragment())
                    .commit()
            }
        }




        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}