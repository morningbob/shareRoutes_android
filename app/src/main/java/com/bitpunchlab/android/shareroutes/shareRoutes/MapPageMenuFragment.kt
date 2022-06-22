package com.bitpunchlab.android.shareroutes.shareRoutes

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.shareroutes.R
import com.bitpunchlab.android.shareroutes.ShareRouteState
import com.bitpunchlab.android.shareroutes.SuggestRoutesState
import com.bitpunchlab.android.shareroutes.databinding.FragmentMapPageBinding
import com.bitpunchlab.android.shareroutes.databinding.FragmentMapPageMenuBinding
import com.bitpunchlab.android.shareroutes.map.LocationInfoViewModel


class MapPageMenuFragment : Fragment() {

    private var _binding : FragmentMapPageMenuBinding? = null
    private val binding get() = _binding!!
    private lateinit var locationViewModel: LocationInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapPageMenuBinding.inflate(inflater, container, false)
        locationViewModel = ViewModelProvider(requireActivity())
            .get(LocationInfoViewModel::class.java)

        binding.shareRouteButton.setOnClickListener {
            locationViewModel._shareRouteAppState.value = ShareRouteState.START
        }

        // this button starts the flow of suggesting routes
        // first, it let user pick a location
        // then run the suggest routes fragment and search through the database
        binding.suggestRoutesButton.setOnClickListener {
            locationViewModel._suggestRoutesAppState.value = SuggestRoutesState.PICK_LOCATION
        }

        binding.myLocationButton.setOnClickListener {
            locationViewModel._showMyLocation.value = true
        }

        binding.mainMenuButton.setOnClickListener {
            // clean last know location, other it doesn't wait for map to initialize
            // and use the map
            locationViewModel._lastKnownLocation.value = null
            // dismiss map page fragment
            locationViewModel._dismissMapPage.value = true
        }

        return binding.root
    }


}