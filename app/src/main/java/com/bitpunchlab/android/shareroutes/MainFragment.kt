package com.bitpunchlab.android.shareroutes

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
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
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.shareroutes.databinding.FragmentMainBinding

private const val TAG = "MainFragment"

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var loginViewModel: LoginViewModel
    private val enabledLocation = MutableLiveData<Boolean>(false)

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMainBinding.inflate(inflater, container, false)
        loginViewModel = ViewModelProvider(requireActivity(), LoginViewModelFactory(requireActivity()))
            .get(LoginViewModel::class.java)

        binding.lifecycleOwner = viewLifecycleOwner

        binding.buttonLogout.setOnClickListener {
            Log.i(TAG, "logging out")
            loginViewModel.logoutUser()
            //findNavController().navigate(R.id.action_MainFragment_to_LoginFragment)
        }

        loginViewModel.loggedInUser.observe(viewLifecycleOwner, Observer { loggedIn ->
            if (loggedIn == null) {
                findNavController().navigate(R.id.action_MainFragment_to_LoginFragment)
            }
        })

        enabledLocation.observe(viewLifecycleOwner, Observer { enabled ->
            if (enabled) {
                //findCurrentLocation()
                // navigate to share a route page
                findNavController().navigate(R.id.action_MainFragment_to_shareARouteFragment)
            }
        })

        binding.buttonShareRoutes.setOnClickListener {
            //findNavController().navigate(R.id.action_MainFragment_to_shareARouteFragment)
            checkLocationPermission()
        }

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    private fun findCurrentLocation() {

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