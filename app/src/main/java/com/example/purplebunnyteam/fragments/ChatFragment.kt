package com.example.purplebunnyteam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ChatFragment : Fragment() {
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
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        chatAdapter = ReviewAdapter().apply {
            if (cafeId != null) {
                updateData(
                    reviews = fetchCafeSpecificReviews(cafeId!!),
                    showHeader = true,
                    cafeName = this@ChatFragment.cafeName,
                    cafeImageUrl = this@ChatFragment.cafeImageUrl
                )
            } else {
                updateData(fetchRecentReviews())
            }
        }

        chatRecyclerView.adapter = chatAdapter
    }


    private fun fetchCafeSpecificReviews(cafeId: String): List<Review> {
        return listOf(
            Review(
                comment = "Absolutely love the cappuccino here! Perfect balance of espresso and milk.",
                userId = "user123",
                cafeId = cafeId,
                ratings = mapOf("coffee" to 4.8, "service" to 4.5),
                likes = 42
            ),
            Review(
                comment = "Cozy atmosphere and friendly staff. The matcha latte is a must-try!",
                userId = "user456",
                cafeId = cafeId,
                ratings = mapOf("coffee" to 4.5, "ambiance" to 5.0),
                likes = 28
            ),
            Review(
                comment = "Great workspace with reliable WiFi. Flat white is my go-to drink.",
                userId = "remoteWorker99",
                cafeId = cafeId,
                ratings = mapOf("coffee" to 4.2, "wifi" to 4.8),
                likes = 35
            )
        )
    }

    private fun fetchRecentReviews(): List<Review> {
        return listOf(
            Review(
                comment = "Just discovered this hidden gem! Amazing cold brew.",
                userId = "coffeeLover22",
                cafeId = "cafe002",
                ratings = mapOf("coffee" to 4.7),
                likes = 15
            ),
            Review(
                comment = "Perfect spot for afternoon tea. Their scones are divine!",
                userId = "teaEnthusiast",
                cafeId = "cafe003",
                ratings = mapOf("tea" to 4.9),
                likes = 22
            )
        )
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