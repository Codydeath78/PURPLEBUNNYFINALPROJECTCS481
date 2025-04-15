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
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker


class CustomInfoWindowAdapter(
    private val inflater: LayoutInflater,
    private val markerMap: Map<String, Marker>
) : GoogleMap.InfoWindowAdapter {

    private val viewMap = mutableMapOf<String, View?>()
    private val imageCache = mutableMapOf<String, Drawable?>()



    override fun getInfoWindow(marker: Marker): View? {
        return null
    }


    override fun getInfoContents(marker: Marker): View? {
        val place = marker.tag as? PlaceDetails ?: return null
        val cachedView = viewMap[place.placeId]

        //This will return cached view if image already loaded.
        if (cachedView != null && imageCache[place.placeId] != null) {
            return cachedView
        }

        val view = inflater.inflate(R.layout.custom_info_window, null)
        val title = view.findViewById<TextView>(R.id.info_title)
        val address = view.findViewById<TextView>(R.id.info_address)
        val rating = view.findViewById<TextView>(R.id.info_rating)
        val image = view.findViewById<ImageView>(R.id.info_image)

        title.text = place.name
        address.text = "Address: ${place.address}"
        rating.text = "Rating: ${place.rating}"

        val context = view.context

        Glide.with(context)
            .load(place.photoUrl)
            .thumbnail(
                Glide.with(context)
                    .load(place.photoUrl)
                    .override(50, 50)
            )
            .override(300, 300)
            .placeholder(R.drawable.ic_placeholder)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    imageCache[place.placeId] = resource
                    image.setImageDrawable(resource)

                    //This will refresh the marker.
                    Handler(Looper.getMainLooper()).post {
                        marker.showInfoWindow()
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    image.setImageDrawable(placeholder)
                }
            })

        viewMap[place.placeId] = view
        return view
    }
    companion object { //This is a helpful companion that I didn't need since Glide had a function to preload...
        fun preloadImage(context: android.content.Context, place: PlaceDetails) {
            Glide.with(context)
                .load(place.photoUrl)
                .override(300, 300)
                .placeholder(R.drawable.ic_placeholder)
                .preload()
        }
    }
}
