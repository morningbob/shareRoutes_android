package com.bitpunchlab.android.shareroutes.directionService

import com.bitpunchlab.android.shareroutes.DIRECTION_API_BASE_URL
import com.google.maps.model.DirectionsResult
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Deferred
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(ScalarsConverterFactory.create())
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .baseUrl(DIRECTION_API_BASE_URL)
    .build()

interface DirectionsAPIService {

    @GET
    fun getRoute() : Deferred<DirectionsResult>
}

object Network {
    val apiService: DirectionsAPIService
            by lazy { retrofit.create(DirectionsAPIService::class.java) }
}

