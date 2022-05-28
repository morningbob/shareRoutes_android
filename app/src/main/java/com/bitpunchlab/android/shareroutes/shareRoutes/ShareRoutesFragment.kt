package com.bitpunchlab.android.shareroutes.shareRoutes

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bitpunchlab.android.shareroutes.R
import com.bitpunchlab.android.shareroutes.databinding.FragmentShareRoutesBinding


class ShareRoutesFragment : Fragment() {

    private var _binding : FragmentShareRoutesBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentShareRoutesBinding.inflate(layoutInflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        insertMapFragment()
    }

    private fun insertMapFragment() {
        val mapFragment = ShowMapFragment()
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.map_fragment_container, mapFragment).commit()
    }
}