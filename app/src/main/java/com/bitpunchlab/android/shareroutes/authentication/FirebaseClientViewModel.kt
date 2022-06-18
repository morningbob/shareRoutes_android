package com.bitpunchlab.android.shareroutes

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.util.Patterns
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.lifecycle.*
import com.bitpunchlab.android.shareroutes.models.Route
import com.bitpunchlab.android.shareroutes.models.User
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern


private const val TAG = "LoginViewModel"

class FirebaseClientViewModel(@SuppressLint("StaticFieldLeak") val activity: Activity) : ViewModel() {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    // I define the loggedInUser as nullable that null represents the original non-login state
    // when the user first visits the page.  That the user is just not logged in yet.  Not failed to
    // login.  So, it is null, not false.
    // so, whenever it is false, it only means there is error logging in (maybe on the server side)
    // when it is null, don't trigger error.
    // I use loggedInUser to decide whether to navigate to main fragment or not
    // but beware that I also need the user object to save routes in the database
    // I actually need to wait for the user object to be ready too
    var loggedInUser = MutableLiveData<Boolean?>()
    //var loggedOutUser = MutableLiveData<Boolean>(false)
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
    // this is the same technique as loggedInUser, I need 3 states,
    // null: when user is trying to create an account
    // true: email already exists in database
    // false: email doesn't exists in database, it is after checking
    val verifyEmailError = MutableLiveData<Boolean?>()
    // we will get the user from the database if we successfully authenticate the user
    var userObject = MutableLiveData<User>()

    var _routeToShare = MutableLiveData<Route>()
    val routeToShare get() = _routeToShare

    var _routesResult = MutableLiveData<List<Route>>()
    val routesResult get() = _routesResult

    var _shareSuccess = MutableLiveData<Boolean>(false)
    val shareSuccess get() = _shareSuccess



    private var authStateListener = FirebaseAuth.AuthStateListener { auth ->
        // this is when user is in the logout process, it is not completed yet,
        // so, the user is still logged in.
        // this is to distinguish from when the user didn't login yet, but is not logging out
        if (loggedInUser.value != null && auth.currentUser == null) {
            Log.i(TAG, "logout out successfully")
            loggedInUser.postValue(null)
        } else if (auth.currentUser == null) {
            Log.i("auth listener", "auth state is changed to null")
        } else if (auth.currentUser != null) {
            Log.i("auth listener", "auth is not null")
            userEmail.value = auth.currentUser!!.email
            // this is the case when user logged in before, and close and then start the app
            // again, it should be logged in
            retrieveUserObject()
            loggedInUser.postValue(true)

            // need to get the user from the database for reference
            // need to use user's routesCreated to save new route in database
        }
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
        // this part is to register user in the Firebase Auth
        auth.createUserWithEmailAndPassword(userEmail.value!!, userPassword.value!!)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "successfully created user")
                    loggedInUser.postValue(true)
                    // this part is to register user in my database
                    saveUser(userName.value!!, userEmail.value!!, userPassword.value!!)
                    //currentUser.postValue(auth.currentUser)
                } else {
                    Log.i(TAG, "error in creating user")
                    // alert user system problem
                }
            }

    }

    // when we log in user, we also get the user object from the database as a
    // reference to quickly access properties
    fun authenticateUser() {
        auth.signInWithEmailAndPassword(userEmail.value!!, userPassword.value!!)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "successfully logged in user")
                    //retrieveUserObject()
                    loggedInUser.postValue(true)
                } else {
                    Log.i(TAG, "error logging in user")
                    loginError.postValue(true)
                }
            }
    }

    private fun retrieveUserObject() {
        Log.i("retrieving user, email", userEmail.value!!)
        database
            .child("users")
            .orderByChild("userEmail")
            .equalTo(userEmail.value)
            .addListenerForSingleValueEvent(userValueEventListener)
    }

    private fun saveUser(name: String, email: String, password: String) {
        user = createUser(name, email, password)
        saveUserInDatabase(user!!)

    }

    private fun createUser(name: String, email: String, password: String) : User {
        return User(name = name, email = email, password = password,
            routes = HashMap<String, Route>())
    }

    private fun saveUserInDatabase(user: User)  {
        database.child("users").child(user.userID).setValue(user, DatabaseReference.CompletionListener() {
            error: DatabaseError?, ref: DatabaseReference ->
            if (error != null) {
                Log.i(TAG, "there is error writing to database: ${error.message}")

            } else {
                Log.i(TAG, "successfully saved user")
            }
        })
        // we also update the emails, that list all the emails of the user for quick reference
        val keyID = database.push().key
        keyID?.let { key ->
            database.child("emails").child(key).setValue(user.userEmail)
        }
    }

    private var emailValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            // post value true or false for found or not found
            // that ends the verification, and proceed register or alert failure
            Log.i("snapshot: ", snapshot.value.toString())
            if (snapshot.children.count() > 0) {
                verifyEmailError.postValue(true)
                // we store the user object in the app
                Log.i("email event listener", "saving userObject")
                Log.i("userObject: ", snapshot.children.first().getValue<User>().toString())
                userObject.postValue(snapshot.children.first().getValue<User>())

            } else {
                verifyEmailError.postValue(false)
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.i("error: ", error.message)
        }
    }

    private var userValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            Log.i("retrieving user", "result back")
            if (snapshot.children.count() > 0) {
                Log.i("retrieving user", "found the user")
                val resultString = snapshot.children.first().getValue().toString()
                userObject.postValue(snapshot.children.first().getValue<User>())
            } else {
                Log.i("retrieving user", "user not found")
            }
        }
        override fun onCancelled(error: DatabaseError) {
            Log.i("error: ", error.message)
        }
    }

    private var routesValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            Log.i("routes search result", "got back")
            if (snapshot.children.count() > 0) {
                Log.i("routes search result", "there is at least 1 route.")
                val resultListRoute : MutableList<Route> = mutableListOf()
                snapshot.children.map { routeSnapshot ->
                    val route = routeSnapshot.getValue<Route>()
                    route?.let {
                        resultListRoute.add(route)
                    }
                }
                Log.i("routes search result, routes size: ", resultListRoute.size.toString())
                routesResult.postValue(resultListRoute)
            } else {
                Log.i("routes search result", "there is no route found.")
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.i("error: ", error.message)
        }
    }

    fun checkIfEmailUsed(email: String) {
        database
            .child("users")
            .orderByChild("userEmail")
            .equalTo(email)
            .addListenerForSingleValueEvent(emailValueEventListener)
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
        verifyEmailError.value = null
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

    // this method save the route in Firebase database
    fun saveRoute(newRoute: Route) {
        // get logged in user's auth object
        // save in database
        // we save the route in 2 places, 1 in a collection of all routes
        // one inside the user object
        // this is to make searching for routes faster to look up in routes
        database.child("routes").child(newRoute.id).setValue(newRoute) {
                error: DatabaseError?, ref: DatabaseReference ->
            if (error != null) {
                Log.i(TAG, "there is error writing to database: ${error.message}")

            } else {
                Log.i(TAG, "successfully saved route")
                Log.i("saved new route in routes", "ok")
            }
        }
        // first, we get the routesCreated from the user object
        // then, we add the route in the routesCreated
        // then, we update the routesCreated property by the new list

        var newRoutes = HashMap<String, Route>()
        if (userObject.value != null) {
            userObject.value!!.routesCreated.let {
                newRoutes = it
                Log.i("routes: ", "got old routes ${newRoutes.size}")
                newRoutes.put(newRoute.id, newRoute)
            }
        } else {
            Log.i("routes: ", "no route")
            newRoutes.put(newRoute.id, newRoute)
        }

        // save in the user object for the user
        // first we find the user
        // then update the child of the user

        database.child("users").child(userObject.value!!.userID)
            .child("routesCreated").setValue(newRoutes){
                    error: DatabaseError?, ref: DatabaseReference ->
                if (error != null) {
                    Log.i(TAG, "there is error writing to database: ${error.message}")

                } else {
                    Log.i(TAG, "successfully saved routes in user")
                    // here we decided the share route task succeeded
                    _shareSuccess.postValue(true)
                }
            }
    }

    fun searchRoutes(clickedLocation: LatLng) {
        database
            .child("routes")
            .orderByChild("timeCreated")
            //.equalTo(email)
            .addListenerForSingleValueEvent(routesValueEventListener)
    }
}

class FirebaseClientViewModelFactory(private val activity: Activity)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FirebaseClientViewModel::class.java)) {
            return FirebaseClientViewModel(activity) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/*
    private fun getUserRoutes() {
        database
            .child("users")
            .orderByChild("userID")
            .equalTo(email)
            .addListenerForSingleValueEvent(userValueEventListener)
    }

private fun getNewRouteKey(routes: HashMap<String, Route>) : String {
        if (routes.isNotEmpty()) {
            // get all keys
            val keys = routes.keys
            // sort keys
            val sortedKeys = keys.sortedBy { it }
            Log.i("sorted keys: ", sortedKeys.toString())
            val lastKey = sortedKeys.last()
            // get the numeric index
            val lastIndex = lastKey.substring(0, lastKey.length - 1).toInt()
            val newKey = "${lastIndex}A"
            Log.i("newKey: ", newKey)
            return newKey
        } else {
            return "0A"
        }
    }
 */





