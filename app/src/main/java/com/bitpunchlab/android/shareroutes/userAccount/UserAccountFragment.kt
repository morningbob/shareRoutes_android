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
import com.bitpunchlab.android.shareroutes.models.User


class UserAccountFragment : Fragment() {

    private var _binding : FragmentUserAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseViewModel: FirebaseClientViewModel
    private var userNewEmail: String = ""
    private var userCurrentPassword: String = ""
    private var userNewPassword: String = ""

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

        firebaseViewModel.userAccountState.value = UserAccountState.NORMAL

        firebaseViewModel.emailValid.observe(viewLifecycleOwner, Observer { valid ->
            if (valid) {
                Log.i("user account", "email is valid")
                binding.updateEmailButton.visibility = View.VISIBLE
            }
        })

        firebaseViewModel.readyUpdatePasswordLiveData.observe(viewLifecycleOwner, Observer { ready ->
            if (ready) {
                Log.i("user account", "password is valid")
                binding.updatePasswordButton.visibility = View.VISIBLE
            }
        })

        binding.updateEmailButton.setOnClickListener {
            firebaseViewModel.userAccountState.value = UserAccountState.UPDATE_EMAIL
        }

        binding.updatePasswordButton.setOnClickListener {
            firebaseViewModel.userAccountState.value = UserAccountState.UPDATE_PASSWORD
        }

        firebaseViewModel.systemLogout.observe(viewLifecycleOwner, Observer { logout ->
            if (logout) {
                Log.i("systemLogout", "auth is null")
                // when I use the first navigation technique, the user account fragment
                // navigate to Login fragment no matter what condition systemLogout is
                // so, I use pop back stack.  The behaviour is alright.
                //findNavController().navigate(R.id.action_userAccountFragment_to_LoginFragment)
                findNavController().popBackStack()
            }
        })

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        Log.i("onResume", "reset app state")
        firebaseViewModel.resetAppState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observerUserAccountAppState() {
        firebaseViewModel.userAccountState.observe(viewLifecycleOwner, Observer { appState ->
            when (appState) {
                UserAccountState.NORMAL -> {
                    Log.i("observe state", "app state changed to normal")
                }
                UserAccountState.UPDATE_EMAIL -> {
                    userNewEmail = binding.edittextEmail.text.toString()
                    requirePasswordAlert(userNewEmail)
                }
                UserAccountState.UPDATE_PASSWORD -> {
                    userCurrentPassword = binding.edittextCurrentPassword.text.toString()
                    userNewPassword = binding.edittextPassword.text.toString()
                    Log.i("current password ${userCurrentPassword}", "new password ${userNewPassword}")
                    firebaseViewModel.updateUserPassword(newPassword = userNewPassword,
                        oldPassword = userCurrentPassword)
                }
                UserAccountState.UPDATE_EMAIL_DATA_ERROR -> {
                    updateEmailDataErrorAlert()
                    firebaseViewModel.resetAppState()
                }
                UserAccountState.UPDATE_EMAIL_SERVER_ERROR -> {
                    updateEmailServerErrorAlert()
                    firebaseViewModel.resetAppState()
                }
                UserAccountState.UPDATE_EMAIL_PASSWORD_ERROR -> {
                    updateEmailPasswordErrorAlert(userNewEmail)
                    firebaseViewModel.resetAppState()
                }

                UserAccountState.UPDATE_EMAIL_SUCCESS -> {
                    Log.i("observe app state", "update email success state triggered")
                    updateEmailSuccessAlert()
                    //firebaseViewModel.resetAppState()
                }
                UserAccountState.UPDATE_PASSWORD_DATA_ERROR -> {
                    updatePasswordDataErrorAlert()
                    firebaseViewModel.resetAppState()
                }
                UserAccountState.UPDATE_PASSWORD_SERVER_ERROR -> {
                    updatePasswordServerErrorAlert()
                    firebaseViewModel.resetAppState()
                }
                UserAccountState.UPDATE_PASSWORD_LOGIN_ERROR -> {
                    updatePasswordLoginAlert()
                    firebaseViewModel.resetAppState()
                }
                UserAccountState.UPDATE_PASSWORD_SUCCESS -> {
                    updatePasswordSuccessAlert()
                    //firebaseViewModel.resetAppState()
                }
                UserAccountState.LOGGED_OUT_USER -> {
                    // logout user and navigate to login page
                    firebaseViewModel.logoutUser()
                    Log.i("appState", "logged out user")

                    //findNavController().popBackStack()
                    firebaseViewModel.resetAppState()
                }
            }
        })
    }

    private fun requirePasswordAlert(newEmail: String) {
        val passwordAlert = AlertDialog.Builder(requireContext())
        val passwordEditText = EditText(context)

        passwordAlert.setCancelable(false)
        passwordAlert.setTitle(getString(R.string.require_password_alert_title))
        passwordAlert.setMessage(getString(R.string.require_password_alert_desc))
        passwordAlert.setView(passwordEditText)

        passwordAlert.setPositiveButton(getString(R.string.button_send),
            DialogInterface.OnClickListener { dialog, button ->
                val password = passwordEditText.text.toString()
                if (!password.isNullOrEmpty()) {
                    firebaseViewModel.updateUserEmail(newEmail, password)
                } else {
                    nullPasswordAlert()
                }

            })

        passwordAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
            })

        passwordAlert.show()
    }

    private fun nullPasswordAlert() {
        val passwordAlert = AlertDialog.Builder(requireContext())

        passwordAlert.setCancelable(false)
        passwordAlert.setTitle(getString(R.string.null_password_alert_title))
        passwordAlert.setMessage(getString(R.string.null_password_alert_desc))

        passwordAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
            })


        passwordAlert.show()
    }

    private fun updateEmailServerErrorAlert() {
        val serverAlert = AlertDialog.Builder(requireContext())

        serverAlert.setCancelable(false)
        serverAlert.setTitle(getString(R.string.update_email_server_error_alert_title))
        serverAlert.setMessage(getString(R.string.update_email_server_error_alert_desc))

        serverAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
            })

        serverAlert.show()
    }

    private fun updateEmailDataErrorAlert() {
        val dataAlert = AlertDialog.Builder(requireContext())

        dataAlert.setCancelable(false)
        dataAlert.setTitle(getString(R.string.update_email_data_error_alert_title))
        dataAlert.setMessage(getString(R.string.update_email_data_error_alert_desc))

        dataAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
            })

        dataAlert.show()
    }

    private fun updateEmailPasswordErrorAlert(newEmail: String) {
        val passwordAlert = AlertDialog.Builder(requireContext())
        val passwordEditText = EditText(context)

        passwordAlert.setCancelable(false)
        passwordAlert.setTitle(getString(R.string.update_email_password_error_alert_title))
        passwordAlert.setMessage(getString(R.string.update_email_password_error_alert_desc))
        passwordAlert.setView(passwordEditText)

        passwordAlert.setPositiveButton(getString(R.string.button_send),
            DialogInterface.OnClickListener { dialog, button ->
                // try to update again
                val password = passwordEditText.text.toString()
                if (!password.isNullOrEmpty()) {
                    firebaseViewModel.updateUserEmail(newEmail, password)
                } else {
                    nullPasswordAlert()
                }
            })

        passwordAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
            })

        passwordAlert.show()
    }

    private fun updateEmailSuccessAlert() {
        val successAlert = AlertDialog.Builder(requireContext())

        successAlert.setCancelable(false)
        successAlert.setTitle(getString(R.string.update_email_success_alert_title))
        successAlert.setMessage(getString(R.string.update_email_success_alert_desc))

        successAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                firebaseViewModel.userAccountState.value = UserAccountState.LOGGED_OUT_USER
            })

        successAlert.show()
    }

    private fun updatePasswordDataErrorAlert() {
        val dataAlert = AlertDialog.Builder(requireContext())

        dataAlert.setCancelable(false)
        dataAlert.setTitle(getString(R.string.update_password_data_error_alert_title))
        dataAlert.setMessage(getString(R.string.update_password_data_error_alert_desc))

        dataAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
            })

        dataAlert.show()
    }

    private fun updatePasswordServerErrorAlert() {
        val serverAlert = AlertDialog.Builder(requireContext())

        serverAlert.setCancelable(false)
        serverAlert.setTitle(getString(R.string.update_password_server_error_alert_title))
        serverAlert.setMessage(getString(R.string.update_password_server_error_alert_desc))

        serverAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
            })

        serverAlert.show()
    }

    private fun updatePasswordLoginAlert() {
        val loginAlert = AlertDialog.Builder(requireContext())
        val passwordEditText = EditText(context)

        loginAlert.setCancelable(false)
        loginAlert.setTitle(getString(R.string.update_password_login_alert_title))
        loginAlert.setMessage(getString(R.string.update_password_login_alert_desc))
        loginAlert.setView(passwordEditText)

        loginAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
                // try to update again
                val password = passwordEditText.text.toString()
                if (!password.isNullOrEmpty()) {
                    firebaseViewModel.updateUserPassword(newPassword = userNewPassword,
                        oldPassword = password)
                } else {
                    nullPasswordAlert()
                }
            })

        loginAlert.show()
    }

    private fun updatePasswordSuccessAlert() {
        val successAlert = AlertDialog.Builder(requireContext())

        successAlert.setCancelable(false)
        successAlert.setTitle(getString(R.string.update_password_success_alert_title))
        successAlert.setMessage(getString(R.string.update_password_success_alert_desc))

        successAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener { dialog, button ->
                // do nothing
                firebaseViewModel.userAccountState.value = UserAccountState.LOGGED_OUT_USER
                dialog.dismiss()
            })

        successAlert.show()
    }

}