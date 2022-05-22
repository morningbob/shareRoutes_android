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
    val userInfo = UserInfo()

    val enableRegistration: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        //addSource(userInfo.email, userInfo.name, userInfo.password)
    }

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.i(TAG, "already logged in")
            isLoggedIn.value = true
        } else {
            Log.i(TAG, "not logged in")
            isLoggedIn.value = false
        }
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

    fun isEmailValid() : Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(userInfo.email).matches()
    }

    fun isPasswordValid() : Boolean {
        val passwordPattern = Pattern.compile("^[A-Za-z0-9]{8,20}$")
        return passwordPattern.matcher(userInfo.password).matches()
    }

    fun isConfirmPasswordValid() : Boolean {
        return userInfo.password == userInfo.confirmPassword
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

