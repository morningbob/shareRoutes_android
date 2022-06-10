package com.bitpunchlab.android.shareroutes.shareRoutes

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bitpunchlab.android.shareroutes.R
import com.bitpunchlab.android.shareroutes.databinding.FragmentMapPageBinding
import com.bitpunchlab.android.shareroutes.databinding.FragmentMapPageMenuBinding


class MapPageMenuFragment : Fragment() {

    private var _binding : FragmentMapPageMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapPageMenuBinding.inflate(inflater, container, false)



        return binding.root
    }


}