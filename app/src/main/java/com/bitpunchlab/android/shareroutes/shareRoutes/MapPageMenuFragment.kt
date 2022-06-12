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

        binding.searchLocationButton.setOnClickListener {
            if (binding.searchMapEditText.text.toString() != null ||
                binding.searchMapEditText.text != null) {
                locationViewModel._searchTerm.value = binding.searchMapEditText.text.toString()
                Log.i("search button", "got search term ${binding.searchMapEditText.text.toString()}")
            }
        }

        binding.shareRouteButton.setOnClickListener {
            //findNavController().navigate(R.id.a)
            locationViewModel._shouldNavigateShareRoute.value = true
        }



        return binding.root
    }


}