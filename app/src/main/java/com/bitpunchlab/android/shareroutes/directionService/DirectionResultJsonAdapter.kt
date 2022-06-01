package com.bitpunchlab.android.shareroutes.directionService

import com.google.maps.model.DirectionsResult
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson


@JsonClass(generateAdapter = true)
data class DirectionResultJson(
    val data: String,
    val status: String
)
/*
class DirectionResultJsonAdapter {
    @ToJson
    fun toJson(result: DirectionsResult): DirectionResultJson {
        return DirectionResultJson(
            data = result.data
        )
    }

}

 */