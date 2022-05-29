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
import com.bitpunchlab.android.shareroutes.R
import com.bitpunchlab.android.shareroutes.databinding.FragmentShareARouteBinding
import com.bitpunchlab.android.shareroutes.map.LocationInfoViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient

private const val TAG = "ShareARouteFragment"

class ShareARouteFragment : Fragment() {

    private var _binding : FragmentShareARouteBinding? = null
    private val binding get() = _binding!!
    private val enabledLocation = MutableLiveData<Boolean>(false)
    private lateinit var placesClient: PlacesClient
    private lateinit var locationViewModel: LocationInfoViewModel
    private lateinit var fusedLocationProviderClient : FusedLocationProviderClient
    private var lastKnowLocation : Location? = null

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
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        checkLocationPermission()

        Places.initialize(requireContext(), MAPS_API_KEY)
        placesClient = Places.createClient(requireContext())

        enabledLocation.observe(viewLifecycleOwner, Observer { enabled ->
            if (enabled) {
                findCurrentLocation()
            }
        })

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
}