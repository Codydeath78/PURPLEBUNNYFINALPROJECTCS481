package com.example.purplebunnyteam.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.purplebunnyteam.Bookmark
import com.example.purplebunnyteam.BookmarkAdapter
import com.example.purplebunnyteam.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class BookmarkFragment : Fragment() {

    private lateinit var adapter: BookmarkAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bookmark, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvBookmarks)
        val emptyState = view.findViewById<TextView>(R.id.tvEmptyState)
        val notsignedinState = view.findViewById<TextView>(R.id.tvNotSignedInState)

        val userauthId = FirebaseAuth.getInstance().currentUser?.uid

        if (userauthId != null) {

            adapter = BookmarkAdapter().apply {
                setEmptyStateView(emptyState)
            }
        }
        else {
            adapter = BookmarkAdapter().apply {
                setEmptyStateView(notsignedinState)
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Fetch bookmarks from Firestore
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val favorites = userDoc.get("favorites") as? List<DocumentReference> ?: emptyList()
                val bookmarks = mutableListOf<Bookmark>()

                if (favorites.isEmpty()) {
                    adapter.submitList(emptyList())
                    return@addOnSuccessListener
                }

                favorites.forEach { cafeRef ->
                    cafeRef.get().addOnSuccessListener { cafeDoc ->
                        val bookmark = Bookmark(
                            id = cafeDoc.id,
                            name = cafeDoc.getString("name") ?: "",
                            address = cafeDoc.getString("address") ?: "",
                            imageUrl = cafeDoc.getString("imageUrl") ?: "",
                            rating = cafeDoc.getDouble("rating")?.toFloat() ?: 0.0f
                        )
                        bookmarks.add(bookmark)

                        // Submit list when all cafes are processed
                        if (bookmarks.size == favorites.size) {
                            adapter.submitList(bookmarks)
                        }
                    }.addOnFailureListener { e ->
                        Log.e("BookmarkFragment", "Error fetching cafe: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("BookmarkFragment", "Error fetching user: ${e.message}")
            }
    }
}