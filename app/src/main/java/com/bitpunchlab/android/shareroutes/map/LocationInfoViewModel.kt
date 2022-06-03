package com.bitpunchlab.android.shareroutes.map

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.model.PlaceLikelihood

class LocationInfoViewModel : ViewModel() {

    var _currentListLocation = MutableLiveData<List<PlaceLikelihood>>()
    val currentListLocation get() = _currentLocation

    var _currentLocation = MutableLiveData<PlaceLikelihood?>()
    val currentLocation get() = _currentLocation

    var _lastKnownLocation = MutableLiveData<Location?>()
    val lastKnownLocation get() = _lastKnownLocation

    var _markerList = MutableLiveData<List<MarkerOptions>>(emptyList())
    val markerList get() = _markerList

    var _startCreatingRoute = MutableLiveData<Boolean>(false)
    val startCreatingRoute get() = _startCreatingRoute

    var _readyToCreateRoute = MutableLiveData<Boolean>(false)
    val readyToCreateRoute get() = _readyToCreateRoute


    fun addToMarkerList(marker: MarkerOptions) {
        var list = mutableListOf<MarkerOptions>()//emptyList<MarkerOptions>()
        if (!markerList.value.isNullOrEmpty()) {
            list = markerList.value!!.toMutableList()
        }
        list.add(marker)
        markerList.value = list
    }
}