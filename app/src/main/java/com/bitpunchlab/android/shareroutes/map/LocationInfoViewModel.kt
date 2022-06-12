package com.bitpunchlab.android.shareroutes.map

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bitpunchlab.android.shareroutes.models.Route
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.libraries.places.api.model.PlaceLikelihood

class LocationInfoViewModel : ViewModel() {

    var _currentListLocation = MutableLiveData<List<PlaceLikelihood>>()
    val currentListLocation get() = _currentLocation

    var _currentLocation = MutableLiveData<PlaceLikelihood?>()
    val currentLocation get() = _currentLocation

    var _lastKnownLocation = MutableLiveData<Location?>()
    val lastKnownLocation get() = _lastKnownLocation

    var _markerList = MutableLiveData<List<Marker>>(emptyList())
    val markerList get() = _markerList

    var _startCreatingRoute = MutableLiveData<Boolean>(false)
    val startCreatingRoute get() = _startCreatingRoute

    var _readyToCreateRoute = MutableLiveData<Boolean>(false)
    val readyToCreateRoute get() = _readyToCreateRoute

    var _clearRouteInfo = MutableLiveData<Boolean>(false)
    val clearRouteInfo get() = _clearRouteInfo

    var _shouldClearPath = MutableLiveData<Boolean>(false)
    val shouldClearPath get() = _shouldClearPath

    var _shouldRestart = MutableLiveData<Boolean>(false)
    val shouldRestart get() = _shouldRestart

    var _routeToShare = MutableLiveData<Route>()
    val routeToShare get() = _routeToShare

    var _routeLine = MutableLiveData<Polyline?>()
    val routeLine get() = _routeLine

    var _searchTerm = MutableLiveData<String?>()
    val searchTerm get() = _searchTerm

    var _shouldNavigateShareRoute = MutableLiveData<Boolean>(false)
    val shouldNavigateShareRoute get() = _shouldNavigateShareRoute

    var _shouldAddMarker = MutableLiveData<Boolean>(false)
    val shouldAddMarker get() = _shouldAddMarker

    var _createRouteChecking = MutableLiveData<Boolean>(false)
    val createRouteChecking get() = _createRouteChecking

    var _shouldShareRoute = MutableLiveData<Boolean>(false)
    val shouldShareRoute get() = _shouldShareRoute


    fun addToMarkerList(marker: Marker) {
        var list = mutableListOf<Marker>()//emptyList<MarkerOptions>()
        if (!markerList.value.isNullOrEmpty()) {
            list = markerList.value!!.toMutableList()
        }
        list.add(marker)
        markerList.value = list
    }
}