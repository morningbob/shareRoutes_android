package com.bitpunchlab.android.shareroutes

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.shareroutes.databinding.FragmentLoginBinding

private const val TAG = "LoginFragment"

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var loginViewModel: LoginViewModel
    private var loginName: String? = null
    private var loginPassword: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        loginViewModel = ViewModelProvider(requireActivity(), LoginViewModelFactory(requireActivity()))
            .get(LoginViewModel::class.java)

        loginViewModel.isLoggedIn.observe(viewLifecycleOwner, Observer { loggedIn ->
            if (loggedIn) {

            }
        })

        binding.buttonLogin.setOnClickListener {
            // check if there is value in both edittext field
            loginName = binding.edittextName.text.toString()
            if (!loginName.isNullOrEmpty() && !loginPassword.isNullOrEmpty()) {
                Log.i(TAG, "got login name: $loginName")
                Log.i(TAG, "got password: $loginPassword")
                // authenticate
                loginViewModel.authenticateUser(loginName!!, loginPassword!!)
            } else {
                // alert user
            }
        }

        binding.buttonCreateAccount.setOnClickListener {
            findNavController().navigate(R.id.action_LoginFragment_to_createUserFragment)
        }

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}