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
import com.google.maps.DirectionsApiRequest
import com.google.maps.GeoApiContext
import com.google.maps.model.TravelMode
import java.lang.Exception

private const val TAG = "ShowMapFragment"
private const val MAX_DISTANCE = 5000
private const val PATH_LINE_WIDTH = 10F

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

        locationInfoViewModel.lastKnownLocation.observe(viewLifecycleOwner, Observer { location ->
            location?.let {
                Log.i(TAG, "got last known location.")

                showUserLocation()
            }
        })

        path.observe(viewLifecycleOwner, Observer { thePath ->
            Log.i("path variable count", thePath.size.toString())
        })

        locationInfoViewModel.startCreatingRoute.observe(viewLifecycleOwner, Observer { start ->
            if (start) {
                // set up onTap listener
                map.setOnMapClickListener { clickedLocation ->
                    Log.i("user tapped the map: ", "location $clickedLocation")
                    addMarkerAlert(clickedLocation)
                }
            }
        })

        locationInfoViewModel.readyToCreateRoute.observe(viewLifecycleOwner, Observer { ready ->
            if (ready) {
                getARoute()
                // clean the marker list, markers on the map, path after the route was created.

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
            // temporary, add the current position as the first marker
            var marker = MarkerOptions().position(location).title("origin")
            //addMarker(marker)
            locationInfoViewModel.addToMarkerList(marker)

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
            if (checkMaxDistance(clickedLocation)) {
                addMarker(position = clickedLocation, "new marker")
            } else {
                maxDistanceExceededAlert()
            }

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
    // can specific transportation method too

    // this method initiates a get request to Directions API
    // it retrieve the markers' lat lng and compose the origin and destination in the request
    // it also get all the points of the route from the result
    private fun getARoute() {
        // at first, we can just get the first and last markers from the view model
        val originMarker = locationInfoViewModel.markerList.value!!.first()
        val destinationMarker = locationInfoViewModel.markerList.value!!.last()

        var waypointMarkers : MutableList<MarkerOptions> = locationInfoViewModel.markerList.value!!.toMutableList()

        waypointMarkers.removeFirst()  // the origin marker
        waypointMarkers.removeLast()  // the destination marker

        val waypointLatLng = waypointMarkers.map { com.google.maps.model.LatLng(it.position.latitude,
            it.position.longitude) }

        Log.i("waypoint list", waypointLatLng.toString())
        Log.i("waypoint", waypointLatLng[0].toString())

        val geoContext = GeoApiContext.Builder()
            .apiKey(MAPS_API_KEY)
            .build()

        val directionRequest = DirectionsApi.newRequest(geoContext)
            .mode(TravelMode.WALKING)
            .origin(com.google.maps.model.LatLng(originMarker.position.latitude, originMarker.position.longitude))
            .destination(com.google.maps.model.LatLng(destinationMarker.position.latitude,
                destinationMarker.position.longitude))
            .waypoints(*waypointLatLng.toTypedArray())
            // here, we don't set optimize to true because we want the original route,
            // not faster route.

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
                                            //coordsInside.forEach {  coord ->
                                            for (coord in coordsInside) {
                                                Log.i("coordInside", "for each added once")
                                                add(LatLng(coord.lat, coord.lng))
                                                //path.value!!.add(LatLng(coord.lat, coord.lng))
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
                                        //coordsInside.forEachIndexed { index, coord ->
                                            add(LatLng(coord.lat, coord.lng))
                                            Log.i("coordInside", "for each added once")
                                        //if (index == (coordsInside.size - 1)) {
                                            // notify the end of parsing the result

                                        //}
                                        }

                                    }

                                }
                            }
                        }
                    }
                }
            }
            // assume this is the end of parsing the result
            Log.i("parsing result completed", "we construct path here")
            constructPath()
            // there used to be 0 result exception
            // so need to consider and handle when no direction result is found.
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

    // this method keeps checking the distance between the first marker and the coordinate
    // of the clicked place.
    // if the distance exceeds 50 km, there will be an alert to remind user, user can't add
    // the marker
    private fun checkMaxDistance(clickedLocation: LatLng) : Boolean {
        if (locationInfoViewModel.markerList.value!!.isNotEmpty()) {
            val originMarker = locationInfoViewModel.markerList.value!!.first()
            //val destinationMarker = locationInfoViewModel.markerList.value!!.last()

            // convert to location object to get the distance
            val originLocation = Location("")
            originLocation.latitude = originMarker.position.latitude
            originLocation.longitude = originMarker.position.longitude
            val destinationLocation = Location("")
            destinationLocation.latitude = clickedLocation.latitude
            destinationLocation.longitude = clickedLocation.longitude

            val distance = originLocation.distanceTo(destinationLocation)
            return distance < MAX_DISTANCE
        } else {
            // in the case that the marker list is empty, that means it is the first marker
            // we'll let the test pass
            return true
        }
    }

    private fun maxDistanceExceededAlert() {
        val exceededAlert = AlertDialog.Builder(requireContext())

        exceededAlert.setCancelable(false)
        exceededAlert.setTitle(getString(R.string.maximum_distance_exceed_alert_title))
        exceededAlert.setMessage(getString(R.string.maximum_distance_exceed_alert_desc))
        exceededAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
            // do nothing, wait for another event
        })

        exceededAlert.show()
    }

    private fun constructPath() {
        if (path.value!!.isNotEmpty()) {
            val opts = PolylineOptions().addAll(path.value!!).color(Color.BLUE).width(
                PATH_LINE_WIDTH)
            map.addPolyline(opts)
        }
        // we move the camera only after we got all the points of the polyline
        // and we should construct the line only once.
        val destinationMarker = locationInfoViewModel.markerList.value!!.last()
        map.moveCamera(CameraUpdateFactory.newLatLng(LatLng(destinationMarker.position.latitude,
            destinationMarker.position.longitude)))
    }

    private fun cleanMarkersInfo() {

    }
}
