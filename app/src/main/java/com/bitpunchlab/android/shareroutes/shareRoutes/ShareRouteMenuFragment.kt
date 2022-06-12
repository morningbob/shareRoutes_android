package com.bitpunchlab.android.shareroutes.shareRoutes

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.shareroutes.R
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

        binding.addMarkerButton.setOnClickListener {
            // notice map page fragment to display alert and pass the instruction to
            // map fragment
            locationViewModel._shouldAddMarker.value = true
        }

        binding.createRouteButton.setOnClickListener {
            locationViewModel._createRouteChecking.value = true
        }

        binding.shareButton.setOnClickListener {
            locationViewModel._shouldShareRoute.value = true
        }

        binding.clearPathButton.setOnClickListener {
            locationViewModel._shouldClearPath.value = true
        }

        binding.restartButton.setOnClickListener {
            locationViewModel._shouldRestart.value = true
        }

        binding.cancelSharingButton.setOnClickListener {
            findNavController().popBackStack(R.id.MainFragment, false)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}