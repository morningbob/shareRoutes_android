package com.bitpunchlab.android.shareroutes.models

import android.util.Log
import java.util.*
import kotlin.collections.HashMap

class User  {

    var userID : String = ""//UUID.randomUUID().toString()
    var userName : String = ""
    var userEmail : String = ""
    var routesCreated = HashMap<String, Route>()

    constructor()

    constructor(id: String, name : String, email : String,
                routes : HashMap<String, Route>) : this() {
                    userID = id
                    userName = name
                    userEmail = email
                    routesCreated = routes
                }
}