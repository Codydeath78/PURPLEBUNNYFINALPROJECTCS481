package com.example.purplebunnyteam

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatFragment : Fragment(), ReviewAdapter.ReviewActionListener {
    private lateinit var chatAdapter: ReviewAdapter
    private var cafeId: String? = null
    private var cafeName: String? = null
    private var cafeImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            cafeId = it.getString("cafeId")
            cafeName = it.getString("cafeName")
            cafeImageUrl = it.getString("cafeImageUrl")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chatRecyclerView = view.findViewById<RecyclerView>(R.id.chatRecyclerView)
        val emptyStateView = view.findViewById<LinearLayout>(R.id.emptyStateView)

        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        chatAdapter = ReviewAdapter().apply {
            setEmptyStateView(emptyStateView)
            setReviewActionListener(this@ChatFragment)
            if (cafeId != null) {
                fetchCafeSpecificReviews(cafeId!!)
            } else {
                fetchAllCafes()
            }
        }

        chatRecyclerView.adapter = chatAdapter
    }

    override fun onFetchCafeReviews(cafeId: String) {
        fetchCafeSpecificReviews(cafeId)
    }

    override fun onLikeReview(review: Review) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val reviewRef = db.collection("review_comments").document(review.reviewId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(reviewRef)
            val currentLikes = snapshot.get("likes") as? List<String> ?: emptyList()

            val newLikes = if (review.isLiked) {
                currentLikes - userId
            } else {
                currentLikes + userId
            }

            transaction.update(reviewRef, "likes", newLikes)
        }.addOnSuccessListener {
            if (cafeId != null) {
                fetchCafeSpecificReviews(cafeId!!)
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Failed to update like", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchAllCafes() {
        val db = FirebaseFirestore.getInstance()
        db.collection("cafes").get()
            .addOnSuccessListener { result ->
                val cafes = result.map { doc ->
                    Review(
                        cafeId = doc.id,
                        comment = doc.getString("name") ?: "",
                        ratings = mapOf("overall" to (doc.getDouble("rating") ?: 0.0)),
                        userId = "",
                        address = doc.getString("address") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: ""
                    )
                }
                chatAdapter.updateData(cafes, showHeader = false)
            }
            .addOnFailureListener { exception ->
                Log.w("ChatFragment", "Error getting cafes", exception)
                Toast.makeText(requireContext(), "Failed to load cafes", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchCafeSpecificReviews(cafeId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("review_comments")
            .whereEqualTo("cafeId", cafeId)
            .get()
            .addOnSuccessListener { result ->
                val reviews = result.map { doc ->
                    val likes = (doc.get("likes") as? List<*>)?.size ?: 0
                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val userLiked = (doc.get("likes") as? List<String> ?: emptyList()).contains(userId)

                    Review(
                        reviewId = doc.id,
                        userId = doc.getString("userId") ?: "",
                        cafeId = cafeId,
                        comment = doc.getString("text") ?: "",
                        ratings = doc.get("ratings") as? Map<String, Double> ?: emptyMap(),
                        likes = likes,
                        isLiked = userLiked,
                        timestamp = doc.getDate("timestamp"),
                        address = doc.getString("address") ?:""
                    )
                }
                chatAdapter.updateData(
                    reviews,
                    showHeader = true,
                    cafeName = this.cafeName,
                    cafeImageUrl = this.cafeImageUrl,
                    cafeId = this.cafeId
                )
            }
            .addOnFailureListener { exception ->
                Log.w("ChatFragment", "Error getting reviews", exception)
                Toast.makeText(requireContext(), "Failed to load reviews", Toast.LENGTH_SHORT).show()
                chatAdapter.updateData(
                    emptyList(),
                    showHeader = true,
                    cafeName = this.cafeName,
                    cafeImageUrl = this.cafeImageUrl,
                    cafeId = this.cafeId
                )
            }
    }

    companion object {
        fun newInstance(cafeId: String?, cafeName: String?, cafeImageUrl: String?) =
            ChatFragment().apply {
                arguments = Bundle().apply {
                    putString("cafeId", cafeId)
                    putString("cafeName", cafeName)
                    putString("cafeImageUrl", cafeImageUrl)
                }
            }
    }
}