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
import com.bitpunchlab.android.shareroutes.databinding.FragmentCreateUserBinding

private const val TAG = "CreateUserFragment"

class CreateUserFragment : Fragment() {

    private var _binding : FragmentCreateUserBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseViewModel: FirebaseClientViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCreateUserBinding.inflate(inflater, container, false)
        firebaseViewModel = ViewModelProvider(requireActivity(), FirebaseClientViewModelFactory(requireActivity()))
            .get(FirebaseClientViewModel::class.java)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.firebaseViewModel = firebaseViewModel

        binding.buttonSend.setOnClickListener {
            // check all the fields are not empty
            // need to confirm passwords are the same and with some length
            Log.i("registering user? ", firebaseViewModel.registerUserLiveData.value.toString())
            firebaseViewModel.checkIfEmailUsed(firebaseViewModel.userEmail.value!!)
            //loginViewModel.registerNewUser()
        }

        firebaseViewModel.registerUserLiveData.observe(viewLifecycleOwner, Observer { value ->
            value?.let {
                Log.i("ready to register user: ", value.toString())
                binding.buttonSend.visibility = View.VISIBLE
            }
        })

        firebaseViewModel.loggedInUser.observe(viewLifecycleOwner, Observer { loggedIn ->
            if (loggedIn == true) {
                Log.i(TAG, "navigate to main fragment")
                //loginViewModel.resetLoginState()
                findNavController().navigate(R.id.action_createUserFragment_to_MainFragment)
            } else if (loggedIn != null && loggedIn == false) {
                Log.i(TAG, "alert user failure")
                firebaseViewModel.resetLoginState()
                registrationFailureAlert()
            }
            // when it is null, do nothing
        })

        firebaseViewModel.verifyEmailError.observe(viewLifecycleOwner, Observer { error ->
            if (error == true) {
                // show alert of error
                emailExistsAlert()
                firebaseViewModel.resetLoginState()
            } else if (error == false){
                firebaseViewModel.registerNewUser()
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

        registrationAlert.setCancelable(false)
        registrationAlert.setTitle(getString(R.string.registration_failure_alert_title))
        registrationAlert.setMessage(getString(R.string.registration_failure_alert_desc))
        registrationAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener() { dialog, button ->

            })

        registrationAlert.show()
    }

    private fun emailExistsAlert() {
        val emailAlert = AlertDialog.Builder(context)

        emailAlert.setCancelable(false)
        emailAlert.setTitle(getString(R.string.email_exists_alert_title))
        emailAlert.setMessage(getString(R.string.email_exists_alert_desc))
        emailAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener() { dialog, button ->

            })

        emailAlert.show()
    }
}