package com.bitpunchlab.android.shareroutes.shareRoutes

import android.app.AlertDialog
import android.content.DialogInterface
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.navigation.ui.NavigationUI
import com.bitpunchlab.android.shareroutes.*
import com.bitpunchlab.android.shareroutes.databinding.FragmentMapPageBinding
import com.bitpunchlab.android.shareroutes.map.LocationInfoViewModel
import com.bitpunchlab.android.shareroutes.suggestRoutes.SuggestRoutesFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.net.PlacesClient
import java.util.*

// this class responsible for displaying the share routes interface
// the map view, the controls of sharing.
// it's layout include a fragment container to host the supportMapFragment
// it communicates with show map fragment by using the location view model
// it also deals with the database for sharing routes and getting routes suggestions

// I need to find out the city of the route is in.
// This info is for the user to search suggested routes in a city.
// So, all the routes in the city will be shown to the user.
// This info also help identifying the route from the other routes.
// I use Geocoding API to get these info from a LatLng point in the route.
// Route class will store all these info.
class MapPageFragment : Fragment() {

    private var _binding : FragmentMapPageBinding? = null
    private val binding get() = _binding!!
    private lateinit var locationViewModel: LocationInfoViewModel
    private lateinit var firebaseViewModel: FirebaseClientViewModel
    private lateinit var geoCoder : Geocoder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        _binding = FragmentMapPageBinding.inflate(inflater, container, false)
        locationViewModel = ViewModelProvider(requireActivity())
            .get(LocationInfoViewModel::class.java)
        firebaseViewModel = ViewModelProvider(requireActivity(), FirebaseClientViewModelFactory(requireActivity()))
            .get(FirebaseClientViewModel::class.java)

        geoCoder = Geocoder(requireContext(), Locale.getDefault())
        observeAppState()

        locationViewModel.runSuggestRoutesFragment.observe(viewLifecycleOwner, Observer { run ->
            if (run) {
                prepareLayoutForSuggestion()
            }
        })

        firebaseViewModel.shareSuccess.observe(viewLifecycleOwner, Observer { share ->
            if (share) {
                locationViewModel._shareRouteAppState.value = ShareRouteState.SHARED
            }
        })

        locationViewModel.dismissMapPage.observe(viewLifecycleOwner, Observer { dismiss ->
            if (dismiss) {
                // we set the suggest state to normal, so there won't be listeners attach
                // to the map before it is initialized again when navigating from main
                // fragment to map page fragment the second time.  Everything needs to
                // start fresh
                locationViewModel.suggestRoutesAppState.value = SuggestRoutesState.NORMAL
                findNavController().popBackStack()
            }
        })

        firebaseViewModel.databaseError.observe(viewLifecycleOwner, Observer { error ->
            if (error) {
                userDataErrorAlert()
            }
        })

        // we post a request to collect the point's info, like city, here
        // Later, send to Firebase for search routes in the same city
        locationViewModel.chosenSearchLocation.observe(viewLifecycleOwner, Observer { location ->
            location?.let {
                locationViewModel.searchCity.value = searchPlaceCity(location)
            }
        })

        locationViewModel.searchCity.observe(viewLifecycleOwner, Observer { city ->
            firebaseViewModel.searchRoutesSameCity(city)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_map_page, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return NavigationUI.onNavDestinationSelected(item,
            requireView().findNavController())
                || super.onOptionsItemSelected(item)
    }

    private fun insertMapFragment() {
        val mapFragment = MapFragment()
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.map_fragment_container, mapFragment).commit()
    }

    private fun insertMenuFragment() {
        val mapPageMenuFragment = MapPageMenuFragment()
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.map_menu_fragment_container, mapPageMenuFragment).commit()
    }

    private fun insertShareMenuFragment() {
        val shareMenuFragment = ShareRouteMenuFragment()

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

        createAlert.show()
    }

    private fun shareAlert() {
        val shareAlert = AlertDialog.Builder(requireContext())

        shareAlert.setCancelable(false)
        shareAlert.setTitle(getString(R.string.share_the_route_alert_title))
        shareAlert.setMessage(getString(R.string.share_the_route_alert_desc))

        shareAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                locationViewModel._shareRouteAppState.value = ShareRouteState.SAVE_ROUTE
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

    private fun shareSuccessAlert() {
        val shareAlert = AlertDialog.Builder(requireContext())

        shareAlert.setCancelable(false)
        shareAlert.setTitle(getString(R.string.share_success_alert_title))
        shareAlert.setMessage(getString(R.string.share_success_alert_desc))

        shareAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing.
                // reset notifier
                firebaseViewModel._shareSuccess.value = false
            })

        shareAlert.show()
    }

    private fun userDataErrorAlert() {
        val dataErrorAlert = AlertDialog.Builder(requireContext())

        dataErrorAlert.setCancelable(false)
        dataErrorAlert.setTitle("User Data Error")
        dataErrorAlert.setMessage("There is error in getting user data.  Please logout and login again to try.  Thank you very much.")

        dataErrorAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing, user need to do it themselves
            })

        dataErrorAlert.show()
    }

    private fun searchPlaceCity(latLngPoint: LatLng) : String? {
        val addressList = geoCoder.getFromLocation(latLngPoint.latitude, latLngPoint.longitude, 1)

        if (!addressList.isNullOrEmpty()) {
            return addressList.get(0).locality
        }
        return null
    }

    private fun observeAppState() {
        locationViewModel.shareRouteAppState.observe(viewLifecycleOwner, Observer { appState ->
            when (appState) {
                ShareRouteState.NORMAL -> 0

                ShareRouteState.START -> {
                    insertShareMenuFragment()
                }

                ShareRouteState.ADD_MARKER -> {
                    // check if there is at least 2 markers
                    // check if there is a route line
                    createMarkerAlert()
                }

                ShareRouteState.ROUTE_TO_BE_CREATED -> {
                    // after the route is created, keep track of if it is shared
                    if (locationViewModel.markerList.value!!.size >= 2) {
                        //locationViewModel._readyToCreateRoute.value = true
                        locationViewModel.shareRouteAppState.value = ShareRouteState.CREATING_ROUTE
                    } else {
                        // alert user that there is not enough markers
                        Log.i("create route button", "marker list size < 2")
                        noMarkersAlert()
                    }

                }

                ShareRouteState.CREATING_ROUTE -> {
                    0
                }

                ShareRouteState.ROUTE_CREATED -> 0

                ShareRouteState.TO_BE_SHARED -> {
                    // here we check if there is a route line created yet, otherwise can't share
                    if (locationViewModel.routeLine.value != null) {
                        shareAlert()
                    } else {
                        // route line is empty, not ready to share
                        notReadyShareAlert()
                    }
                }

                ShareRouteState.SAVE_ROUTE -> {
                    // contact login view model to save the route in the user object and route table
                    // in Firebase database
                    // check if there is a route
                    if (locationViewModel.routeToShare.value != null) {
                        firebaseViewModel._routeToShare.value = locationViewModel.routeToShare.value!!
                        firebaseViewModel.saveRoute(firebaseViewModel.routeToShare.value!!)

                    } else {
                        Log.i("error", "there is no route from map fragment")
                        notReadyShareAlert()
                    }
                }

                ShareRouteState.CANCEL_SHARING -> {
                    // besides changing back to map menu, we also need to clean all routes related info
                    insertMenuFragment()
                    //locationViewModel._shouldRestart.value = true
                    // also clean suggest routes related info.
                    //locationViewModel._shouldSuggestRoutes.value = false
                }

                ShareRouteState.SHARED -> {
                    // don't let user share it again
                    // do cleaning, show alert
                    shareSuccessAlert()
                    //locationViewModel._shareRouteAppState.value = ShareRouteState.SHARED
                    // clear previous path info
                    //locationViewModel._clearRouteInfo.value = true
                    // reset after saving

                    //locationViewModel._routeLine.value?.remove()
                    //locationViewModel.routeLine.value = null
                    locationViewModel._routeToShare.value = null
                    firebaseViewModel._routeToShare.value = null

                }
                ShareRouteState.RESTART -> {
                    // clean all info and route line
                    4
                }
            }
        })

        locationViewModel.suggestRoutesAppState.observe(viewLifecycleOwner, Observer { appState ->
            when (appState) {
                SuggestRoutesState.NORMAL -> 0

                SuggestRoutesState.START -> {
                    prepareLayoutForSuggestion()
                }

                SuggestRoutesState.SEARCHING -> {

                    1
                }

                SuggestRoutesState.PICK_LOCATION -> {

                }

                SuggestRoutesState.DISPLAY_ROUTES -> {

                    2
                }

                SuggestRoutesState.DISPLAY_CHOSEN -> {
                    3
                }

                SuggestRoutesState.END -> {
                    prepareLayoutBackToNormal()
                }

                SuggestRoutesState.RESTART -> {
                    // clean route line
                    4
                }
            }
        })
    }
}