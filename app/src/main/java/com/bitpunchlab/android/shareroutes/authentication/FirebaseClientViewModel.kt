package com.bitpunchlab.android.shareroutes

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.util.Patterns
import android.widget.Toast
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
    val databaseError = MutableLiveData<Boolean>(false)
    // we will get the user from the database if we successfully authenticate the user
    var userObject = MutableLiveData<User>()

    var _routeToShare = MutableLiveData<Route>()
    val routeToShare get() = _routeToShare

    var _routesResult = MutableLiveData<List<Route>>()
    val routesResult get() = _routesResult

    var _shareSuccess = MutableLiveData<Boolean>(false)
    val shareSuccess get() = _shareSuccess

    val currentPassword = MutableLiveData<String?>()
    val userAccountState = MutableLiveData<UserAccountState>(UserAccountState.NORMAL)
    val systemLogout = MutableLiveData<Boolean>(false)

    val resetPasswordSuccess = MutableLiveData<Boolean?>()

    var _routesSameCityResult = MutableLiveData<List<Route>>()
    val routesSameCityResult get() = _routesSameCityResult

    private var authStateListener = FirebaseAuth.AuthStateListener { auth ->
        // this is when user is in the logout process, it is not completed yet,
        // so, the user is still logged in.
        // this is to distinguish from when the user didn't login yet, but is not logging out
        if (loggedInUser.value != null && auth.currentUser == null) {
            Log.i(TAG, "logout out successfully")
            loggedInUser.postValue(null)

        } else if (auth.currentUser == null) {
            Log.i("auth listener", "auth state is changed to null")
            systemLogout.value = true
        } else if (auth.currentUser != null) {
            systemLogout.value = false
            Log.i("auth listener", "auth is not null")
            userEmail.value = auth.currentUser!!.email
            // clear the error fields here
            resetErrors()
            // this is the case when user logged in before, and close and then start the app
            // again, it should be logged in
            getUserObject()
            Log.i("auth.uid", auth.uid!!)
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

    // we use this email valid also in user account fragment.
    // we observe it in the fragment to see if it is ready to be updated
    // and only by observing it will it triggers the email error to be shown
    val emailValid: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
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

    // for create user fragment
    var registerUserLiveData = MediatorLiveData<Boolean>()
    // for login fragment
    var readyLoginLiveData = MediatorLiveData<Boolean>()
    // for user account fragment
    var readyUpdatePasswordLiveData = MediatorLiveData<Boolean>()

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

        readyUpdatePasswordLiveData.addSource(passwordValid) { valid ->
            if (valid) {
                readyUpdatePasswordLiveData.value = isReadyUpdatePassword()
            } else {
                readyUpdatePasswordLiveData.value = false
            }
        }
        readyUpdatePasswordLiveData.addSource(confirmPasswordValid) { valid ->
            if (valid) {
                readyUpdatePasswordLiveData.value = isReadyUpdatePassword()
            } else {
                readyUpdatePasswordLiveData.value = false
            }
        }
    }

    fun registerNewUser() {
        // reset default
        verifyEmailError.value = false
        // this part is to register user in the Firebase Auth
        auth.createUserWithEmailAndPassword(userEmail.value!!, userPassword.value!!)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "successfully created user")
                    loggedInUser.postValue(true)
                    Log.i("firebase client, auth.currentUser",
                        (auth.currentUser != null).toString()
                    )
                    // this part is to register user in my database
                    saveUser(auth.uid!!, userName.value!!, userEmail.value!!, userPassword.value!!)
                    // clear password field
                    userPassword.value = ""
                    userConfirmPassword.value = ""
                } else {
                    Log.i(TAG, "error in creating user")
                    // alert user system problem
                    // there will be error if the email already exist
                    // or the firebase system is down.
                    verifyEmailError.postValue(true)
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
                    loggedInUser.postValue(true)
                    // clear password field
                    userPassword.value = ""
                    userConfirmPassword.value = ""
                } else {
                    Log.i(TAG, "error logging in user")
                    loginError.postValue(true)
                }
            }
    }

    private fun resetErrors() {
        nameError.value = ""
        emailError.value = ""
        passwordError.value = ""
        confirmPasswordError.value = ""
    }

    fun resetAppState() {
        userAccountState.value = UserAccountState.NORMAL
    }

    fun updateUserEmail(newEmail: String, password: String) {

        // this part, we update the email in our database
        updateUserEmailInDatabase(newEmail)

        // this part updates email in Firebase Auth
        // here we need to signin again before updating the email.  it is sensitive info
        if (auth.currentUser != null) {
            auth.signInWithEmailAndPassword(auth.currentUser!!.email!!, password)
                .addOnCompleteListener(activity) { task ->
                    if (task.isSuccessful) {
                        Log.i(TAG, "successfully logged in user")
                        auth.currentUser!!.updateEmail(newEmail).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Log.i("update user email", "successfully updated email")
                                // here we update the user email in the app too
                                userEmail.value = ""
                                // we notice user here too
                                userAccountState.postValue(UserAccountState.UPDATE_EMAIL_SUCCESS)
                            } else {
                                Log.i("update user email", "failed to update email")
                                userAccountState.postValue(UserAccountState.UPDATE_EMAIL_SERVER_ERROR)
                            }
                        }
                    } else {
                        userAccountState.postValue(UserAccountState.UPDATE_EMAIL_PASSWORD_ERROR)
                    }
                }
        } else {
            Log.i("update user email", "couldn't find user in database")
            // show alert to user, should login again
            userAccountState.value = UserAccountState.UPDATE_EMAIL_DATA_ERROR
        }
    }

    fun updateUserPassword(newPassword: String, oldPassword: String) {

        if (auth.currentUser != null) {
            auth.signInWithEmailAndPassword(auth.currentUser!!.email!!, oldPassword)
                .addOnCompleteListener(activity) { task ->
                    if (task.isSuccessful) {
                        Log.i(TAG, "successfully logged in user")
                        auth.currentUser!!.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Log.i("update user password", "successfully updated password")
                                // here we reset the user password in the app too
                                userPassword.value = ""
                                userConfirmPassword.value = ""
                                currentPassword.value = ""
                                // we notice user here too
                                userAccountState.postValue(UserAccountState.UPDATE_PASSWORD_SUCCESS)
                            } else {
                                Log.i("update user password", "failed to update password in server")
                                userAccountState.postValue(UserAccountState.UPDATE_PASSWORD_SERVER_ERROR)
                            }
                        }
                    } else {
                        Log.i(TAG, "error logging in user")
                        userAccountState.postValue(UserAccountState.UPDATE_PASSWORD_LOGIN_ERROR)
                    }
                }

        } else {
            Log.i("update user password", "can't get user's profile.")
            userAccountState.value = UserAccountState.UPDATE_PASSWORD_DATA_ERROR
        }
    }

    fun sendPasswordResetEmail(givenEmail: String) {
        // we reset this resetEmailSuccess variable here, to make sure the value is correct
        // when we make the request.
        // null -> no result yet    false -> failed to send     true -> success
        resetPasswordSuccess.value = null
        auth.sendPasswordResetEmail(givenEmail).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.i("reset password email", "is successfully sent.")
                resetPasswordSuccess.postValue(true)
            } else {
                Log.i("error sending password reset email.", "error: ${task.exception}")
                resetPasswordSuccess.postValue(false)
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

    private fun getUserObject() {
        database
            .child("users")
            .orderByChild("userID")
            .equalTo(auth.uid!!)
            .addListenerForSingleValueEvent(userValueEventListener)
    }

    private fun saveUser(id: String, name: String, email: String, password: String) {
        user = createUser(id, name, email)
        saveUserInDatabase(user!!)

    }

    private fun createUser(id: String, name: String, email: String) : User {
        return User(id = id, name = name, email = email,
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

    private fun updateUserEmailInDatabase(newEmail: String) {
        // this part is to update the email in the emails folder
        //userObject.value?.userEmail?.let { email ->
        //    Log.i("update method", "user email: ${userObject.value!!.userEmail!!}")
        //    checkEmailInFolder(email!!)
        //}

        // this part is to update the email property in the user object
        // we access the email field directly and change it.
        userObject.value?.let {
            database.child("users").child(userObject.value!!.userID)
                .child("email")
                .setValue(newEmail) { error: DatabaseError?, ref: DatabaseReference ->
                    if (error != null) {
                        Log.i(TAG, "there is error writing to database: ${error.message}")

                    } else {
                        Log.i(TAG, "successfully saved email in user")
                        // here we decided the share route task succeeded
                    }
                }
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

    private var routesSameCityValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            Log.i("routes search result", "got back")
            if (snapshot.children.count() > 0) {
                Log.i("routes search same city result", "there is at least 1 route.")
                val resultListRoute : MutableList<Route> = mutableListOf()
                snapshot.children.map { routeSnapshot ->
                    val route = routeSnapshot.getValue<Route>()
                    route?.let {
                        resultListRoute.add(route)
                    }
                }
                Log.i("routes search result, routes size: ", resultListRoute.size.toString())
                routesSameCityResult.postValue(resultListRoute)
            } else {
                Log.i("routes search result", "there is no route found.")
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.i("error: ", error.message)
        }
    }

    private var updateEmailValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            Log.i("onDataChange", "got back snapshot")
            if (snapshot.children.count() > 0) {
                snapshot.children.map { emailSnapshot ->
                    Log.i("update listener", "result: $emailSnapshot")
                }
            } else {
                Log.i("update listener", "error, can't find user email.")
            }
        }

        override fun onCancelled(error: DatabaseError) {

        }
    }
/*
    fun checkEmailInFolder(email: String) {
        database
            .child("emails")
            .orderByKey()
            .equalTo(email)
            .addListenerForSingleValueEvent(updateEmailValueEventListener)
    }
*/
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
        loggedInUser.value = null
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
        // this is to prevent crash if the values are null at the beginning
        return (emailValid.value != null || passwordValid.value != null
                || confirmPasswordValid.value != null) &&
                (emailValid.value!! && passwordValid.value!! && confirmPasswordValid.value!!)
    }

    private fun isReadyLogin() : Boolean {
        return (emailValid.value!! && passwordValid.value!!)
    }

    private fun isReadyUpdatePassword() : Boolean {
        return (passwordValid.value!! && confirmPasswordValid.value!!)
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
        // reset here
        databaseError.value = false
        var newRoutes = HashMap<String, Route>()
        if (userObject.value != null) {
            userObject.value!!.routesCreated.let {
                newRoutes = it
                Log.i("routes: ", "got old routes ${newRoutes.size}")
                newRoutes.put(newRoute.id, newRoute)
            }
        } else {
            // notice user couldn't write to database, hence sharing failed
            Log.i("userObject: ", "not found when sharing")
            databaseError.value = true
            //newRoutes.put(newRoute.id, newRoute)
        }

        // save in the user object for the user
        // first we find the user
        // then update the child of the user

        // we update the userObject we got earlier, the routesCreated
        userObject.value?.let {
            database.child("users").child(userObject.value!!.userID)
                //.setValue({ routesCreated: newRoutes})
                .child("routesCreated")
                .setValue(newRoutes) { error: DatabaseError?, ref: DatabaseReference ->
                    if (error != null) {
                        Log.i(TAG, "there is error writing to database: ${error.message}")

                    } else {
                        Log.i(TAG, "successfully saved routes in user")
                        // here we decided the share route task succeeded
                        _shareSuccess.postValue(true)
                    }
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

    // if city is null, return all routes for now
    fun searchRoutesSameCity(city: String?) {

        if (!city.isNullOrEmpty()) {
            Log.i("searching...", "city is $city")
            database
                .child("routes")
                .orderByChild("city")
                .equalTo(city)
                .addListenerForSingleValueEvent(routesSameCityValueEventListener)
        } else {
            Log.i("searching...", "city is null")
            database
                .child("routes")
                .orderByChild("city")
                //.equalTo(city)
                .addListenerForSingleValueEvent(routesSameCityValueEventListener)
        }
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





