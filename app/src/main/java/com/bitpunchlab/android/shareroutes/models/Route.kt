package com.bitpunchlab.android.shareroutes.models

import com.google.android.gms.maps.model.LatLng
import java.util.*


class Route {

    var points : List<List<String>> = emptyList()
    val id = UUID.randomUUID().toString()
    val timeCreated = Calendar.getInstance().time

    constructor()

    constructor(pts: List<List<String>>) : this() {
        points = pts
    }
}