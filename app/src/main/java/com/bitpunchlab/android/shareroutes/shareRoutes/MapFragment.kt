package com.bitpunchlab.android.shareroutes.shareRoutes

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationRequest
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.shareroutes.BuildConfig
import com.bitpunchlab.android.shareroutes.BuildConfig.MAPS_API_KEY
import com.bitpunchlab.android.shareroutes.R
import com.bitpunchlab.android.shareroutes.ShareRouteState
import com.bitpunchlab.android.shareroutes.SuggestRoutesState
import com.bitpunchlab.android.shareroutes.map.LocationInfoViewModel
import com.bitpunchlab.android.shareroutes.models.Route
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.TravelMode
import java.lang.Exception
import java.util.*
import javax.net.ssl.SSLEngineResult
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

private const val TAG = "MapFragment"
private const val MAX_DISTANCE = 5000
private const val PATH_LINE_WIDTH = 10F
private const val MAX_NUMBER_MARKERS = 10

// this is the fragment that controls the supportMapFragment.
// all the map related operations happen here
// this include posting request to Directions API
// and placing markers in the map
// it also responsible for constructing the path of the route
// to calculate the distance between the marker and the suggested routes first marker,
// I convert the latlng to Location objects and use .distanceTo method
class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map : GoogleMap
    private lateinit var supportMapFragment: SupportMapFragment
    private lateinit var locationViewModel: LocationInfoViewModel
    private lateinit var fusedLocationProviderClient : FusedLocationProviderClient
    private var path = MutableLiveData<ArrayList<LatLng>>(ArrayList())
    private lateinit var autocompleteFragment: AutocompleteSupportFragment
    private lateinit var placeClient: PlacesClient
    private lateinit var geoCoder : Geocoder

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

        observeAppState()

        supportMapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupAutoCompleteFragment()

        locationViewModel.lastKnownLocation.observe(viewLifecycleOwner, Observer { location ->
            location?.let {
                Log.i("last know locate observing", "got last known location.")
                var newLocation : LatLng = LatLng(location.latitude, location.longitude)
                showUserLocation(newLocation)
            }
        })

        locationViewModel.chosenRoute.observe(viewLifecycleOwner, Observer { route ->
            route?.let {
                // draw the route
                locationViewModel.finishedNavigatingRoute()
                // clean previous route
                clearPath()
                val routeLatLngPoints = transformPointsMapToLatLngList(route.pointsMap)
                drawRoute(routeLatLngPoints)
                displayRouteLine(routeLatLngPoints[0])
                locationViewModel._suggestRoutesAppState.value = SuggestRoutesState.DISPLAY_CHOSEN
            }
        })

        locationViewModel.showMyLocation.observe(viewLifecycleOwner, Observer { show ->
            if (show) {
                findDeviceLocation()
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

    }

    private fun setupAutoCompleteFragment() {
        // need to initialize Place
        Places.initialize(requireContext(), MAPS_API_KEY)
        placeClient = Places.createClient(requireContext())
        // Initialize the AutocompleteSupportFragment.
        autocompleteFragment =
            parentFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i(TAG, "Place: ${place.name}, ${place.id}")
                // alert user
                navigateAlert(place)
                // navigate to the place
            }

            override fun onError(status: Status) {
                Log.i(TAG, "An error occurred: $status")
            }
        })
    }

    // when user clicks a marker, we show an alert if the user wants to remove
    // the marker.
    override fun onMarkerClick(marker: Marker): Boolean {
        Log.i("onMarkerClicked", "marker is clicked")
        removeMarkerAlert(marker)
        return true
    }

    private fun showUserLocation(location: LatLng) {

        if (location != null) {

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

            //marker.let {
            //    locationViewModel.addToMarkerList(it!!)
            //}
            // clear last known location here
            //locationViewModel._lastKnownLocation.value = null

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
                //Log.i("found location", task.result.latitude.toString())
            } else {
                Log.i("error in finding location", "true")
            }
        }
    }


    private fun addMarker(position: LatLng, title: String) {
        // check if there are enough markers
        if (locationViewModel.markerList.value!!.size < MAX_NUMBER_MARKERS) {
            val bitmapDescriptor =
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            var markerOptions = MarkerOptions().position(position).title(title).icon(bitmapDescriptor)
            var marker = map.addMarker(markerOptions)
            // we save the marker in the view model
            marker?.let {
                Log.i("add marker", "adding to market list")
                locationViewModel.addToMarkerList(it!!)
            }
        } else {
            // remind user that they can only place 10 markers
            maxNumMarkersAlert()
        }
    }

    private fun maxNumMarkersAlert() {
        val numMarkersAlert = AlertDialog.Builder(requireContext())

        numMarkersAlert.setCancelable(false)
        numMarkersAlert.setTitle(getString(R.string.max_markers_alert_title))
        numMarkersAlert.setMessage(getString(R.string.max_markers_alert_desc))
        numMarkersAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing, don't add the marker
            })
        numMarkersAlert.show()
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

    // use getPosition in markers to get the LatLng
    // use the LatLng to create Location object
    // use Location object's distanceTo method
    // I decided to set the maximum distance between 2 points to be 5000 km
    // it is because if the distance is too long, there are too many points in the
    // response that the app can't process.
    // Yet, for walking distance, a route to walk a dog, it should be too long too

    // this method keeps checking the distance between the first marker and the coordinate
    // of the clicked place.
    // if the distance exceeds 50 km, there will be an alert to remind user, user can't add
    // the marker
    private fun checkMaxDistance(clickedLocation: LatLng) : Boolean {
        if (locationViewModel.markerList.value!!.isNotEmpty()) {
            val originMarker = locationViewModel.markerList.value!!.first()
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

    private fun removeMarkerAlert(marker: Marker) {
        val removeAlert = AlertDialog.Builder(requireContext())

        removeAlert.setCancelable(false)
        removeAlert.setTitle(getString(R.string.remove_marker_alert_title))
        removeAlert.setMessage(getString(R.string.remove_marker_alert_desc))

        removeAlert.setPositiveButton(getString(R.string.confirm_button),
            DialogInterface.OnClickListener { dialog, button ->
                // remove the marker
                // remove from the map interface
                marker.remove()
                // remove from view model marker list
                removeMarkerFromViewModel(marker)
            })

        removeAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
            })
        removeAlert.show()
    }

    // to remove a marker, also need to remove it from view model marker list
    private fun removeMarkerFromViewModel(marker: Marker) {
        if (!locationViewModel.markerList.value.isNullOrEmpty()) {
            val markerToRemove : Marker? = locationViewModel.markerList.value!!.find { markerInList ->
                markerInList.id == marker.id
            }
            markerToRemove?.let {
                val markerList = locationViewModel.markerList.value!!.toMutableList()
                markerList.remove(it)
                locationViewModel._markerList.value = markerList
            }
        }
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
        val originMarker = locationViewModel.markerList.value!!.first()
        val destinationMarker = locationViewModel.markerList.value!!.last()

        var waypointMarkers : MutableList<Marker> = locationViewModel.markerList.value!!.toMutableList()

        waypointMarkers.removeFirst()  // the origin marker
        waypointMarkers.removeLast()  // the destination marker

        val waypointLatLng = waypointMarkers.map { com.google.maps.model.LatLng(it.position.latitude,
            it.position.longitude) }

        Log.i("waypoint list", waypointLatLng.toString())
        //Log.i("waypoint", waypointLatLng[0].toString())

        val geoContext = GeoApiContext.Builder()
            .apiKey(BuildConfig.MAPS_API_KEY)
            .build()

        val directionRequest = DirectionsApi.newRequest(geoContext)
            .mode(TravelMode.WALKING)
            .origin(com.google.maps.model.LatLng(originMarker.position.latitude, originMarker.position.longitude))
            .destination(com.google.maps.model.LatLng(destinationMarker.position.latitude,
                destinationMarker.position.longitude))
            .waypoints(*waypointLatLng.toTypedArray())
            .optimizeWaypoints(true)
        // here, we optimize to true because we want google to ignore the orders of the markers.

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
                                            coordsInside.forEach {  coord ->
                                                Log.i("coordInside", "for each added once")
                                                addPoint(LatLng(coord.lat, coord.lng))
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
                                        coordsInside.forEach { coord ->
                                            addPoint(LatLng(coord.lat, coord.lng))
                                            Log.i("coordInside", "for each added once")
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
            drawRoute(path.value!!)
            createRoute(path.value!!)
            displayRouteLine(path.value!!.last())
            // there used to be 0 result exception
            // so need to consider and handle when no direction result is found.
        } catch (e: Exception) {
            Log.i("response", "there is error getting directions")
            Log.i("error", e.printStackTrace().toString())
        }
    }

    private fun addPoint(point: LatLng) {
        var list = mutableListOf<LatLng>()
        if (!path.value.isNullOrEmpty()) {
            list = path.value!!.toMutableList()
        }
        list.add(point)
        path.value = list as ArrayList<LatLng>
    }

    private fun displayRouteLine(destination: LatLng) {
        // we move the camera only after we got all the points of the polyline
        // and we should construct the line only once.
        //val destinationMarker = locationViewModel.markerList.value!!.last()
        map.moveCamera(CameraUpdateFactory.newLatLng(LatLng(destination.latitude,
            destination.longitude)))
    }

    private fun drawRoute(points: List<LatLng>) {
        if (!points.isNullOrEmpty()) {
            val opts = PolylineOptions().addAll(points).color(Color.BLUE).width(
                PATH_LINE_WIDTH)
            locationViewModel._routeLine.value = map.addPolyline(opts)
        }
    }

    private fun createRoute(points: List<LatLng>) {
        locationViewModel._routeToShare.value = createRouteObject(points)
    }

    // transform list of lan lng to list of map with letter key and map of lan lng object
    private fun transformLatLngToMap(point: LatLng) : HashMap<String, Double> {
        var latlngMap = HashMap<String, Double>()
        latlngMap.put("lat", point.latitude)
        latlngMap.put("lng", point.longitude)
        return latlngMap
    }

    private fun transformPointToMap(point: LatLng, key: String) : HashMap<String, HashMap<String, Double>> {
        var pointMap = HashMap<String, HashMap<String, Double>>()
        pointMap.put(key, transformLatLngToMap(point))
        return pointMap
    }

    private fun transformPointsToMap(points: List<LatLng>) : HashMap<String, HashMap<String, HashMap<String, Double>>> {
        var pointsMap = HashMap<String, HashMap<String, HashMap<String, Double>>>()
        for (i in 0 until points.size) {
            // generate letter key
            var key = "${i}A"

            pointsMap.put(key, transformPointToMap(points[i], key))
        }
        return pointsMap
    }

    // we need to transform the hashmap route data back into a list of LatLng
    // notice we don't need to convert a whole route list, just a route
    // that we need to consider, when we get back the map from the database
    // the keys are not in order, so, we need to sort the keys before we do transformation
    // so, I first sort the key and put it into a list, then transform the list
    private fun transformPointsMapToLatLngList(pointsMap: HashMap<String, HashMap<String, HashMap<String, Double>>>) :
            List<LatLng> {
        val latLngList = mutableListOf<LatLng>()
        var sortedListOfHashmaps = mutableListOf<HashMap<String, Double>>()
        // remove the key, get the list of hashmaps of hashmaps of latlng
        val listOfHashmapOfHashmapOfLatLng = mutableListOf<HashMap<String, HashMap<String, Double>>>()
        for ((key, value) in pointsMap) {
            listOfHashmapOfHashmapOfLatLng.add(value)
        }

        // sort the LatLngMaps by creating a sorted list
        sortedListOfHashmaps = sortLatLngMapKeys(listOfHashmapOfHashmapOfLatLng).toMutableList()

        // now the list of LatLng maps are sorted
        sortedListOfHashmaps.forEach { map ->
            latLngList.add(transformLatLngMapToPoint(map))
        }
        return latLngList
    }

    private fun transformLatLngMapToPoint(latLngMap: HashMap<String, Double>) : LatLng {
        //Log.i("lat lng to point method", latLngMap.toString())
        return LatLng(latLngMap.get("lat")!!, latLngMap.get("lng")!!)
    }

    private fun sortLatLngMapKeys(listOflatLngMaps: List<HashMap<String, HashMap<String, Double>>>) :
            List<HashMap<String, Double>> {
        // cut last character and turn the rest into number
        // create a hashmap with the number we got as key
        // with the value as the lat lng hashmap
        val hashmap = HashMap<Int, HashMap<String, Double>>()
        val sortedListOfLatLngHashmap = mutableListOf<HashMap<String, Double>>()
        listOflatLngMaps.map { hashmapOfLatLngHashmap ->
            hashmapOfLatLngHashmap.keys.map { key ->
                // should have only 1 key in the whole map
                val numKey = key.substring(0, key.length - 1).toInt()
                hashmap.put(numKey, hashmapOfLatLngHashmap[key] as kotlin.collections.HashMap)
            }
        }

        val listOfKeys = hashmap.keys.sorted()

        listOfKeys.map { key ->
            //Log.i("sorting: key", key.toString())
            hashmap[key]?.let {
                sortedListOfLatLngHashmap.add(it)
                //Log.i("got a map for key", key.toString())
            }
        }

        return sortedListOfLatLngHashmap
    }

    private fun clearPath() {
        locationViewModel._routeLine.value?.remove()
        locationViewModel._routeLine.value = null
    }

    private fun cleanRouteInfo() {
        removeAllMarkers()
        locationViewModel._markerList.value = emptyList()
        path.value = ArrayList()
        // clear the markers on the map
        // clear the path on the map
        locationViewModel._routeLine.value?.remove()
        locationViewModel._routeLine.value = null
    }

    // loop through all markers to remove it
    private fun removeAllMarkers() {
        Log.i("remove all markers, list size", locationViewModel.markerList.value!!.size.toString())
        for (marker in locationViewModel.markerList.value!!) {
            Log.i("removing markers one by one", marker.id)
            marker.remove()
        }
        // reset marker list by clearing set the list to null
    }

    private fun navigateAlert(place: Place) {
        Log.i("navigate", "running alert")
        val navAlert = AlertDialog.Builder(requireContext())

        navAlert.setCancelable(false)
        navAlert.setTitle("Navigation")
        navAlert.setMessage("Do you want to navigate to ${place.name}?")

        navAlert.setPositiveButton("Navigate",
            DialogInterface.OnClickListener { dialog, button ->
                // clear route info and starts fresh
                cleanRouteInfo()
                // navigate to the place
                showUserLocation(place.latLng)
            })
        navAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing, wait for user's event
            })
        navAlert.show()
    }

    private fun pickLocationAlert() {
        val pickAlert = AlertDialog.Builder(requireContext())

        pickAlert.setCancelable(false)
        pickAlert.setTitle("Pick Location")
        pickAlert.setMessage("Please pick a location by tapping the map.")

        pickAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // wait for user to pick a location

            })
        pickAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing, wait for the next event
            })

        pickAlert.show()
    }

    private fun confirmLocationAlert(clickedLocation: LatLng) {
        val searchAlert = AlertDialog.Builder(requireContext())

        searchAlert.setCancelable(false)
        searchAlert.setTitle("Confirm Location")
        searchAlert.setMessage("Get suggested routes around this location, ${clickedLocation} ?")

        searchAlert.setPositiveButton(getString(R.string.confirm_button),
            DialogInterface.OnClickListener { dialog, button ->
                // clean routes info, so when user come back, starts fresh
                cleanRouteInfo()

                // if user choosed this location, then we cancel the alert to set place
                locationViewModel._clearSuggestRoutesListener.value = true
                //locationViewModel._shouldSuggestRoutes.value = false
                // run the suggest routes fragment and do the search
                // notice map page fragment to navigate to suggest routes fragment
                locationViewModel._runSuggestRoutesFragment.value = true
                locationViewModel.chosenSearchLocation.value = clickedLocation
                locationViewModel._suggestRoutesAppState.value = SuggestRoutesState.CONFIRMED_LOCATION
            })
        searchAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
            })
        searchAlert.show()
    }

    private fun getRouteInfo(point: LatLng) : Address {
        // the result comes back as a list of address objects.
        // we only need the first one, and I restrict the result to be 1 only too

        geoCoder = Geocoder(requireContext(), Locale.getDefault())
        var addressList: List<Address> = geoCoder.getFromLocation(point.latitude, point.longitude, 1)

        return addressList.first()
    }

    private fun createRouteObject(points: List<LatLng>) : Route {
        val origin = points.first()
        val pointsMap = transformPointsToMap(points)

        val addressObject = getRouteInfo(origin)
        val address = addressObject.getAddressLine(0)
        val city = addressObject.locality
        val state = addressObject.adminArea
        val country = addressObject.countryName
        var name = ""
        addressObject.featureName?.let { knownName ->
            name = knownName
        }

        val newRoute = Route(map = pointsMap, placeName = name, placeAddress = address,
        placeCity = city ?: "", placeState = state ?: "", placeCountry = country ?: "")
        return newRoute
    }

    private fun clearRouteAlert() {
        val clearAlert = AlertDialog.Builder(requireContext())

        clearAlert.setCancelable(false)
        clearAlert.setTitle(getString(R.string.clean_route_line_alert_title))
        clearAlert.setMessage(getString(R.string.clean_route_line_alert_desc))

        clearAlert.setPositiveButton(getString(R.string.clean_route_button),
            DialogInterface.OnClickListener { dialog, button ->
                // proceed to clean the route and create a new route
                locationViewModel.routeLine.value?.remove()
                locationViewModel.routeLine.value = null
                getARoute()
            })
        clearAlert.show()
    }

    private fun observeAppState() {
        locationViewModel.shareRouteAppState.observe(viewLifecycleOwner, Observer { appState ->
            when (appState) {
                ShareRouteState.NORMAL -> 0

                ShareRouteState.ADD_MARKER -> {
                    cleanRouteInfo()
                    // set up onTap listener
                    map.setOnMapClickListener { clickedLocation ->
                        Log.i("user tapped the map: ", "location $clickedLocation")
                        addMarkerAlert(clickedLocation)
                    }
                    map.setOnMarkerClickListener(this)
                }

                ShareRouteState.ROUTE_TO_BE_CREATED -> {
                    // after the route is created, keep track of if it is shared

                    2
                }

                ShareRouteState.CREATING_ROUTE -> {
                    // check if there is already a route line presented, if so, alert user and
                    // remove it.
                    if (locationViewModel.routeLine.value != null) {
                        clearRouteAlert()
                    } else {
                        getARoute()
                    }
                }

                ShareRouteState.CLEAN_ROUTE -> {
                    clearPath()
                }

                ShareRouteState.SAVE_ROUTE -> {

                }

                ShareRouteState.SHARED -> {
                    // don't let user share it again
                    cleanRouteInfo()

                }
                ShareRouteState.RESTART -> {
                    // clean all info and route line
                    cleanRouteInfo()
                }
            }
        })

        locationViewModel.suggestRoutesAppState.observe(viewLifecycleOwner, Observer { appState ->
            when (appState) {
                SuggestRoutesState.NORMAL -> 0

                SuggestRoutesState.PICK_LOCATION -> {
                    // reset this variable, so, it is set to true only when a location is picked
                    //locationViewModel._clearSuggestRoutesListener.value = false
                    // enable map click listener
                    map.setOnMapClickListener { clickedLocation ->
                        Log.i("user tapped the map: ", "location $clickedLocation")
                        confirmLocationAlert(clickedLocation)
                    }
                    pickLocationAlert()
                }

                SuggestRoutesState.CLEAN_CLICK_LISTENER -> {

                }

                SuggestRoutesState.CONFIRMED_LOCATION -> {
                    // When location is confirmed, no need to listen to pick location
                    map.setOnMapClickListener {
                        Log.i("if closed suggest route panel", "set onMapClick null")
                    }
                    // should display suggest routes menu here
                    locationViewModel._runSuggestRoutesFragment.value = true
                }

                SuggestRoutesState.SEARCHING -> {

                    1
                }

                SuggestRoutesState.DISPLAY_ROUTES -> {

                    2
                }

                SuggestRoutesState.DISPLAY_CHOSEN -> {
                    3

                }

                SuggestRoutesState.CLEAR_ROUTE -> {
                    clearPath()
                }

                SuggestRoutesState.END -> {
                    clearPath()
                }

                SuggestRoutesState.RESTART -> {
                    // clean route line
                    4
                }
            }
        })
    }
}

/*
    private fun constructPath() {
        if (path.value!!.isNotEmpty()) {
            val opts = PolylineOptions().addAll(path.value!!).color(Color.BLUE).width(
                PATH_LINE_WIDTH)
            locationViewModel._routeLine.value = map.addPolyline(opts)
            val transformedPoints = transformPointsToMap(path.value!!)
            // create the Route object and share it
            locationViewModel._routeToShare.value = Route(transformedPoints)
        }
    }

        var location = LatLng(43.6532, 79.3832)

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


*/