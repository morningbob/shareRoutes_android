package com.bitpunchlab.android.shareroutes.models

import com.google.android.gms.maps.model.LatLng
import java.util.*
import kotlin.collections.HashMap

class Route  {
    val id = UUID.randomUUID().toString()
    val timeCreated = Calendar.getInstance().time
    lateinit var pointsMap : HashMap<String, HashMap<String, HashMap<String, Double>>>
    var name = ""
    var address : String? = null
    var city = ""
    var state = ""
    var country = ""


    constructor()

    constructor(map: HashMap<String, HashMap<String, HashMap<String, Double>>>,
                placeName: String, placeCity: String, placeAddress: String?,
                placeState: String, placeCountry: String) : this() {
        pointsMap = map
        name = placeName
        city = placeCity
        address = placeAddress
        state = placeState
        country = placeCountry
    }
}

/*
class Route {

    var points : List<List<String>> = emptyList()
    val id = UUID.randomUUID().toString()
    val timeCreated = Calendar.getInstance().time

    constructor()

    constructor(pts: List<List<String>>) : this() {
        points = pts
    }
}
*/