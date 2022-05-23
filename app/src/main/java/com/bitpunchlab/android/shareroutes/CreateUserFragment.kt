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

        binding.lifecycleOwner = viewLifecycleOwner
        binding.loginViewModel = loginViewModel

        binding.buttonSend.setOnClickListener {
            // check all the fields are not empty
            // need to confirm passwords are the same and with some length
            //loginViewModel.createNewUser()
            //validatePassword()
            //Log.i("email: ", loginViewModel.userInfo.value!!.email)
            //Log.i("username: ", loginViewModel.username.value!!)
            Log.i("is valid email? ", loginViewModel.emailValid.value.toString())
        }

        loginViewModel.username.observe(viewLifecycleOwner, Observer { name ->
            name?.let {
                Log.i("username from observing: ", name)
            }
        })

        loginViewModel.userEmail.observe(viewLifecycleOwner, Observer { email ->
            email?.let {
                Log.i("email from observing", email)
            }
        })

        loginViewModel.emailValid.observe(viewLifecycleOwner, Observer { value ->
            value?.let {
                if (value) {
                    Log.i("test: ", "valid email")
                } else {
                    Log.i("test: ", "invalid email")
                }
            }
        })

        loginViewModel.
/*
        loginViewModel.userName.observe(viewLifecycleOwner, Observer { name ->
            name?.let {
                Log.i("userName: ", name)
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
*/
        return binding.root
    }


}