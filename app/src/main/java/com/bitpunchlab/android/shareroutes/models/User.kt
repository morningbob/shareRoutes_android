package com.bitpunchlab.android.shareroutes.models

import android.util.Log
import java.util.*

class User  {

    val userID = UUID.randomUUID().toString()
    var userName : String = ""
    var userEmail : String = ""
    var userPassword : String = ""
    var routesCreated : List<Route> = emptyList()

    constructor()

    constructor(name : String, email : String,
                password : String, routes : List<Route>) : this() {
                    userName = name
                    userEmail = email
                    userPassword = password
                    routesCreated = routes
                }
}