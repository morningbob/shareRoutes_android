package com.bitpunchlab.android.shareroutes

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

private const val TAG = "PermissionFragment"

class PermissionFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var enabledLocation = MutableLiveData<Boolean>(false)

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

        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        checkLocationPermission()

        enabledLocation.observe(viewLifecycleOwner, Observer { enabled ->
            if (enabled) {
                testIfLogin()
            }
        })

        return inflater.inflate(R.layout.fragment_permission, container, false)
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enabledLocation.value = true
            // continue with the app, go to the main fragment
        } else {
            // request permission
            requestLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

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

    private fun testIfLogin() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.i(TAG, "already logged in")
            // navigate to Main Fragment
            findNavController().navigate(R.id.action_permissionFragment_to_MainFragment)
        } else {
            // navigate to Login Fragment
            findNavController().navigate(R.id.action_permissionFragment_to_LoginFragment)
        }
    }
}