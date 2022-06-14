package com.bitpunchlab.android.shareroutes.shareRoutes

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.shareroutes.*
import com.bitpunchlab.android.shareroutes.databinding.FragmentMapPageBinding
import com.bitpunchlab.android.shareroutes.map.LocationInfoViewModel
import com.bitpunchlab.android.shareroutes.suggestRoutes.SuggestRoutesFragment


class MapPageFragment : Fragment() {

    private var _binding : FragmentMapPageBinding? = null
    private val binding get() = _binding!!
    private lateinit var locationViewModel: LocationInfoViewModel
    private lateinit var firebaseViewModel: FirebaseClientViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapPageBinding.inflate(inflater, container, false)
        locationViewModel = ViewModelProvider(requireActivity())
            .get(LocationInfoViewModel::class.java)
        firebaseViewModel = ViewModelProvider(requireActivity(), FirebaseClientViewModelFactory(requireActivity()))
            .get(FirebaseClientViewModel::class.java)

        locationViewModel._shouldNavigateShareRoute.observe(viewLifecycleOwner, Observer { share ->
            if (share) {
                insertShareMenuFragment()
            }
        })

        locationViewModel.shouldAddMarker.observe(viewLifecycleOwner, Observer { addMarker ->
            if (addMarker) {
                createMarkerAlert()
            }
        })

        locationViewModel.createRouteChecking.observe(viewLifecycleOwner, Observer { check ->
            if (check) {
                // check if there are at least 2 markers in the marker list before calling the function
                if (locationViewModel.markerList.value!!.size >= 2) {
                    locationViewModel._readyToCreateRoute.value = true
                } else {
                    // alert user that there is not enough markers
                    Log.i("create route button", "marker list size < 2")
                    noMarkersAlert()
                }
            }
        })

        locationViewModel.shouldShareRoute.observe(viewLifecycleOwner, Observer { share ->
            if (share) {
                // here we check if there is a route line created yet, otherwise can't share
                if (locationViewModel.routeLine.value != null) {
                    shareAlert()
                } else {
                    // route line is empty, not ready to share
                    notReadyShareAlert()
                }
            }
        })

        locationViewModel.shouldCancelSharing.observe(viewLifecycleOwner, Observer { cancel ->
            if (cancel) {
                // besides changing back to map menu, we also need to clean all routes related info
                insertMenuFragment()
                locationViewModel._shouldRestart.value = true
            }
        })

        locationViewModel.runSuggestRoutesFragment.observe(viewLifecycleOwner, Observer { run ->
            if (run) {
                prepareLayoutForSuggestion()
            }
        })

        locationViewModel.closeSuggestion.observe(viewLifecycleOwner, Observer { close ->
            if (close) {
                prepareLayoutBackToNormal()
            }
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        insertMapFragment()
        insertMenuFragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun insertMapFragment() {
        val mapFragment = MapFragment()
        val transaction = childFragmentManager.beginTransaction()
        //transaction.addToBackStack("initial")
        transaction.replace(R.id.map_fragment_container, mapFragment).commit()
    }

    private fun insertMenuFragment() {
        val mapPageMenuFragment = MapPageMenuFragment()
        val transaction = childFragmentManager.beginTransaction()
        //transaction.addToBackStack("initial")
        transaction.replace(R.id.map_menu_fragment_container, mapPageMenuFragment).commit()
    }

    private fun insertShareMenuFragment() {
        val shareMenuFragment = ShareRouteMenuFragment()
        //val transaction = childFragmentManager.beginTransaction()
        //transaction.replace(R.id.map_menu_fragment_container, shareMenuFragment).commit()
        childFragmentManager.commit {
            setReorderingAllowed(true)
            addToBackStack("replacement")
            replace(R.id.map_menu_fragment_container, shareMenuFragment)
        }
    }

    private fun insertSuggestRoutesFragment() {
        val suggestFragment = SuggestRoutesFragment()
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.map_menu_fragment_container, suggestFragment).commit()
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
                    firebaseViewModel._routeToShare.value = locationViewModel.routeToShare.value!!
                    //val newRoute = Route(pts = locationViewModel._routeToShare.value!!)
                    firebaseViewModel.saveRoute(firebaseViewModel.routeToShare.value!!)
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

    private fun notReadyShareAlert() {
        val notShareAlert = AlertDialog.Builder(requireContext())

        notShareAlert.setCancelable(false)
        notShareAlert.setTitle("Not ready to share")
        notShareAlert.setMessage("Please make sure you created a route before sharing.")

        notShareAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing, wait for the user to create route
            })

        notShareAlert.show()
    }

    private fun prepareLayoutForSuggestion() {
        binding.autoCompleteFragmentLayout.isVisible = false
        // change layout properties
        // for map fragment
        var paramsMap = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0) as LinearLayout.LayoutParams
        paramsMap.weight = 5.0f
        binding.mapFragmentContainer.layoutParams = paramsMap
        // for map menu fragment
        var paramsMapMenu = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0) as LinearLayout.LayoutParams
        paramsMapMenu.weight = 5.0f
        binding.mapMenuFragmentContainer.layoutParams = paramsMapMenu

        insertSuggestRoutesFragment()
    }

    private fun prepareLayoutBackToNormal() {
        binding.autoCompleteFragmentLayout.isVisible = true
        // change layout properties back to normal
        // for map fragment
        var paramsMap = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0) as LinearLayout.LayoutParams
        paramsMap.weight = 7.0f
        binding.mapFragmentContainer.layoutParams = paramsMap
        // for map menu fragment
        var paramsMapMenu = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0) as LinearLayout.LayoutParams
        paramsMapMenu.weight = 2.0f
        binding.mapMenuFragmentContainer.layoutParams = paramsMapMenu

        insertMenuFragment()
    }
}