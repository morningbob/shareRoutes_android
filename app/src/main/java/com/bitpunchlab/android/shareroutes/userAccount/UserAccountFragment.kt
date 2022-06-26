package com.bitpunchlab.android.shareroutes.userAccount

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.shareroutes.FirebaseClientViewModel
import com.bitpunchlab.android.shareroutes.FirebaseClientViewModelFactory
import com.bitpunchlab.android.shareroutes.R
import com.bitpunchlab.android.shareroutes.UserAccountState
import com.bitpunchlab.android.shareroutes.databinding.FragmentUserAccountBinding


class UserAccountFragment : Fragment() {

    private var _binding : FragmentUserAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseViewModel: FirebaseClientViewModel
    private var userNewEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserAccountBinding.inflate(layoutInflater, container, false)
        firebaseViewModel = ViewModelProvider(requireActivity(), FirebaseClientViewModelFactory(requireActivity()))
            .get(FirebaseClientViewModel::class.java)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.firebaseViewModel = firebaseViewModel

        binding.user = firebaseViewModel.userObject.value

        observerUserAccountAppState()

        firebaseViewModel.emailValid.observe(viewLifecycleOwner, Observer { valid ->
            if (valid) {
                Log.i("user account", "about to update email")
                binding.updateEmailButton.visibility = View.VISIBLE
            }
        })

        firebaseViewModel.readyUpdatePasswordLiveData.observe(viewLifecycleOwner, Observer { ready ->
            if (ready) {
                Log.i("user account", "about to update password")
                binding.updatePasswordButton.visibility = View.VISIBLE
            }
        })

        binding.updateEmailButton.setOnClickListener {
            userNewEmail = binding.edittextEmail.text.toString()
            requirePasswordAlert(userNewEmail)
        }

        binding.updatePasswordButton.setOnClickListener {
            firebaseViewModel.updateUserPassword(binding.edittextPassword.text.toString(),
                binding.edittextCurrentPassword.text.toString())
        }



        /*
        firebaseViewModel.updateUserEmailServerError.observe(viewLifecycleOwner, Observer { error ->
            if (error) {
                updateEmailServerErrorAlert()
            }
        })

        firebaseViewModel.updateUserEmailDataError.observe(viewLifecycleOwner, Observer { error ->
            if (error) {
                updateEmailDataErrorAlert()
            }
        })

        firebaseViewModel.updateUserEmailPasswordError.observe(viewLifecycleOwner, Observer { error ->
            if (error) {
                updateEmailPasswordErrorAlert(binding.edittextEmail.text.toString())
            }
        })

        firebaseViewModel.updateEmailSuccess.observe(viewLifecycleOwner, Observer { success ->
            if (success) {
                updateEmailSuccessAlert()
            }
        })

        firebaseViewModel.updatePasswordDataError.observe(viewLifecycleOwner, Observer { error ->
            if (error) {
                updatePasswordDataErrorAlert()
            }
        })

        firebaseViewModel.updatePasswordServerError.observe(viewLifecycleOwner, Observer { error ->
            if (error) {
                updatePasswordServerErrorAlert()
            }
        })

        firebaseViewModel.updatePasswordSuccess.observe(viewLifecycleOwner, Observer { success ->
            if (success) {
                updatePasswordSuccessAlert()
            }
        })

         */

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // clear all update errors here
        //firebaseViewModel.updateUserEmailDataError.value = false
        //firebaseViewModel.updateUserEmailServerError.value = false
        //firebaseViewModel.updateUserEmailPasswordError.value = false
        //firebaseViewModel.updateEmailSuccess.value = false

        //firebaseViewModel.updatePasswordDataError.value = false
        //firebaseViewModel.updatePasswordServerError.value = false
        //firebaseViewModel.updatePasswordSuccess.value = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observerUserAccountAppState() {
        firebaseViewModel.userAccountState.observe(viewLifecycleOwner, Observer { appState ->
            when (appState) {
                UserAccountState.UPDATE_EMAIL_DATA_ERROR -> updateEmailDataErrorAlert()
                UserAccountState.UPDATE_EMAIL_SERVER_ERROR -> updateEmailServerErrorAlert()
                UserAccountState.UPDATE_EMAIL_PASSWORD_ERROR -> updateEmailPasswordErrorAlert(userNewEmail)
                UserAccountState.UPDATE_PASSWORD_DATA_ERROR -> updatePasswordDataErrorAlert()
                UserAccountState.UPDATE_PASSWORD_SERVER_ERROR -> updatePasswordServerErrorAlert()
                UserAccountState.UPDATE_PASSWORD_LOGIN_ERROR -> upd
            }
        })
    }

    private fun requirePasswordAlert(newEmail: String) {
        val passwordAlert = AlertDialog.Builder(requireContext())
        val passwordEditText = EditText(context)

        passwordAlert.setCancelable(false)
        passwordAlert.setTitle("Password Confirmation")
        passwordAlert.setMessage("Password is required to change the email.  We need to do re-authentication.  Thanks.")
        passwordAlert.setView(passwordEditText)

        passwordAlert.setPositiveButton(getString(R.string.button_send),
            DialogInterface.OnClickListener { dialog, button ->
                val password = passwordEditText.text.toString()
                firebaseViewModel.updateUserEmail(newEmail, password)

            })

        passwordAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
            })

        passwordAlert.show()
    }

    private fun updateEmailServerErrorAlert() {
        val serverAlert = AlertDialog.Builder(requireContext())

        serverAlert.setCancelable(false)
        serverAlert.setTitle("Update Email Error")
        serverAlert.setMessage("There is error in the server when updating the email.  Please try again later.")

        serverAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
                firebaseViewModel.updateUserEmailServerError.value = false
            })

        serverAlert.show()
    }

    private fun updateEmailDataErrorAlert() {
        val dataAlert = AlertDialog.Builder(requireContext())

        dataAlert.setCancelable(false)
        dataAlert.setTitle("Update Email Error")
        dataAlert.setMessage("There is error when getting user's information.  Please logout and login again and try.")

        dataAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
                // reset error
                firebaseViewModel.updateUserEmailDataError.value = false
            })

        dataAlert.show()
    }

    private fun updateEmailPasswordErrorAlert(newEmail: String) {
        val passwordAlert = AlertDialog.Builder(requireContext())
        val passwordEditText = EditText(context)

        passwordAlert.setCancelable(false)
        passwordAlert.setTitle("Update Email Error")
        passwordAlert.setMessage("There is error when logging in user's account to change the email.  Please make sure the password is correct.  Please enter the password again.")
        passwordAlert.setView(passwordEditText)

        passwordAlert.setPositiveButton(getString(R.string.button_send),
            DialogInterface.OnClickListener { dialog, button ->
                firebaseViewModel.updateUserEmailPasswordError.value = false
                // try to update again
                val password = passwordEditText.text.toString()
                firebaseViewModel.updateUserEmail(newEmail, password)
            })

        passwordAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener { dialog, button ->
                firebaseViewModel.updateUserEmailPasswordError.value = false
                // do nothing
            })

        passwordAlert.show()
    }

    private fun updateEmailSuccessAlert() {
        val successAlert = AlertDialog.Builder(requireContext())

        successAlert.setCancelable(false)
        successAlert.setTitle("Updated Email")
        successAlert.setMessage("The email has been updated successfully.  But since the email is the login key, you need to login again now.  Please use the new email to login.")

        successAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // reset success
                firebaseViewModel.updateEmailSuccess.value = false
                // logout user and navigate to login page
                firebaseViewModel.logoutUser()
                // instead, use auth == null
                findNavController().navigate(R.id.action_userAccountFragment_to_LoginFragment)
            })

        successAlert.show()
    }

    private fun updatePasswordDataErrorAlert() {
        val dataAlert = AlertDialog.Builder(requireContext())

        dataAlert.setCancelable(false)
        dataAlert.setTitle("Update Password Error")
        dataAlert.setMessage("There is error when getting user's information.  Please logout and login again and try.")

        dataAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
                // reset error
                firebaseViewModel.updatePasswordDataError.value = false
            })

        dataAlert.show()
    }

    private fun updatePasswordServerErrorAlert() {
        val serverAlert = AlertDialog.Builder(requireContext())

        serverAlert.setCancelable(false)
        serverAlert.setTitle("Update Password Error")
        serverAlert.setMessage("There is error in the server when updating the password.  Please try again later.")

        serverAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
                // reset error
                firebaseViewModel.updatePasswordServerError.value = false
            })

        serverAlert.show()
    }

    private fun updatePasswordLoginAlert() {
        val loginAlert = AlertDialog.Builder(requireContext())
        val passwordEditText = EditText(context)

        loginAlert.setCancelable(false)
        loginAlert.setTitle("Update Password Error")
        loginAlert.setMessage("There is error when logging in user's account to change the password.  Please make sure the current password is correct.  Please enter the current password again.")
        loginAlert.setView(passwordEditText)

        loginAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
                // reset error
                //firebaseViewModel.updatePasswordServerError.value = false
                // try to update again
                val currentPassword = passwordEditText.text.toString()
                firebaseViewModel.updateUserPassword(, currentPassword)
            })

        loginAlert.show()
    }

    private fun updatePasswordSuccessAlert() {
        val successAlert = AlertDialog.Builder(requireContext())

        successAlert.setCancelable(false)
        successAlert.setTitle("Updated Password")
        successAlert.setMessage("The password has been updated successfully.")

        successAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
                // reset success
                firebaseViewModel.updatePasswordSuccess.value = false
            })

        successAlert.show()
    }

}