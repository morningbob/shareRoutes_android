package com.bitpunchlab.android.shareroutes.authentication

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
import com.bitpunchlab.android.shareroutes.*
import com.bitpunchlab.android.shareroutes.databinding.FragmentLoginBinding

private const val TAG = "LoginFragment"

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseViewModel: FirebaseClientViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        firebaseViewModel = ViewModelProvider(requireActivity(), FirebaseClientViewModelFactory(requireActivity()))
            .get(FirebaseClientViewModel::class.java)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.firebaseViewModel = firebaseViewModel

        firebaseViewModel.readyLoginLiveData.observe(viewLifecycleOwner, Observer { value ->
            if (value) {
                binding.buttonLogin.visibility = View.VISIBLE
            }
        })

        binding.buttonLogin.setOnClickListener {
            firebaseViewModel.authenticateUser()
        }

        binding.buttonCreateAccount.setOnClickListener {
            findNavController().navigate(R.id.action_LoginFragment_to_createUserFragment)
        }

        firebaseViewModel.loggedInUser.observe(viewLifecycleOwner, Observer { loggedIn ->
            if (loggedIn == true) {
                Log.i(TAG, "logged in user")
                findNavController().navigate(R.id.action_LoginFragment_to_MainFragment)

            } else if (loggedIn != null && loggedIn == false){
                Log.i(TAG, "failed to login user")
                firebaseViewModel.resetLoginState()

            }
            // if loggedIn == null, that's the case when user just see the login page without
            // doing anything, so we'll do nothing because nothing is wrong.
        })

        firebaseViewModel.loginError.observe(viewLifecycleOwner, Observer { error ->
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