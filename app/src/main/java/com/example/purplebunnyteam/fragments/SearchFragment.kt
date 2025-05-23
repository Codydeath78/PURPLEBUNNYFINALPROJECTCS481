package com.example.purplebunnyteam.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.purplebunnyteam.PlaceDetails
import com.example.purplebunnyteam.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.view.animation.BounceInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.purplebunnyteam.ChatFragment
import com.google.android.gms.maps.model.MapStyleOptions
import com.example.purplebunnyteam.InfoWindowButtonClickListener
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.graphics.Color
import android.widget.Toast
import com.example.purplebunnyteam.NotificationUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue


class SearchFragment : Fragment(), OnMapReadyCallback, InfoWindowButtonClickListener {

    private lateinit var mMap: GoogleMap
    private val API_KEY = "AIzaSyB-gQe2ZezzKjtyMZirdWv9G1oyqc0Kggc"
    private val markerMap = mutableMapOf<String, Marker>() //This will store Marker references instead of LatLg so we can show info.
    private val placeToMarkerMap = mutableMapOf<String, Marker>()
    private var currentlySelectedMarker: Marker? = null
    private var isSearchTriggered = false
    private var currentBottomSheetDialog: BottomSheetDialog? = null
    private var isBouncing = false
    private var currentBouncingMarker: Marker? = null
    private val cachedPlaces = mutableListOf<Pair<PlaceDetails, LatLng>>() // Stores place details + coordinates

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // This will inflate the layout for this fragment.
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        //val searchView = view.findViewById<SearchView>(R.id.search_bar)

        //This will get the SupportMapFragment and request the map to load.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return view
    }




    override fun onBookmarkClicked(place: PlaceDetails) {
        currentlySelectedMarker?.let { stopBouncingMarker(it) }
        currentlySelectedMarker = null

        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        if (userId != null) {
            val userRef = db.collection("users").document(userId)
            val cafeRef = db.collection("cafes").document(place.placeId)

            // Check if cafe exists, create if not
            cafeRef.get().addOnSuccessListener { cafeDoc ->
                if (!cafeDoc.exists()) {
                    val newCafe = hashMapOf(
                        "id" to place.placeId,
                        "name" to place.name,
                        "address" to place.address,
                        "imageUrl" to place.photoUrl,
                        "rating" to place.rating.toFloat(),
                        "lat" to place.lat,
                        "lng" to place.lng,
                        "createdAt" to Timestamp.now(),
                        "addedByUser" to false
                    )
                    cafeRef.set(newCafe)
                }

                // Add reference to user's favorites
                userRef.update("favorites", FieldValue.arrayUnion(cafeRef))
                    .addOnSuccessListener {
                        //Toast.makeText(context, "Bookmarked ${place.name}", Toast.LENGTH_SHORT).show()
                        NotificationUtils.showToast(requireContext(), "Bookmarked ${place.name}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("BookmarkError", "Error: ${e.message}")
                    }
            }
        }
        else {
            //Toast.makeText(context, "Please login first in order to bookmark!", Toast.LENGTH_SHORT).show()
            NotificationUtils.showToast(requireContext(), "Please login first in order to bookmark!")
            return
        }
    }


    override fun onChatClicked(place: PlaceDetails) {
        currentlySelectedMarker?.let { stopBouncingMarker(it) }
        currentlySelectedMarker = null

        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val fragment = ChatFragment().apply {
                arguments = Bundle().apply {
                    putString("cafeId", place.placeId)
                    putString("cafeName", place.name)
                    putString("cafeImageUrl", place.photoUrl)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
        else {
            //Toast.makeText(context, "Please login first to access reviews!", Toast.LENGTH_SHORT).show()
            NotificationUtils.showToast(requireContext(), "Please login first to access reviews!")
            return
        }
    }

    //This will update the map configuration at runtime.
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        //This will set the map coordinates to CSUSM.
        val CSUSM = LatLng(33.1284, -117.1592)
        val csusmMarker = mMap.addMarker(
            MarkerOptions()
                .position(CSUSM)
                .title("CSUSM")
        )

        csusmMarker?.tag = PlaceDetails(
            name = "CSUSM",
            address = "333 S Twin Oaks Valley Rd, San Marcos, CA 92096",
            rating = 5.0,
            photoUrl = "https://www.csusm.edu/facultyopportunities/images/campus_drone.png",
            placeId = "Null",
            lng = 33.1284,
            lat = -117.1592
        )

        val sharedPrefs = requireContext().getSharedPreferences("UserPreferences", 0)
        val locationDisabled = sharedPrefs.getBoolean("location_disabled", false)

        val selectedLat = sharedPrefs.getFloat("lat", 0f).toDouble()
        val selectedLng = sharedPrefs.getFloat("lng", 0f).toDouble()
        val radiusKm = sharedPrefs.getInt("search_radius", 5)
        val openNow = sharedPrefs.getBoolean("open_now", false)
        val mapStylePref = sharedPrefs.getString("map_style", "Normal")

        //This will determine base location.
        val defaultLocation = LatLng(33.1284, -117.1592) // CSUSM
        val selectedLocation = if (!locationDisabled && selectedLat != 0.0 && selectedLng != 0.0)
            LatLng(selectedLat, selectedLng)
        else defaultLocation

        if (isDarkMode) {
            try {
                val success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_dark)
                )
                if (!success) {
                    Log.e("MapStyle", "Style parsing failed.")
                }
            } catch (e: Resources.NotFoundException) {
                Log.e("MapStyle", "Can't find style. Error: ", e)
            }
        } else {
            //This will apply user preference-based map style.
            when (mapStylePref) {
                "Hybrid" -> mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
                "Satellite" -> mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                "Terrain" -> mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                else -> mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            }
        }
        //This will move the camera to the map coordinates and zoom in closer.
        //mMap.setInfoWindowAdapter(CustomInfoWindowAdapter(layoutInflater, placeToMarkerMap, this))
        mMap.setOnMarkerClickListener { marker ->
            // If this was triggered by a search and it's the same marker, skip showing again
            if (isSearchTriggered && marker == currentlySelectedMarker) {
                isSearchTriggered = false // reset flag
                return@setOnMarkerClickListener true // consume the event
            }

            currentlySelectedMarker = marker
            val place = marker.tag as? PlaceDetails
            place?.let {
                showPlaceBottomSheet(it)
            }
            true
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 10f))
        //This will display traffic.
        mMap.isTrafficEnabled = true
        mMap.isBuildingsEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true //This will enable zoom controls.
        mMap.uiSettings.isCompassEnabled = true      //This will enable compass.
        mMap.uiSettings.isMyLocationButtonEnabled = true  //This will enable current location button.
        mMap.uiSettings.isMapToolbarEnabled = true   //This will enable map toolbar.

        //This will fetch nearby coffee shops.
        //getNearbyPlaces(CSUSM, radiusKm * 1000, openNow)

        ////////////////////////
        if (cachedPlaces.isNotEmpty()) {
            Log.d("MARKER_RESTORE", "Restoring markers from cache: ${cachedPlaces.size}")
            for ((place, latLng) in cachedPlaces) {
                val marker = mMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(place.name)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)) // Optional style
                )

                marker?.tag = place
                if (marker != null) {
                    markerMap[place.name] = marker
                    placeToMarkerMap[place.placeId] = marker
                }
            }
        } else {
            getNearbyPlaces(selectedLocation, radiusKm * 1000, openNow)
            setupSearchBar()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::mMap.isInitialized) {
            return // This will avoid crashing if map is not yet ready
        }
        val sharedPrefs = requireContext().getSharedPreferences("UserPreferences", 0)
        val selectedLat = sharedPrefs.getFloat("lat", 0f).toDouble()
        val selectedLng = sharedPrefs.getFloat("lng", 0f).toDouble()
        val openNow = sharedPrefs.getBoolean("open_now", false)
        val radiusKm = sharedPrefs.getInt("search_radius", 5)

        val selectedLocation = if (selectedLat != 0.0 && selectedLng != 0.0) {
            LatLng(selectedLat, selectedLng)
        } else {
            LatLng(33.1284, -117.1592) // Default to CSUSM
        }

        mMap.clear()
        cachedPlaces.clear()
        markerMap.clear()
        placeToMarkerMap.clear()
        getNearbyPlaces(selectedLocation, radiusKm * 1000, openNow)
    }

    private fun setupSearchBar() {
        val searchView = requireView().findViewById<SearchView>(R.id.search_bar)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchForMarker(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun showPlaceBottomSheet(place: PlaceDetails) {
        currentBottomSheetDialog?.dismiss() // Dismiss existing one

        val dialog = BottomSheetDialog(requireContext())
        currentBottomSheetDialog = dialog // Save reference
        val view = layoutInflater.inflate(R.layout.bottom_sheet_info, null)

        val title = view.findViewById<TextView>(R.id.info_title)
        val address = view.findViewById<TextView>(R.id.info_address)
        val rating = view.findViewById<TextView>(R.id.info_rating)
        val image = view.findViewById<ImageView>(R.id.info_image)
        val bookmarkBtn = view.findViewById<Button>(R.id.bookmark_btn)
        val chatBtn = view.findViewById<Button>(R.id.chat_btn)


        //This will detect dark mode and set text color
        val isDarkTheme = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDarkTheme) Color.WHITE else Color.BLACK
        title.setTextColor(textColor)
        address.setTextColor(textColor)
        rating.setTextColor(textColor)

        title.text = place.name
        address.text = "Address: ${place.address}"
        rating.text = "Rating: ${place.rating}"

        Glide.with(requireContext())
            .load(place.photoUrl)
            .placeholder(R.drawable.ic_placeholder)
            .into(image)




        bookmarkBtn.setOnClickListener {
            dialog.dismiss()
            onBookmarkClicked(place)
        }

        chatBtn.setOnClickListener {
            dialog.dismiss()
            onChatClicked(place)
        }

        //This will stop bouncing the previous selected marker if any
        currentlySelectedMarker?.let {
            stopBouncingMarker(it)
        }
        //This will get the marker associated with this place
        currentlySelectedMarker = placeToMarkerMap[place.placeId]
        Log.d("BOUNCE", "Trying to bounce marker for placeId: ${place.placeId}")
        if (currentlySelectedMarker == null) {
            Log.w("BOUNCE", "No marker found for this placeId. Skipping bounce.")
        }
        currentlySelectedMarker?.let { startBouncingMarker(it) }

        dialog.setContentView(view)

        dialog.setOnDismissListener {
            currentlySelectedMarker?.let { stopBouncingMarker(it) }
        }
        dialog.show()
    }

    private fun startBouncingMarker(marker: Marker) {
        if (marker == currentBouncingMarker) {
            Log.d("BOUNCE", "Bounce already active for marker: ${marker.title}")
            return // prevent double trigger
        }

        stopBouncingMarker(currentBouncingMarker) // stop previous one if any

        currentBouncingMarker = marker
        isBouncing = true
        val handler = Handler(Looper.getMainLooper())
        val duration = 1200L
        val interpolator = BounceInterpolator()

        Log.d("BOUNCE", "Starting bounce animation for marker: ${marker.title}")

        fun animateBounce() {
            val start = System.currentTimeMillis()

            handler.post(object : Runnable {
                override fun run() {
                    if (!isBouncing || marker != currentBouncingMarker) {
                        Log.d("BOUNCE", "Bounce cancelled or switched.")
                        marker.setAnchor(0.5f, 1f)
                        return
                    }

                    val elapsed = System.currentTimeMillis() - start
                    val t = (elapsed.toFloat() / duration).coerceAtMost(1f)
                    val bounce = 1 - interpolator.getInterpolation(t)

                    //marker.setAnchor(0.5f, 1f + 0.9f * bounce)
                    try {
                        marker.setAnchor(0.5f, 1f + 0.9f * bounce)
                    } catch (e: Exception) {
                        Log.e("BOUNCE", "Error setting anchor: ${e.message}")
                    }

                    if (t < 1f) {
                        handler.postDelayed(this, 16)
                    } else {
                        handler.postDelayed({ animateBounce() }, 200) //This does small pause between bounces
                    }
                }
            })
        }

        animateBounce()
    }

    private fun stopBouncingMarker(marker: Marker?) {
        //isBouncing = false
        //marker?.setAnchor(0.5f, 1f) // Reset bounce anchor
        if (marker == null || marker != currentBouncingMarker) return

        Log.d("BOUNCE", "Stopping bounce for marker: ${marker.title}")
        isBouncing = false
        try {
            marker.setAnchor(0.5f, 1f)
        } catch (e: Exception) {
            Log.e("BOUNCE", "Error resetting anchor: ${e.message}")
        }
        currentBouncingMarker = null
    }

    private fun addRippleEffect(latLng: LatLng) {

        val nightModeFlags = requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        val rippleResource = when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> R.drawable.white_ripple_circle // for dark mode
            else -> R.drawable.black_ripple_circle // for light mode
        }


        val circle = mMap.addGroundOverlay(
            GroundOverlayOptions()
                .position(latLng, 100f) // Size in meters
                .transparency(0.5f)
                .image(BitmapDescriptorFactory.fromResource(rippleResource))
                .zIndex(1f)
        )


        val handler = Handler(Looper.getMainLooper())
        val start = System.currentTimeMillis()
        val duration = 1200L // in ms

        handler.post(object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - start
                val progress = elapsed.toFloat() / duration

                if (progress > 1f) {
                    circle?.remove()
                    return
                }

                //This will expand and fade out
                val size = 100 + 200 * progress
                circle?.setDimensions(size.toFloat())
                circle?.transparency = 0.5f + 0.5f * progress

                handler.postDelayed(this, 16)
            }
        })
    }

    private fun searchForMarker(query: String) { //This searches for marker and moves camera if found.
        val result = markerMap.entries.find { it.key.contains(query, ignoreCase = true)}
        if (result != null) {
            val marker = result.value

            // Stop previous bounce
            //currentlySelectedMarker?.let { stopBouncingMarker(it) }

            //currentlySelectedMarker = marker
            //isSearchTriggered = true

            //startBouncingMarker(marker)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 18f))

            //startBouncingMarker(marker)
            addRippleEffect(marker.position)


            //marker.showInfoWindow() // Open the InfoWindow
            //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 18f))
            val placeDetails = marker.tag as? PlaceDetails
            placeDetails?.let {
                showPlaceBottomSheet(it)
            }

            Log.d("SEARCH", "Navigating to: ${result.key}")
        } else {
            Log.d("SEARCH", "No match found for: $query")
        }
    }

    private fun getNearbyPlaces(location: LatLng, radiusMeters: Int, openNow: Boolean) {
        /////////////////////////////////////////////////////////////////////////////
        val sharedPrefs = requireContext().getSharedPreferences("UserPreferences", 0)
        val minPrice = sharedPrefs.getInt("min_price_index", 0).takeIf { it in 1..5 }?.minus(1)
        val maxPrice = sharedPrefs.getInt("max_price_index", 0).takeIf { it in 1..5 }?.minus(1)
        //val minPrice = sharedPrefs.getInt("min_price_index", -1)
        //val maxPrice = sharedPrefs.getInt("max_price_index", -1)
        ////////////////////////////////////////////////////////////////////////////

        val baseUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/"
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(PlacesApiService::class.java)

        //This makes API call
        val call = service.getNearbyPlaces(
            location = "${location.latitude},${location.longitude}",
            radius = radiusMeters,
            type = "cafe",
            apiKey = API_KEY,
            openNow = if (openNow) "true" else null, //This will only send param if true.
            minPrice = minPrice,
            maxPrice = maxPrice
        )

        fun resizeMapIcon(iconResId: Int, width: Int, height: Int): BitmapDescriptor {
            val bitmap = BitmapFactory.decodeResource(resources, iconResId)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
            return BitmapDescriptorFactory.fromBitmap(resizedBitmap)
        }

        fun buildPhotoUrl(photoReference: String, apiKey: String): String {
            return "https://maps.googleapis.com/maps/api/place/photo" +
                    "?maxwidth=400" +
                    "&photoreference=$photoReference" +
                    "&key=$apiKey"
        }

        call.enqueue(object : Callback<PlacesResponse> {
            override fun onResponse(call: Call<PlacesResponse>, response: Response<PlacesResponse>) {
                if (response.isSuccessful) {
                    response.body()?.results?.forEach { place ->
                        val latLng = LatLng(place.geometry.location.lat, place.geometry.location.lng)
                        val photoReference = place.photos?.getOrNull(0)?.photoReference
                        val photoUrl = photoReference?.let { buildPhotoUrl(it, API_KEY) } ?: ""

                        val placeId = place.place_id ?: return@forEach

                        //Firestore sync starting here
                        val db = FirebaseFirestore.getInstance()
                        val cafeRef = db.collection("cafes").document(placeId)

                        cafeRef.get().addOnSuccessListener { doc ->
                            if (!doc.exists()) {
                                val newCafe = hashMapOf(
                                    "id" to placeId,
                                    "name" to place.name,
                                    "address" to place.vicinity,
                                    "imageUrl" to photoUrl,
                                    "rating" to (place.rating?.toFloat() ?: 0.0f), // Convert Double to Float
                                    "lat" to place.geometry.location.lat,
                                    "lng" to place.geometry.location.lng,
                                    "createdAt" to Timestamp.now(),
                                    "addedByUser" to false
                                )
                                cafeRef.set(newCafe)
                            }
                        }
                        // Firestore sync ends here

                        //This will preload image using Glide.
                        Glide.with(requireContext())
                            .load(photoUrl)
                            .preload()


                        // Check if marker already exists
                        val existingMarker = placeToMarkerMap[placeId]
                        if (existingMarker == null) {
                            // No existing marker, create one
                            val marker = mMap.addMarker(
                                MarkerOptions()
                                    .position(latLng)
                                    .title(place.name)
                                    .icon(resizeMapIcon(R.drawable.coffee_cup, 100, 100))
                            )

                            marker?.let {
                                it.tag = PlaceDetails(
                                    name = place.name ?: "No name",
                                    address = place.vicinity ?: "No address available",
                                    rating = place.rating ?: 0.0,
                                    photoUrl = photoUrl,
                                    placeId = placeId,
                                    lng = 0.0,
                                    lat = 0.0
                                )

                                if (marker != null) {
                                    markerMap[place.name ?: "Unknown"] = it
                                    placeToMarkerMap[placeId] = it
                                }
                                //cachedPlaces.add(it, latLng)
                            }
                        } else {
                            // Marker already exists, optionally update position/title if needed
                            existingMarker.position = latLng
                            existingMarker.title = place.name
                        ///////////////old code below caused ghost coffee shop icon////////////////
                        }
                    }
                }
            }

            override fun onFailure(call: Call<PlacesResponse>, t: Throwable) {
                t.printStackTrace()
                Log.e("NearbyPlaces", "API call failed: ${t.localizedMessage}")
            }
        })
    }
}