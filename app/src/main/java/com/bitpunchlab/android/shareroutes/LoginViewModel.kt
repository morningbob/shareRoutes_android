package com.bitpunchlab.android.shareroutes

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.util.Patterns
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.util.regex.Pattern

private const val TAG = "LoginViewModel"

class LoginViewModel(@SuppressLint("StaticFieldLeak") val activity: Activity) : ViewModel() {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    var isLoggedIn = MutableLiveData<Boolean>(false)
    var registeredUser = MutableLiveData<Boolean>(false)
    var readyLogin = MutableLiveData<Boolean>(false)
    var currentUser : FirebaseUser? = null
    val userName = MutableLiveData<String>("")
    val userEmail = MutableLiveData<String>("")
    val userPassword = MutableLiveData<String>("")
    val userConfirmPassword = MutableLiveData<String>("")
    val nameError = MutableLiveData<String>("")
    val emailError = MutableLiveData<String>("")
    val passwordError = MutableLiveData<String>("")
    val confirmPasswordError = MutableLiveData<String>("")

    private val nameValid: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(userName) { name ->
            if (name.isNullOrEmpty()) {
                nameError.value = "Name must not be empty."
                value = false
            } else {
                value = true
                nameError.value = ""
            }
        }
    }

    private val emailValid: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(userEmail) { email ->
            if (!email.isNullOrEmpty()) {
                if (!isEmailValid(email)) {

                    emailError.value = "Please enter a valid email."
                    value = false

                } else {
                    value = true
                    emailError.value = ""
                }
            } else {
                value = false
            }
            //value =
            Log.i("email valid? ", value.toString())
        }
    }

    private val passwordValid: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(userPassword) { password ->
            if (!password.isNullOrEmpty()) {
                if (isPasswordContainSpace(password)) {
                    passwordError.value = "Password cannot has space."
                    value = false
                } else if (password.count() < 8) {
                    passwordError.value = "Password should be at least 8 characters."
                    value = false
                } else if (!isPasswordValid(password)) {
                    passwordError.value = "Password can only be composed of letters and numbers."
                    value = false
                } else {
                    passwordError.value = ""
                    value = true
                }
            } else {
                value = false
            }
            Log.i("password valid? ", value.toString())
        }
    }

    private val confirmPasswordValid: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(userConfirmPassword) { confirmPassword ->
            if (!confirmPassword.isNullOrEmpty()) {
                if (!isConfirmPasswordValid(userPassword.value!!, confirmPassword)) {
                    confirmPasswordError.value = "Passwords must be the same."
                    value = false
                } else {
                    confirmPasswordError.value = ""
                    value = true
                }
            } else {
                value = false
            }
            Log.i("confirm valid? ", value.toString())
        }
    }

    var registerUserLiveData = MediatorLiveData<Boolean>()
    var readyLoginLiveData = MediatorLiveData<Boolean>()

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.i(TAG, "already logged in")
            isLoggedIn.value = true
        } else {
            Log.i(TAG, "not logged in")
            isLoggedIn.value = false
        }
        registerUserLiveData.addSource(nameValid) { valid ->
            // we only apply that isEnableRegistration test if the name is valid
            if (valid) {
                registerUserLiveData.value = isEnableRegistration()
            } else {
                registerUserLiveData.value = false
            }
        }
        registerUserLiveData.addSource(emailValid) { valid ->
            if (valid) {
                registerUserLiveData.value = isEnableRegistration()
            } else {
                registerUserLiveData.value = false
            }
        }
        registerUserLiveData.addSource(passwordValid) { valid ->
            if (valid) {
                registerUserLiveData.value = isEnableRegistration()
            } else {
                registerUserLiveData.value = false
            }
        }
        registerUserLiveData.addSource(confirmPasswordValid) { valid ->
            if (valid) {
                registerUserLiveData.value = isEnableRegistration()
            } else {
                registerUserLiveData.value = false
            }
        }
        readyLoginLiveData.addSource(emailValid) { valid ->
            if (valid) {
                readyLoginLiveData.value = isReadyLogin()
            } else {
                readyLoginLiveData.value = false
            }
        }
        readyLoginLiveData.addSource(passwordValid) { valid ->
            if (valid) {
                readyLoginLiveData.value = isReadyLogin()
            } else {
                readyLoginLiveData.value = false
            }
        }
    }

    fun createNewUser() {
        auth.createUserWithEmailAndPassword(userEmail.value!!, userPassword.value!!)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "successfully created user")
                    registeredUser.postValue(true)
                    currentUser = auth.currentUser
                } else {
                    Log.i(TAG, "error in creating user")
                }
            }
    }

    fun authenticateUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "successfully logged in user")
                    currentUser = auth.currentUser
                } else {
                    Log.i(TAG, "error logging in user")
                }
            }
    }

    fun logoutUser() {
        auth.signOut()
    }

    private fun isEmailValid(email: String) : Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String) : Boolean {
        val passwordPattern = Pattern.compile("^[A-Za-z0-9]{8,20}$")
        return passwordPattern.matcher(password).matches()
    }

    private fun isPasswordContainSpace(password: String) : Boolean {
        return password.contains(" ")
    }

    private fun isConfirmPasswordValid(password: String, confirmPassword: String) : Boolean {
        //Log.i("confirming password: ", "password: $password, confirm: $confirmPassword")
        return password == confirmPassword
    }

    private fun isEnableRegistration() : Boolean {
        return (emailValid.value!! && passwordValid.value!! && confirmPasswordValid.value!!)
    }

    private fun isReadyLogin() : Boolean {
        return (emailValid.value!! && passwordValid.value!!)
    }
}


class LoginViewModelFactory(private val activity: Activity)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(activity) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

