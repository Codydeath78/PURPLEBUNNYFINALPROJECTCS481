package com.example.purplebunnyteam

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class CustomInfoWindowAdapter(
    private val inflater: LayoutInflater,
    private val markerMap: Map<String, Marker>
) : GoogleMap.InfoWindowAdapter {

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
        val image = view.findViewById<ImageView>(R.id.info_image)

        place?.let {
            title.text = it.name
            address.text = "Address: ${it.address}"
            rating.text = "Rating: ${it.rating}"

            Glide.with(view.context)
                .load(it.photoUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        image.setImageDrawable(resource)

                        // Refresh InfoWindow for the correct marker using placeId
                        val markerToRefresh = markerMap[it.placeId]
                        markerToRefresh?.let { m ->
                            if (m.isInfoWindowShown) {
                                m.hideInfoWindow()
                                Handler(Looper.getMainLooper()).postDelayed({
                                    m.showInfoWindow()
                                }, 0)
                            }
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        image.setImageDrawable(placeholder)
                    }
                })
        }

        return view
    }
}
