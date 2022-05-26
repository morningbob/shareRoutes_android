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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        loginViewModel = ViewModelProvider(requireActivity(), LoginViewModelFactory(requireActivity()))
            .get(LoginViewModel::class.java)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.loginViewModel = loginViewModel

        loginViewModel.readyLoginLiveData.observe(viewLifecycleOwner, Observer { value ->
            if (value) {
                binding.buttonLogin.visibility = View.VISIBLE
            }
        })

        binding.buttonLogin.setOnClickListener {
            Log.i("loginEmail: ", loginViewModel.loginEmail.value!!)
            loginViewModel.authenticateUser()
        }

        binding.buttonCreateAccount.setOnClickListener {
            findNavController().navigate(R.id.action_LoginFragment_to_createUserFragment)
        }

        loginViewModel.currentUser.observe(viewLifecycleOwner, Observer { user ->
            if (user != null) {
                Log.i(TAG, "logged in user")
                findNavController().navigate(R.id.action_LoginFragment_to_MainFragment)
            } else {
                Log.i(TAG, "failed to login user")

            }
        })

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