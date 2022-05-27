package com.bitpunchlab.android.shareroutes

import android.app.AlertDialog
import android.content.DialogInterface
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
            loginViewModel.authenticateUser()
        }

        binding.buttonCreateAccount.setOnClickListener {
            findNavController().navigate(R.id.action_LoginFragment_to_createUserFragment)
        }

        loginViewModel.loggedInUser.observe(viewLifecycleOwner, Observer { loggedIn ->
            if (loggedIn) {
                Log.i(TAG, "logged in user")
                // I clear the states in both places because if loggedIn is null,
                // we won't clear the states
                //loginViewModel.resetLoginState()
                findNavController().navigate(R.id.action_LoginFragment_to_MainFragment)

            } else if (!loggedIn){
                Log.i(TAG, "failed to login user")
                loginViewModel.resetLoginState()

            }
            // if loggedIn == null, that's the case when user just see the login page without
            // doing anything, so we'll do nothing because nothing is wrong.
        })

        loginViewModel.loginError.observe(viewLifecycleOwner, Observer { error ->
            if (error) {
                loginFailureAlert()
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

    private fun loginFailureAlert() {
        val loginAlert = AlertDialog.Builder(context)

        loginAlert.setCancelable(false)
        loginAlert.setTitle(getString(R.string.login_failure_alert_title))
        loginAlert.setMessage(getString(R.string.login_failure_alert_desc))
        loginAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener() { dialog, button ->

            })

        loginAlert.show()
    }
}