package com.bitpunchlab.android.shareroutes

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
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
            Log.i("registering user? ", loginViewModel.registerUserLiveData.value.toString())
            loginViewModel.createNewUser()
        }

        loginViewModel.registerUserLiveData.observe(viewLifecycleOwner, Observer { value ->
            value?.let {
                Log.i("ready to register user: ", value.toString())
                binding.buttonSend.visibility = View.VISIBLE
            }
        })

        loginViewModel.registeredUser.observe(viewLifecycleOwner, Observer { registered ->
            if (registered) {
                Log.i(TAG, "navigate to main fragment")
                findNavController().navigate(R.id.action_createUserFragment_to_MainFragment)
            } else {
                Log.i(TAG, "alert user failure")

            }
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun registrationFailureAlert() {
        val registrationAlert = AlertDialog.Builder(context)

        registrationAlert.setTitle(getString(R.string.registration_failure_alert_title))
        registrationAlert.setMessage(getString(R.string.registration_failure_alert_desc))
        registrationAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener() { dialog, button ->

            })

        registrationAlert.show()
    }
}