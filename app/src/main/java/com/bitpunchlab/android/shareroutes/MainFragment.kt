package com.bitpunchlab.android.shareroutes

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
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

        //setHasOptionsMenu(true)

        _binding = FragmentMainBinding.inflate(inflater, container, false)
        loginViewModel = ViewModelProvider(requireActivity(), LoginViewModelFactory(requireActivity()))
            .get(LoginViewModel::class.java)

        binding.lifecycleOwner = viewLifecycleOwner

        checkLocationPermission()

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

        // navigate to share a route fragment once we got location permission
        enabledLocation.observe(viewLifecycleOwner, Observer { enabled ->
            if (enabled) {
                // navigate to share a route page
                //findNavController().navigate(R.id.action_MainFragment_to_shareARouteFragment)
            }
        })

        binding.buttonShareRoutes.setOnClickListener {
            //checkLocationPermission()
            findNavController().navigate(R.id.action_MainFragment_to_shareARouteFragment)
        }

        binding.buttonSuggestRoutes.setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_suggestRoutesFragment)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return NavigationUI.onNavDestinationSelected(item,
            requireView().findNavController())
                || super.onOptionsItemSelected(item)
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