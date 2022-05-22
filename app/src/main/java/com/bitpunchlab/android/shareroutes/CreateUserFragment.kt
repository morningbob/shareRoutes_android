package com.bitpunchlab.android.shareroutes

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.shareroutes.databinding.FragmentCreateUserBinding

private const val TAG = "CreateUserFragment"

class CreateUserFragment : Fragment() {

    private var _binding : FragmentCreateUserBinding? = null
    private val binding get() = _binding!!
    private lateinit var loginViewModel: LoginViewModel

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

        loginViewModel.email.observe(viewLifecycleOwner, Observer { email ->
            email?.let {
                Log.i("valid email? ", loginViewModel.isEmailValid().toString())
            }
        })

        loginViewModel.password.observe(viewLifecycleOwner, Observer { password ->
            password?.let {
                Log.i("valid password? ", loginViewModel.isPasswordValid().toString())
            }
        })

        loginViewModel.confirmPassword.observe(viewLifecycleOwner, Observer { confirmPassword ->
            confirmPassword?.let {
                Log.i("passwords the same? ", loginViewModel.isConfirmPasswordValid().toString())
            }
        })

        return binding.root
    }


}