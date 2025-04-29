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
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ReviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_REVIEW = 1
    }

    private var showHeader: Boolean = false
    private var cafeName: String? = null
    private var cafeImageUrl: String? = null
    private var reviews: List<Review> = emptyList()

    fun updateData(
        reviews: List<Review>,
        showHeader: Boolean = false,
        cafeName: String? = null,
        cafeImageUrl: String? = null
    ) {
        this.reviews = reviews
        this.showHeader = showHeader
        this.cafeName = cafeName
        this.cafeImageUrl = cafeImageUrl
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (showHeader && position == 0) TYPE_HEADER else TYPE_REVIEW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.header_cafe_details, parent, false)
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
                // Bind header data
                cafeName?.let { holder.tvCafeName.text = it }
                Glide.with(holder.itemView.context)
                    .load(cafeImageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(holder.ivCafeHeader)
            }
            is ReviewViewHolder -> {
                // Adjust position for reviews list
                val reviewPosition = if (showHeader) position - 1 else position
                holder.bind(reviews[reviewPosition])
            }
        }
    }

    override fun getItemCount() = reviews.size + if (showHeader) 1 else 0

    fun updateData(newReviews: List<Review>, showHeader: Boolean = false) {
        this.showHeader = showHeader
        this.reviews = newReviews
        notifyDataSetChanged()
    }

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
                review.isLiked = !review.isLiked
                review.likes += if (review.isLiked) 1 else -1
                notifyItemChanged(adapterPosition)
            }

            Glide.with(itemView.context)
                .load(R.drawable.avatar)
                .into(ivUserProfile)
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCafeHeader: ImageView = itemView.findViewById(R.id.ivCafeHeader)
        val tvCafeName: TextView = itemView.findViewById(R.id.tvCafeName)

        init {
            val btnWriteReview: Button = itemView.findViewById(R.id.btnWriteReview)
            btnWriteReview.setOnClickListener {
                showWriteReviewDialog(itemView)
            }
        }

        private fun showWriteReviewDialog(contextView: View) {
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

                    // Optional: Create new Review and pass it back or refresh the list
                    val newReview = Review(
                        reviewId = "new_${System.currentTimeMillis()}",
                        userId = "currentUser", // Replace with actual logged-in user ID
                        cafeId = "currentCafe", // Replace with actual cafe ID
                        ratings = mapOf("overall" to rating),
                        comment = comment
                    )
                    // You can emit this back via a callback interface or use ViewModel/LiveData

                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        }
    }
}