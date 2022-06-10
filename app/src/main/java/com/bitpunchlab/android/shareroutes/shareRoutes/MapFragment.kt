package com.bitpunchlab.android.shareroutes.shareRoutes

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bitpunchlab.android.shareroutes.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map : GoogleMap
    private lateinit var supportMapFragment: SupportMapFragment


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        supportMapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        return view

    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        Log.i("onMapReady", "map is ready")
        map = googleMap

        var marker = map.addMarker(
            MarkerOptions().position(
            LatLng(43.6532, 79.3832)
            ).title("Current location"))


    }


}