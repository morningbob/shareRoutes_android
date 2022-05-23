package com.bitpunchlab.android.shareroutes

import android.app.Activity
import android.util.Log
import android.util.Patterns
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import java.util.regex.Pattern

private const val TAG = "LoginViewModel"

class LoginViewModel(val activity: Activity) : ViewModel() {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    var isLoggedIn = MutableLiveData<Boolean>(false)
    val username = MutableLiveData<String>("")
    val userEmail = MutableLiveData<String>("")
    val userPassword = MutableLiveData<String>("")
    val userConfirmPassword = MutableLiveData<String>("")

    val emailValid: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(userEmail) { email ->
            value = isEmailValid(email)
            Log.i("email valid? ", value.toString())
        }
    }

    val passwordValid: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(userPassword) { password ->
            value = isPasswordValid(password)
            Log.i("password valid? ", value.toString())
        }
    }

    val confirmPasswordValid: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(userPassword) {
            value = isConfirmPasswordValid(userPassword.value!!, userConfirmPassword.value!!)
            Log.i("confirm valid? ", value.toString())
        }
    }

    private var liveDataMerger = MediatorLiveData<Boolean>()


    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.i(TAG, "already logged in")
            isLoggedIn.value = true
        } else {
            Log.i(TAG, "not logged in")
            isLoggedIn.value = false
        }
        //liveDataMerger.addSource(userPassword)
    }

    fun createNewUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "successfully created user")
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
                } else {
                    Log.i(TAG, "error logging in user")
                }
            }
    }

    private fun isEmailValid(email: String) : Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String) : Boolean {
        val passwordPattern = Pattern.compile("^[A-Za-z0-9]{8,20}$")
        return passwordPattern.matcher(password).matches()
    }

    fun isConfirmPasswordValid(password: String, confirmPassword: String) : Boolean {
        return password == confirmPassword
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

