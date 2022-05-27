package com.bitpunchlab.android.shareroutes

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.util.Patterns
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.lifecycle.*
import com.bitpunchlab.android.shareroutes.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.regex.Pattern

private const val TAG = "LoginViewModel"

class LoginViewModel(@SuppressLint("StaticFieldLeak") val activity: Activity) : ViewModel() {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    var isLoggedIn = MutableLiveData<Boolean>(false)
    var loggedInUser = MutableLiveData<Boolean>()
    var loggedOutUser = MutableLiveData<Boolean>(false)
    //var currentUser = MutableLiveData<FirebaseUser>()
    val userName = MutableLiveData<String>("")
    val userEmail = MutableLiveData<String>("")
    val userPassword = MutableLiveData<String>("")
    val userConfirmPassword = MutableLiveData<String>("")
    val nameError = MutableLiveData<String>("")
    val emailError = MutableLiveData<String>("")
    val passwordError = MutableLiveData<String>("")
    val confirmPasswordError = MutableLiveData<String>("")
    var user : User? = null
    private var database : DatabaseReference = Firebase.database.reference
    var loginError = MutableLiveData<Boolean>(false)
    val verifyEmailError = MutableLiveData<Boolean>()

    private var authStateListener = FirebaseAuth.AuthStateListener { auth ->
        if (auth.currentUser == null) {
            Log.i(TAG, "logout out successfully")
            loggedOutUser.postValue(true)
        }
        // may post true value to loggedInUser here
    }

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
            //value = false
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
            //value = false
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
            //value
        }
    }

    var registerUserLiveData = MediatorLiveData<Boolean>()
    var readyLoginLiveData = MediatorLiveData<Boolean>()

    init {
        auth.addAuthStateListener(authStateListener)

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

    fun registerNewUser() {
        auth.createUserWithEmailAndPassword(userEmail.value!!, userPassword.value!!)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "successfully created user")
                    loggedInUser.postValue(true)
                    //currentUser.postValue(auth.currentUser)
                } else {
                    Log.i(TAG, "error in creating user")
                }
            }
        saveUser(userName.value!!, userEmail.value!!, userPassword.value!!)
    }

    fun authenticateUser() {
        auth.signInWithEmailAndPassword(userEmail.value!!, userPassword.value!!)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "successfully logged in user")
                    //currentUser.postValue(auth.currentUser)
                    loggedInUser.postValue(true)
                } else {
                    Log.i(TAG, "error logging in user")
                    loginError.postValue(true)
                }
            }
    }

    private fun saveUser(name: String, email: String, password: String) {
        user = createUser(name, email, password)
        saveUserInDatabase(user!!)
    }

    private fun createUser(name: String, email: String, password: String) : User {
        return User(userName = name, userEmail = email, userPassword = password)
    }

    private fun saveUserInDatabase(user: User)  {
        //var result = false
        database.child("users").child(user.userID).setValue(user, DatabaseReference.CompletionListener() {
            error: DatabaseError?, ref: DatabaseReference ->
            if (error != null) {
                Log.i(TAG, "there is error writing to database: ${error.message}")

            } else {
                //result = true
                Log.i(TAG, "successfully saved user")
            }
        })
        // we also update the emails, that list all the emails of the user for quick reference
        val keyID = database.push().key
        keyID?.let { key ->
            database.child("emails").child(key).setValue(user.userEmail)
        }
        //return result
    }

    private var valueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            // post value true or false for found or not found
            // that ends the verification, and proceed register or alert failure
            Log.i("snapshot: ", snapshot.value.toString())
            if (snapshot.children.count() > 0) {
                verifyEmailError.postValue(true)
            } else {
                verifyEmailError.postValue(false)
            }
            for (user in snapshot.children) {
                Log.i("user email: ", user.child("userEmail").toString())
                //verifyEmailError.postValue(true)
            }

        }

        override fun onCancelled(error: DatabaseError) {
            Log.i("error: ", error.message)
        }

    }

    fun checkIfEmailUsed(email: String) {
        //val allEmails = database.child("emails")
        //database.orderByKey()
        //allEmails.addValueEventListener()
        database
            .child("users")
            .orderByChild("userEmail")
            .equalTo(email)
            .addListenerForSingleValueEvent(valueEventListener)
    }

    fun logoutUser() {
        resetLoginState()
        auth.signOut()
    }

    fun resetLoginState() {
        userName.value = ""
        userEmail.value = ""
        userPassword.value = ""
        userConfirmPassword.value = ""
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
        //return (emailValid.value!! && passwordValid.value!! && confirmPasswordValid.value!!)
        //var result = false
        // this is to prevent crash if the values are null at the beginning
        return (emailValid.value != null || passwordValid.value != null
                || confirmPasswordValid.value != null) &&
                (emailValid.value!! && passwordValid.value!! && confirmPasswordValid.value!!)


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

