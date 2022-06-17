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
import com.bitpunchlab.android.shareroutes.FirebaseClientViewModel
import com.bitpunchlab.android.shareroutes.FirebaseClientViewModelFactory
import com.bitpunchlab.android.shareroutes.R
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
        locationViewModel._clearSuggestRoutesInfo.value = false

        locationViewModel.chosenRoute.observe(viewLifecycleOwner, Observer { route ->
            route?.let {
                // construct the route in the map
                //findNavController().navigate(R.id.action_suggestRoutesFragment_to_mapPageFragment)
                locationViewModel._shouldShowRoute.value = true
            }
        })
        binding.closeTextview.setOnClickListener {
            locationViewModel._closeSuggestion.value = true
        }

        binding.clearRouteTextview.setOnClickListener {
            locationViewModel._shouldClearPath.value = true
        }
        // retrieve the search location latlng.
        // calculate the distance
        // query firebase database
        firebaseViewModel.searchRoutes(locationViewModel.chosenSearchLocation.value!!)

        firebaseViewModel.routesResult.observe(viewLifecycleOwner, Observer { result ->
            Log.i("suggest route fragment: ", "got back routes, routes list size: ${result.size}")
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}