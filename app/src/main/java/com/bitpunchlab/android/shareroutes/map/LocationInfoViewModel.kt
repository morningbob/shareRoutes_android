package com.bitpunchlab.android.shareroutes.map

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.libraries.places.api.model.PlaceLikelihood

class LocationInfoViewModel : ViewModel() {

    var _currentListLocation = MutableLiveData<List<PlaceLikelihood>>()
    val currentListLocation get() = _currentLocation

    var _currentLocation = MutableLiveData<PlaceLikelihood?>()
    val currentLocation get() = _currentLocation

    var _lastKnownLocation = MutableLiveData<Location?>()
    val lastKnownLocation get() = _lastKnownLocation



}