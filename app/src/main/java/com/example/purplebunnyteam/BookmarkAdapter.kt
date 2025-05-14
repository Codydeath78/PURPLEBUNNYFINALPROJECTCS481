package com.example.purplebunnyteam

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class BookmarkAdapter(
    private val clickListener: (Bookmark) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder>() {

    private val bookmarks = mutableListOf<Bookmark>()
    private var emptyStateView: TextView? = null

    fun updateEmptyState() {
        emptyStateView?.visibility = if (bookmarks.isEmpty()) View.VISIBLE else View.GONE
    }

    fun setEmptyStateView(view: TextView) {
        emptyStateView = view
        updateEmptyState()
    }

    fun submitList(newBookmarks: List<Bookmark>) {
        bookmarks.clear()
        bookmarks.addAll(newBookmarks)
        notifyDataSetChanged()
        updateEmptyState()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark, parent, false)
        return BookmarkViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val bookmark = bookmarks[position]

        holder.itemView.setOnClickListener {
            clickListener(bookmark)
        }

        holder.apply {
            tvCafeName.text = bookmark.name
            tvCafeAddress.text = bookmark.address
            ratingBar.rating = bookmark.rating

            Glide.with(itemView.context)
                .load(bookmark.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(ivCafeImage)

            ibRemoveBookmark.setOnClickListener {
                // Get the actual current position in case it changed
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    removeBookmarkFromFirestore(bookmark.id)

                    // Remove item from dummy data
                    bookmarks.removeAt(position)
                    notifyItemRemoved(position)
                    updateEmptyState()
                }
            }
        }
    }
    private fun removeBookmarkFromFirestore(bookmarkId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).update(
            "favorites", FieldValue.arrayRemove(db.document("cafes/$bookmarkId"))
        ).addOnFailureListener { e ->
            Log.e("BookmarkAdapter", "Error removing bookmark: ${e.message}")
        }
    }

    override fun getItemCount() = bookmarks.size

    inner class BookmarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCafeImage: ImageView = itemView.findViewById(R.id.ivCafeImage)
        val tvCafeName: TextView = itemView.findViewById(R.id.tvCafeName)
        val tvCafeAddress: TextView = itemView.findViewById(R.id.tvCafeAddress)
        val ratingBar: RatingBar = itemView.findViewById(R.id.bookmarkRatingBar)
        val ibRemoveBookmark: ImageButton = itemView.findViewById(R.id.ibRemoveBookmark)
    }
}