package com.bitpunchlab.android.shareroutes.shareRoutes

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.shareroutes.BuildConfig.MAPS_API_KEY
import com.bitpunchlab.android.shareroutes.LoginViewModel
import com.bitpunchlab.android.shareroutes.LoginViewModelFactory
import com.bitpunchlab.android.shareroutes.R
import com.bitpunchlab.android.shareroutes.databinding.FragmentShareARouteBinding
import com.bitpunchlab.android.shareroutes.map.LocationInfoViewModel
import com.bitpunchlab.android.shareroutes.models.Route
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient

private const val TAG = "ShareARouteFragment"

// this class responsible for displaying the share routes interface
// the map view, the controls of sharing.
// it's layout include a fragment container to host the supportMapFragment
// it communicates with show map fragment by using the location view model
// it also deals with the database for sharing routes and getting routes suggestions
class ShareARouteFragment : Fragment() {

    private var _binding : FragmentShareARouteBinding? = null
    private val binding get() = _binding!!
    private val enabledLocation = MutableLiveData<Boolean>(false)
    private lateinit var placesClient: PlacesClient
    private lateinit var locationViewModel: LocationInfoViewModel
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var fusedLocationProviderClient : FusedLocationProviderClient

    private var requestLocationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->
        if (isGranted) {
            Log.i(TAG, "location permission granted")
            enabledLocation.value = true
        } else {
            // explain to user that location permission is required for the app
            locationPermissionAlert()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentShareARouteBinding.inflate(layoutInflater, container, false)
        locationViewModel = ViewModelProvider(requireActivity())
            .get(LocationInfoViewModel::class.java)
        loginViewModel = ViewModelProvider(requireActivity(), LoginViewModelFactory(requireActivity()))
            .get(LoginViewModel::class.java)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        checkLocationPermission()

        Places.initialize(requireContext(), MAPS_API_KEY)
        placesClient = Places.createClient(requireContext())

        enabledLocation.observe(viewLifecycleOwner, Observer { enabled ->
            if (enabled) {
                findCurrentLocation()
                findDeviceLocation()
            }
        })

        binding.startRouteButton.setOnClickListener {
            // alert user to create markers
            createMarkerAlert()
            // when user clicks on the button, the tap listener is then activated
            // so user can tap to add markers
            locationViewModel._startCreatingRoute.value = true
        }

        binding.createRouteButton.setOnClickListener {
            // check if there are at least 2 markers in the marker list before calling the function
            if (locationViewModel.markerList.value!!.size >= 2) {
                locationViewModel._readyToCreateRoute.value = true
            } else {
                // alert user that there is not enough markers
                noMarkersAlert()
            }
        }

        binding.clearPathButton.setOnClickListener {
            locationViewModel._shouldClearPath.value = true
        }

        binding.shareButton.setOnClickListener {
            shareAlert()
        }

        binding.restartButton.setOnClickListener {
            locationViewModel._shouldRestart.value = true
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        insertMapFragment()
    }

    @SuppressLint("MissingPermission")
    private fun findCurrentLocation() {
        val placeFields: List<Place.Field> = listOf(Place.Field.NAME)
        val request: FindCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)

        val placeResponse = placesClient.findCurrentPlace(request)
        placeResponse.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val response = task.result
                locationViewModel._currentListLocation.postValue(response?.placeLikelihoods)
                response?.placeLikelihoods.isNullOrEmpty().let {
                    locationViewModel._currentLocation.postValue(response?.placeLikelihoods!!.get(0))
                }

                for (placeLikelihood:  PlaceLikelihood in response?.placeLikelihoods
                    ?: emptyList()) {
                    Log.i(
                        TAG,
                        "Place '${placeLikelihood.place.name}' has likelihood: ${placeLikelihood.likelihood}"
                    )
                }
            } else {
                val exception = task.exception
                if (exception is ApiException) {
                    Log.e(TAG, "Place not found: ${exception.statusCode}")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun findDeviceLocation() {
        val locationResult = fusedLocationProviderClient.lastLocation
        locationResult.addOnCompleteListener(requireActivity()) { task ->
            if (task.isSuccessful) {
                locationViewModel._lastKnownLocation.postValue(task.result)

            }
        }
    }

    private fun insertMapFragment() {
        val mapFragment = ShowMapFragment()
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.map_fragment_container, mapFragment).commit()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Location Permission is granted")
            enabledLocation.value = true
        } else {
            getLocationPermission()
        }
    }

    private fun getLocationPermission() {
        requestLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }


    private fun locationPermissionAlert() {
        val locationAlert = AlertDialog.Builder(context)

        locationAlert.setTitle(getString(R.string.location_permission_alert_title))
        locationAlert.setMessage(getString(R.string.location_permission_alert_desc))
        locationAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener() { dialog, button ->
                requestLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            })
        locationAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener() { dialog, button ->
                // do nothing, and let user stay in permission fragment
            })
        locationAlert.show()
    }

    private fun createMarkerAlert() {
        val createAlert = AlertDialog.Builder(requireContext())

        createAlert.setCancelable(false)
        createAlert.setTitle(getString(R.string.create_route_intro_alert_title))
        createAlert.setMessage(getString(R.string.create_route_intro_alert_desc))
        createAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing, the show map fragment is enabling click listener
            })
        /*
        createAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener { dialog, button ->
                // don't enable click listener
                
            })

         */
        createAlert.show()
    }

    private fun noMarkersAlert() {
        val markerAlert = AlertDialog.Builder(requireContext())

        markerAlert.setTitle(getString(R.string.markers_num_alert_title))
        markerAlert.setMessage(getString(R.string.marker_num_alert_desc))
        markerAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
            // do nothing and wait for user to add markers
        })

        markerAlert.show()
    }

    private fun shareAlert() {
        val shareAlert = AlertDialog.Builder(requireContext())

        shareAlert.setCancelable(false)
        shareAlert.setTitle("Share the Route")
        shareAlert.setMessage("Once the Firebase realtime database is set up, I will send it to the database.")

        shareAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // contact login view model to save the route in the user object and route table
                // in Firebase database
                // check if there is a route
                if (locationViewModel.routeToShare.value != null) {
                    loginViewModel._routeToShare.value = locationViewModel.routeToShare.value!!
                    //val newRoute = Route(pts = locationViewModel._routeToShare.value!!)
                    loginViewModel.saveRoute(loginViewModel.routeToShare.value!!)
                    // clear previous path info
                    locationViewModel._clearRouteInfo.value = true
                } else {
                    Log.i("error", "there is no route from map fragment")
                }

            })

        shareAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
            })

        shareAlert.show()
    }

}