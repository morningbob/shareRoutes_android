package com.bitpunchlab.android.shareroutes.shareRoutes

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.shareroutes.BuildConfig.MAPS_API_KEY
import com.bitpunchlab.android.shareroutes.R
import com.bitpunchlab.android.shareroutes.directionService.DirectionsAPIService
import com.bitpunchlab.android.shareroutes.directionService.Network
import com.bitpunchlab.android.shareroutes.map.LocationInfoViewModel
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.TravelMode
import java.lang.Exception

private const val TAG = "ShowMapFragment"

class ShowMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var locationInfoViewModel: LocationInfoViewModel
    private lateinit var mapFragment: SupportMapFragment
    private var path = MutableLiveData<ArrayList<LatLng>>(ArrayList())

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

        path.observe(viewLifecycleOwner, Observer { thePath ->
            Log.i("path variable count", thePath.size.toString())
            //Log.i("path: ", thePath.toString())
            if (thePath.isNotEmpty()) {
                val opts = PolylineOptions().addAll(path.value!!).color(Color.BLUE).width(5F)
                map.addPolyline(opts)
            }
            map.moveCamera(CameraUpdateFactory.newLatLng(LatLng(52.757500, -108.286110)))
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

            // sample markers
            //map.addMarker(MarkerOptions().position())
            val originLocation = Location("")
            originLocation.latitude = 52.321945
            originLocation.longitude = -106.584167

            val destLocation = Location("")
            destLocation.latitude = 52.757500
            destLocation.longitude = -108.286110
            val distance = originLocation.distanceTo(destLocation)
            Log.i("onMapReady distance: ", distance.toString())
            //measureDistance()
            //getARouteGeneral()
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
    //private fun getARoute(originLocation: LatLng, destinationLocation: LatLng, waypoints: List<LatLng>) {
        //val geoContext = GeoApiContext
    private fun getARoute() {
        val responseString = Network.apiService.getRoute(apiKey = MAPS_API_KEY)
        Log.i("result from Directions API: ", responseString.toString())
    }

    private fun getARouteGeneral() {
        val geoContext = GeoApiContext.Builder()
            .apiKey(MAPS_API_KEY)
            .build()

        val directionRequest = DirectionsApi.newRequest(geoContext)
            .mode(TravelMode.WALKING)
            .origin(com.google.maps.model.LatLng(52.321945, -106.584167))
            .destination(com.google.maps.model.LatLng(52.757500, -108.286110))


        try {
            val directionResult = directionRequest.await()
            Log.i("get route", "result")

            if (directionResult.routes != null && directionResult.routes.isNotEmpty()) {
                Log.i("response: ", "result route count: ${directionResult.routes.size}")

                val route = directionResult.routes[0]
                Log.i("routes print", route.legs.toString())

                if (route.legs != null) {
                    for (leg in route.legs) {
                        Log.i("routes leg ", leg.toString())
                        if (leg.steps != null) {
                            for (step in leg.steps) {
                                Log.i("routes step", step.toString())
                                if (step.steps != null && step.steps.isNotEmpty()) {
                                    for (stepOfStep in step.steps) {
                                        val stepOfStepPoints = stepOfStep.polyline
                                        Log.i("routes step1", stepOfStep.toString())
                                        Log.i("response", "steps point")
                                        if (stepOfStepPoints != null) {
                                            val coordsInside : List<com.google.maps.model.LatLng> = stepOfStepPoints.decodePath()
                                            for (coord in coordsInside) {
                                                path.value!!.add(LatLng(coord.lat, coord.lng))
                                                //Log.i("adding points", path.value.toString())
                                            }
                                        }
                                    }
                                } else {
                                    val stepPoints = step.polyline
                                    //val point1 = stepOfStep.polyline
                                    Log.i("routes step", step.toString())
                                    Log.i("response", "steps point")
                                    if (stepPoints != null) {
                                        val coordsInside : List<com.google.maps.model.LatLng> = stepPoints.decodePath()
                                        for (coord in coordsInside) {
                                            //path.value!!.add(LatLng(coord.lat, coord.lng))
                                            //Log.i("adding points", path.value.toString())
                                            add(LatLng(coord.lat, coord.lng))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // there used to be 0 result exception
        } catch (e: Exception) {
            Log.i("response", "there is error getting directions")
            Log.i("error", e.printStackTrace().toString())
        }
    }

    private fun add(point: LatLng) {
        var list = mutableListOf<LatLng>()
        if (!path.value.isNullOrEmpty()) {
            list = path.value!!.toMutableList()
        }
        list.add(point)
        path.value = list as ArrayList<LatLng>
    }

    // use getPosition in markers to get the LatLng
    // use the LatLng to create Location object
    // use Location object's distanceTo method
    // I decided to set the maximum distance between 2 points to be 50000 km
    // it is because if the distance is too long, there are too many points in the
    // response that the app can't process.
    // Yet, for walking distance, a route to walk a dog, it should be too long too
    private fun measureDistance(origin: Location, destination: Location) {
        // get the first and last markers

    }
}
/*
val sydney = LatLng(-34.0, 151.0)
        map.isMyLocationEnabled = true
        map.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        map.moveCamera(CameraUpdateFactory.newLatLng(sydney))

 */