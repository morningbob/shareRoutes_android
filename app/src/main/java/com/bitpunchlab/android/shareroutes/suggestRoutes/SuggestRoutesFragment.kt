package com.bitpunchlab.android.shareroutes.suggestRoutes

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.shareroutes.*
import com.bitpunchlab.android.shareroutes.databinding.FragmentSuggestRoutesBinding
import com.bitpunchlab.android.shareroutes.map.LocationInfoViewModel

// this fragment shows a list of possible routes near the marker
// upon clicked, the route should be drawn on the map
// it needs to retrieve routes info from the Firebase database
// and interpret the results
// the user should be able to choose a place and place the marker.
// the chosen location is compared to all the routes in the city
class SuggestRoutesFragment : Fragment() {

    private var _binding : FragmentSuggestRoutesBinding? = null
    private val binding get() = _binding!!
    private lateinit var locationViewModel: LocationInfoViewModel
    private lateinit var firebaseViewModel: FirebaseClientViewModel
    private lateinit var routeAdapter: RouteListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSuggestRoutesBinding.inflate(inflater, container, false)
        locationViewModel = ViewModelProvider(requireActivity())
            .get(LocationInfoViewModel::class.java)
        firebaseViewModel = ViewModelProvider(requireActivity(), FirebaseClientViewModelFactory(requireActivity()))
            .get(FirebaseClientViewModel::class.java)
        routeAdapter = RouteListAdapter( RouteOnClickListener { route ->
            locationViewModel.onRouteClicked(route)
        })
        binding.routesRecycler.adapter = routeAdapter

        firebaseViewModel.routesResult.observe(viewLifecycleOwner, Observer { routeList ->
            if (!routeList.isNullOrEmpty()) {
                Log.i("suggest fragment", "got routes: ${routeList.size}")
                routeAdapter.submitList(routeList)
                routeAdapter.notifyDataSetChanged()
            }
        })
        // reset
        //locationViewModel._clearSuggestRoutesListener.value = false

        //locationViewModel.chosenRoute.observe(viewLifecycleOwner, Observer { route ->
        ///    route?.let {
                // construct the route in the map
                //locationViewModel._shouldShowRoute.value = true
        //    }
        //})

        binding.closeTextview.setOnClickListener {
            locationViewModel._suggestRoutesAppState.value = SuggestRoutesState.END
        }

        binding.clearRouteTextview.setOnClickListener {
            locationViewModel._suggestRoutesAppState.value = SuggestRoutesState.CLEAR_ROUTE
        }
        // retrieve the search location latlng.
        // calculate the distance
        // query firebase database
        firebaseViewModel.searchRoutes(locationViewModel.chosenSearchLocation.value!!)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeAppState() {

        locationViewModel.suggestRoutesAppState.observe(viewLifecycleOwner, Observer { appState ->
            when (appState) {
                SuggestRoutesState.NORMAL -> 0

                SuggestRoutesState.PICK_LOCATION -> {
                    0
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

                SuggestRoutesState.RESTART -> {
                    // clean route line
                    4
                }
            }
        })
    }
}