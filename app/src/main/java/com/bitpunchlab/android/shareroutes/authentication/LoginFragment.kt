package com.bitpunchlab.android.shareroutes.authentication

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
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

        binding.forgotPasswordTextview!!.setOnClickListener {
            forgotPasswordAlert()
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

        firebaseViewModel.resetPasswordSuccess.observe(viewLifecycleOwner, Observer { success ->
            if (success == true) {
                resetPasswordSuccessAlert()
            } else if (success == false) {
                resetPasswordFailureAlert()
            }
            // if success == null, don't do anything
        })

        return binding.root

    }

    override fun onResume() {
        super.onResume()
        // clear all errors here
        firebaseViewModel.loginError.value = false
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

    private fun forgotPasswordAlert() {
        val forgotAlert = AlertDialog.Builder(requireContext())
        val emailEditText = EditText(requireContext())

        forgotAlert.setCancelable(false)
        forgotAlert.setTitle(getString(R.string.forgot_password_alert_title))
        forgotAlert.setMessage(getString(R.string.forget_password_alert_desc))
        forgotAlert.setView(emailEditText)

        forgotAlert.setPositiveButton(getString(R.string.button_send),
            DialogInterface.OnClickListener { dialog, button ->
                val email = emailEditText.text.toString()
                if (!email.isNullOrEmpty()) {
                    // send the reset email
                    firebaseViewModel.sendPasswordResetEmail(email)
                } else {
                    // alert empty email
                    emailEmptyAlert()
                }
            })

        forgotAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
            })

        forgotAlert.show()
    }

    private fun emailEmptyAlert() {
        val emailAlert = AlertDialog.Builder(requireContext())

        emailAlert.setTitle(getString(R.string.empty_email_alert_title))
        emailAlert.setMessage(getString(R.string.empty_email_alert_desc))

        emailAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->

            })

        emailAlert.show()
    }

    private fun resetPasswordSuccessAlert() {
        val resetAlert = AlertDialog.Builder(requireContext())

        resetAlert.setTitle(getString(R.string.reset_password_success_alert_title))
        resetAlert.setMessage(getString(R.string.reset_password_success_alert_desc))

        resetAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
            })

        resetAlert.show()
    }

    private fun resetPasswordFailureAlert() {
        val resetAlert = AlertDialog.Builder(requireContext())

        resetAlert.setTitle(getString(R.string.reset_password_failure_alert_title))
        resetAlert.setMessage(getString(R.string.reset_password_failure_alert_desc))

        resetAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
            })

        resetAlert.show()
    }
}