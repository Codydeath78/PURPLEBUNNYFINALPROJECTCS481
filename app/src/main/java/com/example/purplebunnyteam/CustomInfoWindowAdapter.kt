package com.example.purplebunnyteam

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.example.purplebunnyteam.fragments.PlaceResult
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class CustomInfoWindowAdapter(private val inflater: LayoutInflater) : GoogleMap.InfoWindowAdapter {

    override fun getInfoWindow(marker: Marker): View? {
        return null
    }

    @SuppressLint("SetTextI18n")
    override fun getInfoContents(marker: Marker): View? {
        val view = inflater.inflate(R.layout.custom_info_window, null)
        val place = marker.tag as? PlaceDetails
        val title = view.findViewById<TextView>(R.id.info_title)
        val address = view.findViewById<TextView>(R.id.info_address)
        val rating = view.findViewById<TextView>(R.id.info_rating)

        place?.let {
            title.text = it.name
            address.text = "Address: ${it.address}"
            rating.text = "Rating: ${it.rating ?: "No rating"}"
        }
        return view
    }
}