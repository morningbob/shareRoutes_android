package com.bitpunchlab.android.shareroutes.models

import android.util.Log
import java.util.*
import kotlin.collections.HashMap

class User  {

    val userID = UUID.randomUUID().toString()
    var userName : String = ""
    var userEmail : String = ""
    var userPassword : String = ""
    var routesCreated = HashMap<String, Route>()

    constructor()

    constructor(name : String, email : String,
                password : String, routes : HashMap<String, Route>) : this() {
                    userName = name
                    userEmail = email
                    userPassword = password
                    routesCreated = routes
                }
}