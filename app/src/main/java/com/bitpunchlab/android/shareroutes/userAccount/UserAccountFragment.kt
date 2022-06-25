package com.bitpunchlab.android.shareroutes.userAccount

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.shareroutes.FirebaseClientViewModel
import com.bitpunchlab.android.shareroutes.FirebaseClientViewModelFactory
import com.bitpunchlab.android.shareroutes.R
import com.bitpunchlab.android.shareroutes.databinding.FragmentUserAccountBinding


class UserAccountFragment : Fragment() {

    private var _binding : FragmentUserAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseViewModel: FirebaseClientViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserAccountBinding.inflate(layoutInflater, container, false)
        firebaseViewModel = ViewModelProvider(requireActivity(), FirebaseClientViewModelFactory(requireActivity()))
            .get(FirebaseClientViewModel::class.java)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.firebaseViewModel = firebaseViewModel
        // consider
        binding.user = firebaseViewModel.userObject.value

        firebaseViewModel.emailValid.observe(viewLifecycleOwner, Observer { valid ->
            if (valid) {
                Log.i("user account", "about to update email")
                binding.buttonSend.visibility = View.VISIBLE
            }
        })

        firebaseViewModel.readyUpdatePasswordLiveData.observe(viewLifecycleOwner, Observer { ready ->
            if (ready) {
                Log.i("user account", "about to update password")
                binding.buttonSend.visibility = View.VISIBLE
            }
        })

        return binding.root
    }

}