package com.bitpunchlab.android.shareroutes

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth

private const val TAG = "LoginViewModel"

class LoginViewModel(val activity: Activity) : ViewModel() {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()

    var isLoggedIn = MutableLiveData<Boolean>(false)

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

