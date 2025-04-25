package com.example.purplebunnyteam.fragments

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.purplebunnyteam.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.core.content.edit
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.widget.AutocompleteActivity

class LocationFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedLocation: LatLng? = null
    private lateinit var sharedPrefs: android.content.SharedPreferences
    private lateinit var selectedLocationText: TextView

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                getCurrentLocation()
            } else {
                Toast.makeText(requireContext(), R.string.permission_denied, Toast.LENGTH_SHORT).show()
            }
        }

    private val autocompleteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    val data = result.data
                    if (data != null) {
                        val place = Autocomplete.getPlaceFromIntent(data)
                        val latLng = place.latLng
                        val name = place.name

                        if (latLng != null) {
                            selectedLocation = latLng
                            selectedLocationText.text = name ?: getString(R.string.location_selected)
                            saveLocation(latLng.latitude, latLng.longitude, name)
                        } else {
                            Toast.makeText(requireContext(), R.string.unable_to_get_location, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                AutocompleteActivity.RESULT_ERROR -> {
                    val status = Autocomplete.getStatusFromIntent(result.data!!)
                    Toast.makeText(requireContext(), "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
                }

                Activity.RESULT_CANCELED -> {
                    // User canceled without selecting
                    Toast.makeText(requireContext(), R.string.location_cancelled, Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_location, container, false)
        sharedPrefs = requireContext().getSharedPreferences("UserPreferences", 0)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_key))
        }

        selectedLocationText = view.findViewById(R.id.selected_location_text)

        val btnSetLocation: Button = view.findViewById(R.id.btn_select_location)
        val switchUseCurrent: SwitchMaterial = view.findViewById(R.id.switch_use_current)
        val btnRefreshLocation: Button = view.findViewById(R.id.btn_refresh_location)
        val switchDisableLocation: SwitchMaterial = view.findViewById(R.id.switch_disable_location)

        val radiusSpinner: Spinner = view.findViewById(R.id.spinner_radius)
        val mapStyleSpinner: Spinner = view.findViewById(R.id.spinner_map_style)
        val switchOpenNow: SwitchMaterial = view.findViewById(R.id.switch_open_now)

        val shopWifi: CheckBox = view.findViewById(R.id.chk_wifi)
        val shopChains: CheckBox = view.findViewById(R.id.chk_chains)
        val shopIndependent: CheckBox = view.findViewById(R.id.chk_independent)

        radiusSpinner.adapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.radius_options, android.R.layout.simple_spinner_dropdown_item
        )

        mapStyleSpinner.adapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.map_style_options, android.R.layout.simple_spinner_dropdown_item
        )

        btnSetLocation.setOnClickListener {
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(requireContext())
            autocompleteLauncher.launch(intent)
        }

        switchUseCurrent.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestLocationPermission()
            }
        }

        btnRefreshLocation.setOnClickListener {
            if (switchUseCurrent.isChecked) {
                requestLocationPermission()
            }
        }

        switchDisableLocation.setOnCheckedChangeListener { _, isChecked ->
            btnSetLocation.isEnabled = !isChecked
            switchUseCurrent.isEnabled = !isChecked
            btnRefreshLocation.isEnabled = !isChecked

            sharedPrefs.edit {
                putBoolean("location_disabled", isChecked)
                if (isChecked) {
                    remove("lat")
                    remove("lng")
                    remove("location_name")
                    selectedLocation = null
                    selectedLocationText.text = getString(R.string.no_location_selected)
                    Toast.makeText(requireContext(), R.string.location_disabled, Toast.LENGTH_SHORT).show()
                }
            }

            //if (isChecked) {
                //sharedPrefs.edit { putBoolean("location_disabled", true) }
                //Toast.makeText(requireContext(), R.string.location_disabled, Toast.LENGTH_SHORT).show()
            //} else {
                //sharedPrefs.edit { putBoolean("location_disabled", false) }
            //}



        }

        val savePrefs = {
            sharedPrefs.edit {
                putInt("search_radius", radiusSpinner.selectedItem.toString().replace(" km", "").toInt())
                putString("map_style", mapStyleSpinner.selectedItem.toString())
                putBoolean("open_now", switchOpenNow.isChecked)
                putBoolean("pref_wifi", shopWifi.isChecked)
                putBoolean("pref_chains", shopChains.isChecked)
                putBoolean("pref_independent", shopIndependent.isChecked)
            }
        }

        listOf(radiusSpinner, mapStyleSpinner, switchOpenNow, shopWifi, shopChains, shopIndependent).forEach {
            when (it) {
                is Spinner -> it.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = savePrefs()
                    override fun onNothingSelected(p: AdapterView<*>?) {}
                }
                is CompoundButton -> it.setOnCheckedChangeListener { _, _ -> savePrefs() }
            }
        }

        loadPreferences(
            radiusSpinner,
            mapStyleSpinner,
            switchOpenNow,
            shopWifi,
            shopChains,
            shopIndependent,
            switchDisableLocation
        )

        return view
    }

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(requireContext(), R.string.permission_needed, Toast.LENGTH_SHORT).show()
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getCurrentLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    selectedLocation = LatLng(location.latitude, location.longitude)
                    selectedLocationText.text = getString(R.string.using_current_location)
                    saveLocation(location.latitude, location.longitude, getString(R.string.using_current_location))
                } else {
                    Toast.makeText(requireContext(), R.string.unable_to_get_location, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: SecurityException) {
            Toast.makeText(requireContext(), R.string.permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveLocation(lat: Double, lng: Double, name: String? = null) {
        sharedPrefs.edit {
            putFloat("lat", lat.toFloat())
            putFloat("lng", lng.toFloat())
            if (name != null) {
                putString("location_name", name)
            }
        }
    }


    private fun loadPreferences(
        radiusSpinner: Spinner,
        mapStyleSpinner: Spinner,
        switchOpenNow: SwitchMaterial,
        shopWifi: CheckBox,
        shopChains: CheckBox,
        shopIndependent: CheckBox,
        switchDisableLocation: SwitchMaterial
    ) {
        val radius = sharedPrefs.getInt("search_radius", 5)
        val mapStyle = sharedPrefs.getString("map_style", getString(R.string.default_map_style))
        val openNow = sharedPrefs.getBoolean("open_now", false)
        val wifi = sharedPrefs.getBoolean("pref_wifi", false)
        val chains = sharedPrefs.getBoolean("pref_chains", false)
        val independent = sharedPrefs.getBoolean("pref_independent", false)
        val locationDisabled = sharedPrefs.getBoolean("location_disabled", false)

        switchOpenNow.isChecked = openNow
        shopWifi.isChecked = wifi
        shopChains.isChecked = chains
        shopIndependent.isChecked = independent
        switchDisableLocation.isChecked = locationDisabled

        val radiusOptions = resources.getStringArray(R.array.radius_options)
        val radiusIndex = radiusOptions.indexOfFirst { it.startsWith("$radius") }
        if (radiusIndex >= 0) radiusSpinner.setSelection(radiusIndex)

        val mapStyleOptions = resources.getStringArray(R.array.map_style_options)
        val mapStyleIndex = mapStyleOptions.indexOf(mapStyle)
        if (mapStyleIndex >= 0) mapStyleSpinner.setSelection(mapStyleIndex)

        val lat = sharedPrefs.getFloat("lat", 0f).toDouble()
        val lng = sharedPrefs.getFloat("lng", 0f).toDouble()
        if (lat != 0.0 && lng != 0.0) {
            selectedLocation = LatLng(lat, lng)
            val locationName = sharedPrefs.getString("location_name", null)
            selectedLocationText.text = locationName ?: (getString(R.string.location_selected) + ": $lat, $lng")
        }
    }
}



