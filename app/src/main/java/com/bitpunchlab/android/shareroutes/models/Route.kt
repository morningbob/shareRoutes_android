package com.bitpunchlab.android.shareroutes.models

import com.google.android.gms.maps.model.LatLng
import java.util.*


class Route(var points: List<LatLng>) {

    val id = UUID.randomUUID().toString()
    val timeCreated = Calendar.getInstance().time
}