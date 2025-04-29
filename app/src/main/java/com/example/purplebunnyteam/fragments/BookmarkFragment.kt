package com.example.purplebunnyteam.fragments

import android.os.Bundle
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

        adapter = BookmarkAdapter().apply {
            setEmptyStateView(emptyState)
            submitList(getDummyBookmarks())
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun getDummyBookmarks(): List<Bookmark> = listOf(
        Bookmark(
            id = "1",
            name = "Coffee Haven",
            address = "123 Java Street",
            imageUrl = "https://example.com/coffee1.jpg",
            rating = 4.5f
        ),
        Bookmark(
            id = "2",
            name = "Espresso Corner",
            address = "456 Brew Lane",
            imageUrl = "https://example.com/coffee2.jpg",
            rating = 4.0f
        )
    )
}