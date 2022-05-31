package com.bitpunchlab.android.shareroutes.shareRoutes

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.shareroutes.R
import com.bitpunchlab.android.shareroutes.directionService.DirectionsAPIService
import com.bitpunchlab.android.shareroutes.directionService.Network
import com.bitpunchlab.android.shareroutes.map.LocationInfoViewModel
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

private const val TAG = "ShowMapFragment"

class ShowMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var locationInfoViewModel: LocationInfoViewModel
    private lateinit var mapFragment: SupportMapFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_show_map, container, false)
        locationInfoViewModel = ViewModelProvider(requireActivity())
            .get(LocationInfoViewModel::class.java)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return view
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        Log.i("onMapReady", "map is ready")
        map = googleMap
        // enable zoom function
        map.uiSettings.isZoomControlsEnabled = true
        // set up onTap listener
        map.setOnMapClickListener { clickedLocation ->
            Log.i("user tapped the map: ", "location ${clickedLocation}")
            addMarkerAlert(clickedLocation)
        }

        locationInfoViewModel.lastKnownLocation.observe(viewLifecycleOwner, Observer { location ->
            location?.let {
                Log.i(TAG, "got last known location.")

                showUserLocation()
            }
        })

    }

    private fun showUserLocation() {

        if (locationInfoViewModel.lastKnownLocation.value != null &&
                locationInfoViewModel.lastKnownLocation.value != null) {
            var location = LatLng(locationInfoViewModel.lastKnownLocation.value!!.latitude,
                locationInfoViewModel.lastKnownLocation.value!!.longitude)
            map.addMarker(MarkerOptions().position(
                location).title("Current location"))
            map.moveCamera(CameraUpdateFactory.newLatLng(location))

            val cameraPosition = CameraPosition.Builder()
                .target(location)
                .zoom(18f)
                .bearing(90f)
                .tilt(30f)
                .build()

            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            Log.i("showed location", "location: lat ${location.latitude} long ${location.longitude}")
        } else {
            Log.i(TAG, "can't get location")
        }
    }

    private fun addMarker(position: LatLng, title: String) {
        val bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        var marker = MarkerOptions().position(position).title(title).icon(bitmapDescriptor)
        map.addMarker(marker)
        // we save the marker in the view model
        locationInfoViewModel.addToMarkerList(marker)
    }

    private fun addMarkerAlert(clickedLocation: LatLng) {
        val markerAlert = AlertDialog.Builder(requireContext())

        markerAlert.setCancelable(false)
        markerAlert.setTitle(getString(R.string.add_marker_alert_title))
        markerAlert.setMessage(getString(R.string.add_marker_alert_desc))
        markerAlert.setPositiveButton(getString(R.string.add_button),
            DialogInterface.OnClickListener() { dialog, button ->
            addMarker(position = clickedLocation, "new marker")

        })
        markerAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener() { dialog, button ->
                // do nothing and wait for another event
            })
        markerAlert.show()
    }

    // this method get the suggested routes from Directions API
    // I need another API key for the web request
    // the url can contains origin, destination, waypoints and API key
    // waypoints: we can submit some points between the origin and destination,
    //      to guide to generate the nearest route
    // can specific transportion method too
    private fun getARoute(originLocation: LatLng, destinationLocation: LatLng, waypoints: List<LatLng>) {
        //val geoContext = GeoApiContext
        val directionsAPIService = Network.apiService
    }
}
/*
val sydney = LatLng(-34.0, 151.0)
        map.isMyLocationEnabled = true
        map.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        map.moveCamera(CameraUpdateFactory.newLatLng(sydney))

 */