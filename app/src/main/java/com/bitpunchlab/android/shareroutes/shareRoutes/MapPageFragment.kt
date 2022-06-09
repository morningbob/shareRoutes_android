package com.bitpunchlab.android.shareroutes.shareRoutes

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bitpunchlab.android.shareroutes.R
import com.bitpunchlab.android.shareroutes.databinding.FragmentMapPageBinding


class MapPageFragment : Fragment() {

    private var _binding : FragmentMapPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapPageBinding.inflate(inflater, container, false)

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
        val mapFragment = ShowMapFragment()
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.map_fragment_container, mapFragment).commit()
    }

    private fun insertMenuFragment() {
        val mapPageMenuFragment = MapPageMenuFragment()
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.map_menu_fragment_container, mapPageMenuFragment).commit()
    }

}