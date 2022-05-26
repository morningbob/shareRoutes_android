package com.bitpunchlab.android.shareroutes.models

import android.util.Log
import java.util.*

class User(var userName : String, var userEmail : String, var userPassword : String) {

    val userID = UUID.randomUUID().toString()

    fun printUser() {
        Log.i("User", "username: $userName")
        Log.i("User", "useremail: $userEmail")
        Log.i("User", "userpassword: $userPassword")
    }
}