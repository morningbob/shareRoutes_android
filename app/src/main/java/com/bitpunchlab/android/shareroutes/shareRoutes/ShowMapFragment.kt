package com.bitpunchlab.android.shareroutes.shareRoutes

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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

private const val TAG = "ShowMapFragment"

class ShowMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var locationInfoViewModel: LocationInfoViewModel

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

        val mapFragment = parentFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        locationInfoViewModel.lastKnownLocation.observe(viewLifecycleOwner, Observer { location ->
            location?.let {
                Log.i(TAG, "got last known location.")
                showUserLocation()
            }
        })

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        map.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        map.moveCamera(CameraUpdateFactory.newLatLng(sydney))

    }

    private fun showUserLocation() {
        var lat = locationInfoViewModel.lastKnownLocation.value?.latitude
        var long = locationInfoViewModel.lastKnownLocation.value?.longitude

        if (lat != null && long != null) {
            var location = LatLng(lat, long)
            map.addMarker(MarkerOptions().position(location).title("Current location"))
            map.moveCamera(CameraUpdateFactory.newLatLng(location))
            Log.i(TAG, "showed location")
        } else {
            Log.i(TAG, "can't get location")
        }
    }
}