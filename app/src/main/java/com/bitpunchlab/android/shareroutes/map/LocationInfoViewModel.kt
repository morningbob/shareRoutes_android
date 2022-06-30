package com.bitpunchlab.android.shareroutes.map

import android.location.Location
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bitpunchlab.android.shareroutes.ShareRouteState
import com.bitpunchlab.android.shareroutes.SuggestRoutesState
import com.bitpunchlab.android.shareroutes.models.Route
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.libraries.places.api.model.PlaceLikelihood

class LocationInfoViewModel : ViewModel() {

    var _currentLocation = MutableLiveData<PlaceLikelihood?>()
    val currentLocation get() = _currentLocation

    var _lastKnownLocation = MutableLiveData<Location?>()
    val lastKnownLocation get() = _lastKnownLocation

    var _markerList = MutableLiveData<List<Marker>>(emptyList())
    val markerList get() = _markerList

    var _routeToShare = MutableLiveData<Route>()
    val routeToShare get() = _routeToShare

    var _routeLine = MutableLiveData<Polyline?>()
    val routeLine get() = _routeLine

    var _runSuggestRoutesFragment = MutableLiveData<Boolean>(false)
    val runSuggestRoutesFragment get() = _runSuggestRoutesFragment

    var _chosenSearchLocation = MutableLiveData<LatLng>()
    val chosenSearchLocation get() = _chosenSearchLocation

    var _chosenRoute = MutableLiveData<Route?>()
    val chosenRoute get() = _chosenRoute

    var _showMyLocation = MutableLiveData<Boolean>(false)
    val showMyLocation get() = _showMyLocation

    var _shareRouteAppState = MutableLiveData<ShareRouteState>(ShareRouteState.NORMAL)
    val shareRouteAppState get() = _shareRouteAppState

    var _suggestRoutesAppState = MutableLiveData<SuggestRoutesState>(SuggestRoutesState.NORMAL)
    val suggestRoutesAppState get() = _suggestRoutesAppState

    var _dismissMapPage = MutableLiveData(false)
    val dismissMapPage get() = _dismissMapPage

    var _searchCity = MutableLiveData<String>()
    val searchCity get() = _searchCity

    fun addToMarkerList(marker: Marker) {
        var list = mutableListOf<Marker>()//emptyList<MarkerOptions>()
        if (!markerList.value.isNullOrEmpty()) {
            list = markerList.value!!.toMutableList()
        }
        list.add(marker)
        markerList.value = list
    }

    fun onRouteClicked(route: Route) {
        Log.i("on route clicked", "a route is chosen")
        _chosenRoute.value = route
    }

    fun finishedNavigatingRoute() {
        Log.i("finished", "set null")
        _chosenRoute.value = null
    }
}