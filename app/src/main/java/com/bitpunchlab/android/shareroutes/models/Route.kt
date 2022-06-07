package com.bitpunchlab.android.shareroutes.models

import com.google.android.gms.maps.model.LatLng
import java.util.*
import kotlin.collections.HashMap

class Route  {
    val id = UUID.randomUUID().toString()
    val timeCreated = Calendar.getInstance().time
    lateinit var pointsMap : HashMap<String, HashMap<String, HashMap<String, Double>>>

    constructor()

    constructor(map: HashMap<String, HashMap<String, HashMap<String, Double>>>) : this() {
        pointsMap = map
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