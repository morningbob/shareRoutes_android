package com.bitpunchlab.android.shareroutes

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.shareroutes.databinding.FragmentCreateUserBinding

private const val TAG = "CreateUserFragment"

class CreateUserFragment : Fragment() {

    private var _binding : FragmentCreateUserBinding? = null
    private val binding get() = _binding!!
    private lateinit var loginViewModel: LoginViewModel
    private var email: String? = null
    private var name: String? = null
    private var password: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCreateUserBinding.inflate(inflater, container, false)
        loginViewModel = ViewModelProvider(requireActivity(), LoginViewModelFactory(requireActivity()))
            .get(LoginViewModel::class.java)

        binding.buttonSend.setOnClickListener {
            // check all the fields are not empty
            // need to confirm passwords are the same and with some length
            //loginViewModel.createNewUser()
            //validatePassword()
        }

        return binding.root
    }

    private fun validatePassword(password: String, confirmPassword: String) : Boolean {
        // check for length, more than 8 characters
        if (password.count() < 8) {
            Log.i(TAG, "password is less than 8 characters")
            return false
        }
        // check same password
        if (password != confirmPassword) {
            Log.i(TAG, "password and confirm password are not the same.")
            return false
        }

        return true
    }
}