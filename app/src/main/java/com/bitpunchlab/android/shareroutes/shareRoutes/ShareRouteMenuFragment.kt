package com.bitpunchlab.android.shareroutes.shareRoutes

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.shareroutes.R
import com.bitpunchlab.android.shareroutes.ShareRouteState
import com.bitpunchlab.android.shareroutes.SuggestRoutesState
import com.bitpunchlab.android.shareroutes.databinding.FragmentShareRouteMenuBinding
import com.bitpunchlab.android.shareroutes.map.LocationInfoViewModel


class ShareRouteMenuFragment : Fragment() {

    private var _binding : FragmentShareRouteMenuBinding? = null
    private val binding get() = _binding!!
    private lateinit var locationViewModel: LocationInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentShareRouteMenuBinding.inflate(inflater, container, false)
        locationViewModel = ViewModelProvider(requireActivity())
            .get(LocationInfoViewModel::class.java)

        observeAppState()

        binding.addMarkerButton.setOnClickListener {
            // notice map page fragment to display alert and pass the instruction to
            // map fragment
            //locationViewModel._shouldAddMarker.value = true
            locationViewModel.shareRouteAppState.value = ShareRouteState.ADD_MARKER
        }

        binding.createRouteButton.setOnClickListener {
            //locationViewModel._createRouteChecking.value = true
            locationViewModel._shareRouteAppState.value = ShareRouteState.ROUTE_TO_BE_CREATED
        }

        binding.shareButton.setOnClickListener {
            //locationViewModel._shouldShareRoute.value = true
            locationViewModel._shareRouteAppState.value = ShareRouteState.TO_BE_SHARED
        }

        binding.clearPathButton.setOnClickListener {
            locationViewModel._shouldClearPath.value = true
        }

        binding.restartButton.setOnClickListener {
            //locationViewModel._shouldRestart.value = true
            locationViewModel._shareRouteAppState.value = ShareRouteState.RESTART
        }

        binding.cancelSharingButton.setOnClickListener {
            //locationViewModel._shouldCancelSharing.value = true
            locationViewModel._shareRouteAppState.value = ShareRouteState.CANCEL_SHARING
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeAppState() {
        locationViewModel.shareRouteAppState.observe(viewLifecycleOwner, Observer { appState ->
            when (appState) {
                ShareRouteState.NORMAL -> 0

                ShareRouteState.ADD_MARKER -> 1


                ShareRouteState.ROUTE_TO_BE_CREATED -> {
                    // after the route is created, keep track of if it is shared
                    2
                }

                ShareRouteState.SHARED -> {
                    // don't let user share it again
                    3
                }
                ShareRouteState.RESTART -> {
                    // clean all info and route line
                    0
                }
            }
        })

        locationViewModel.suggestRoutesAppState.observe(viewLifecycleOwner, Observer { appState ->
            when (appState) {
                SuggestRoutesState.NORMAL -> 0

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