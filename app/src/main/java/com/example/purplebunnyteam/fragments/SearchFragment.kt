package com.example.purplebunnyteam.fragments
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.purplebunnyteam.CustomInfoWindowAdapter
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
import com.google.android.gms.maps.model.MapStyleOptions


class SearchFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val API_KEY = "AIzaSyBl7Ue74Ln14TV2ltZeYmuvs8Gc0S3cth8"
    private val markerMap = mutableMapOf<String, Marker>() //stores Marker references instead of LatLg so we can show info
    private val placeToMarkerMap = mutableMapOf<String, Marker>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        val searchView = view.findViewById<SearchView>(R.id.search_bar)
        // Get the SupportMapFragment and request the map to load
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return view
    }

    // Update the map configuration at runtime.
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        // Set the map coordinates to CSUSM
        val CSUSM = LatLng(33.1284, -117.1592)
        // Set the map type to Hybrid.
         // googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        // Add a marker on the map coordinates.
        //set custom info window
        mMap.setInfoWindowAdapter(CustomInfoWindowAdapter(layoutInflater, placeToMarkerMap))
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
            placeId = "Null"
        )
        //markerMap["CSUSM"] = CSUSM

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
                val success = mMap.setMapStyle( //THIS DOESN'T EVEN WORK!!!!!!!!!!!!!!!!
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
        mMap.setInfoWindowAdapter(CustomInfoWindowAdapter(layoutInflater, placeToMarkerMap))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 10f))
        //This will display traffic.
        mMap.isTrafficEnabled = true
        mMap.isBuildingsEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true //This will enable zoom controls.
        mMap.uiSettings.isCompassEnabled = true      //This will enable compass.
        mMap.uiSettings.isMyLocationButtonEnabled = true  //This will enable current location button.
        mMap.uiSettings.isMapToolbarEnabled = true   //This will enable map toolbar.

        //This will fetch nearby coffee shops.
        getNearbyPlaces(CSUSM, radiusKm * 1000, openNow)
        getNearbyPlaces(selectedLocation, radiusKm * 1000, openNow)
        setupSearchBar()
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

    private fun searchForMarker(query: String) { //This searches for marker and moves camera if found.
        val result = markerMap.entries.find { it.key.contains(query, ignoreCase = true)}

        if (result != null) {
            val marker = result.value
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 18f))
            marker.showInfoWindow() // Open the InfoWindow
            Log.d("SEARCH", "Navigating to: ${result.key}")
        } else {
            Log.d("SEARCH", "No match found for: $query")
        }
    }

    private fun getNearbyPlaces(location: LatLng, radiusMeters: Int, openNow: Boolean) {
        val baseUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/"
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(PlacesApiService::class.java)

        val call = service.getNearbyPlaces(
            location = "${location.latitude},${location.longitude}",
            radius = radiusMeters,
            type = "cafe",
            apiKey = API_KEY,
            openNow = if (openNow) "true" else null // only send param if true
        )
        ///////////////////////////////////////

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

                        //This will preload image using Glide.
                        Glide.with(requireContext())
                            .load(photoUrl)
                            .preload()

                        val placeDetails = PlaceDetails(
                            placeId = placeId,
                            photoUrl = photoUrl,
                            name = place.name ?: "No name",
                            address = place.vicinity ?: "No address available",
                            rating = place.rating ?: 0.0
                        )

                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(place.name)
                                .icon(resizeMapIcon(R.drawable.coffee_cup, 100, 100))
                        )

                        marker?.tag = placeDetails

                        marker?.let {
                            markerMap[place.name ?: "Unknown"] = it
                            placeToMarkerMap[placeId] = it
                        }
                    }



                }
            }

            override fun onFailure(call: Call<PlacesResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

}