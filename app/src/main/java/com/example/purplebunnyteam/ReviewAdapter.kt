package com.example.purplebunnyteam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ReviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_REVIEW = 1
        private const val TYPE_CAFE = 2
    }

    private var showHeader: Boolean = false
    private var cafeName: String? = null
    private var cafeId: String? = null
    private var cafeImageUrl: String? = null
    private var reviews: List<Review> = emptyList()
    private var emptyStateView: View? = null

    interface ReviewActionListener {
        fun onFetchCafeReviews(cafeId: String)
        fun onLikeReview(review: Review)
    }

    private var reviewActionListener: ReviewActionListener? = null

    fun setReviewActionListener(listener: ReviewActionListener) {
        this.reviewActionListener = listener
    }

    fun setEmptyStateView(view: View) {
        emptyStateView = view
        updateEmptyState()
    }

    private fun updateEmptyState() {
        emptyStateView?.visibility = if (reviews.isEmpty() && !showHeader) View.VISIBLE else View.GONE
    }

    fun updateData(
        reviews: List<Review>,
        showHeader: Boolean = false,
        cafeName: String? = null,
        cafeImageUrl: String? = null,
        cafeId: String? = null
    ) {
        this.reviews = reviews
        this.showHeader = showHeader
        this.cafeName = cafeName
        this.cafeId = cafeId
        this.cafeImageUrl = cafeImageUrl
        notifyDataSetChanged()
        updateEmptyState()
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            showHeader && position == 0 -> TYPE_HEADER
            reviews[if (showHeader) position-1 else position].userId.isEmpty() -> TYPE_CAFE
            else -> TYPE_REVIEW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.header_cafe_details, parent, false),
                cafeId,
                { cafeId?.let { reviewActionListener?.onFetchCafeReviews(it) } }
            )
            TYPE_CAFE -> CafeViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_cafe, parent, false)
            )
            else -> ReviewViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_review, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                cafeName?.let { holder.tvCafeName.text = it }

                val averageRating = if (reviews.isNotEmpty()) {
                    reviews.map { it.ratings.values.average() }.average().toFloat()
                } else {
                    0f
                }

                val address = if (reviews.isNotEmpty()) reviews.first().address else null

                holder.bind(cafeName, cafeImageUrl, averageRating, address)

                Glide.with(holder.itemView.context)
                    .load(cafeImageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(holder.ivCafeHeader)
            }
            is CafeViewHolder -> {
                val cafePosition = if (showHeader) position - 1 else position
                holder.bind(reviews[cafePosition])
            }
            is ReviewViewHolder -> {
                val reviewPosition = if (showHeader) position - 1 else position
                holder.bind(reviews[reviewPosition])
            }
        }
    }

    inner class CafeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCafeImage: ImageView = itemView.findViewById(R.id.ivCafeImage)
        private val tvCafeName: TextView = itemView.findViewById(R.id.tvCafeName)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        private val tvCafeAddress: TextView = itemView.findViewById(R.id.tvCafeAddress)

        fun bind(review: Review) {
            tvCafeName.text = review.comment
            tvCafeAddress.text = review.address
            ratingBar.rating = review.ratings.values.average().toFloat()

            if (review.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(review.imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(ivCafeImage)
            } else {
                ivCafeImage.setImageResource(R.drawable.ic_placeholder)
            }

            itemView.setOnClickListener {
                if (review.cafeId.isNotEmpty()) {
                    val context = itemView.context
                    if (context is FragmentActivity) {
                        val chatFragment = ChatFragment.newInstance(
                            review.cafeId,
                            review.comment,
                            review.imageUrl
                        )
                        context.supportFragmentManager.beginTransaction()
                            .replace(R.id.fContainer, chatFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }
        }
    }

    override fun getItemCount() = reviews.size + if (showHeader) 1 else 0

    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvPostContent: TextView = itemView.findViewById(R.id.tvPostContent)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.reviewRatingBar)
        private val ivUserProfile: ImageView = itemView.findViewById(R.id.ivUserProfile)
        private val ibLike: ImageButton = itemView.findViewById(R.id.ibLike)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)

        fun bind(review: Review) {
            tvUserName.text = "User ${review.userId.take(4)}"
            tvPostContent.text = review.comment

            val avgRating = review.ratings.values.average()
            ratingBar.rating = avgRating.toFloat()

            tvLikeCount.text = review.likes.toString()
            ibLike.setImageResource(
                if (review.isLiked) R.drawable.ic_like_filled
                else R.drawable.ic_like
            )
            ibLike.setOnClickListener {
                reviewActionListener?.onLikeReview(review)
            }

            Glide.with(itemView.context)
                .load(R.drawable.avatar)
                .into(ivUserProfile)
        }
    }

    class HeaderViewHolder(
        itemView: View,
        private val cafeId: String?,
        private val refreshCallback: () -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        val ivCafeHeader: ImageView = itemView.findViewById(R.id.ivCafeHeader)
        val tvCafeName: TextView = itemView.findViewById(R.id.tvCafeName)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        private val tvCafeAddress: TextView = itemView.findViewById(R.id.tvCafeAddress)
        private val tvOpenHours: TextView = itemView.findViewById(R.id.tvOpenHours)
        private val tvReviewCount: TextView = itemView.findViewById(R.id.tvReviewCount)
        private val tvRating: TextView = itemView.findViewById(R.id.tvRating)

        fun bind(cafeName: String?, cafeImageUrl: String?, averageRating: Float, address: String?) {
            tvCafeName.text = cafeName ?: "Cafe Name"

            // Load cafe image
            Glide.with(itemView.context)
                .load(cafeImageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(ivCafeHeader)

            // Set rating
            ratingBar.rating = averageRating

            // Set address
            tvCafeAddress.text = address ?: "123 Coffee Lane"

            tvOpenHours.text = "Open ⋅ Closes 9PM"
            tvReviewCount.text = "142 reviews"
            tvRating.text = " ⋅ ⭐ ${String.format("%.1f", averageRating)}"
        }

        init {
            val btnWriteReview: Button = itemView.findViewById(R.id.btnWriteReview)
            btnWriteReview.setOnClickListener {
                cafeId?.let { id ->
                    showWriteReviewDialog(itemView, id, refreshCallback)
                } ?: run {
                    Toast.makeText(itemView.context, "Cafe ID not available", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        private fun showWriteReviewDialog(contextView: View, cafeId: String, refreshCallback: () -> Unit) {
            val context = contextView.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_write_review, null)

            val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
            val etReview = dialogView.findViewById<EditText>(R.id.etReview)

            val dialog = android.app.AlertDialog.Builder(context)
                .setTitle("Write a Review")
                .setView(dialogView)
                .setPositiveButton("Submit") { _, _ ->
                    val rating = ratingBar.rating.toDouble()
                    val comment = etReview.text.toString()

                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton

                    val newReview = hashMapOf(
                        "userId" to userId,
                        "cafeId" to cafeId,
                        "text" to comment,
                        "ratings" to mapOf("overall" to rating),
                        "likes" to emptyList<String>(),
                        "timestamp" to FieldValue.serverTimestamp()
                    )

                    FirebaseFirestore.getInstance().collection("review_comments")
                        .add(newReview)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Review added successfully", Toast.LENGTH_SHORT).show()
                            refreshCallback()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to add review: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        }
    }
}