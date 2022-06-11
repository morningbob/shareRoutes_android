package com.bitpunchlab.android.shareroutes.shareRoutes

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.shareroutes.R
import com.bitpunchlab.android.shareroutes.map.LocationInfoViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map : GoogleMap
    private lateinit var supportMapFragment: SupportMapFragment
    private lateinit var locationViewModel: LocationInfoViewModel
    private lateinit var fusedLocationProviderClient : FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        locationViewModel = ViewModelProvider(requireActivity())
            .get(LocationInfoViewModel::class.java)

        supportMapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        locationViewModel.lastKnownLocation.observe(viewLifecycleOwner, Observer { location ->
            location?.let {
                Log.i("last know locate observing", "got last known location.")

                showUserLocation()
            }
        })

        return view

    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        Log.i("onMapReady", "map is ready")
        map = googleMap
        // enable zoom function
        map.uiSettings.isZoomControlsEnabled = true

        findDeviceLocation()
        val myLocation = LatLng(43.6532, 79.3832)
/*
        var marker = map.addMarker(
            MarkerOptions().position(
            myLocation
            ).title("Current location"))

        map.moveCamera(CameraUpdateFactory.newLatLng(myLocation))

        val cameraPosition = CameraPosition.Builder()
            .target(myLocation)
            .zoom(18f)
            .bearing(90f)
            .tilt(30f)
            .build()

        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

 */
    }

    private fun showUserLocation() {

        if (locationViewModel.lastKnownLocation.value != null &&
            locationViewModel.lastKnownLocation.value != null) {
            var location = LatLng(locationViewModel.lastKnownLocation.value!!.latitude,
                locationViewModel.lastKnownLocation.value!!.longitude)
            var marker = map.addMarker(MarkerOptions().position(
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
            // temporary, add the current position as the first marker

            marker.let {
                locationViewModel.addToMarkerList(it!!)
            }

        } else {
            Log.i("show user location", "can't get location")
        }
    }

    @SuppressLint("MissingPermission")
    private fun findDeviceLocation() {
        Log.i("device location", "finding")
        val locationResult = fusedLocationProviderClient.lastLocation
        locationResult.addOnCompleteListener(requireActivity()) { task ->
            if (task.isSuccessful) {
                locationViewModel._lastKnownLocation.postValue(task.result)

            }
        }
    }

}