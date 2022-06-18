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

    var _shouldCancelSharing = MutableLiveData<Boolean>(false)
    val shouldCancelSharing get() = _shouldCancelSharing

    var _shouldSuggestRoutes = MutableLiveData<Boolean>(false)
    val shouldSuggestRoutes get() = _shouldSuggestRoutes

    var _runSuggestRoutesFragment = MutableLiveData<Boolean>(false)
    val runSuggestRoutesFragment get() = _runSuggestRoutesFragment

    var _chosenSearchLocation = MutableLiveData<LatLng>()
    val chosenSearchLocation get() = _chosenSearchLocation

    var _chosenRoute = MutableLiveData<Route?>()
    val chosenRoute get() = _chosenRoute

    var _routesResult = MutableLiveData<List<Route>>()
    val routesResult get() = _routesResult

    var _closeSuggestion = MutableLiveData<Boolean>(false)
    val closeSuggestion get() = _closeSuggestion

    var _shouldShowRoute = MutableLiveData<Boolean>(false)
    val shouldShowRoute get() = _shouldShowRoute

    var _clearSuggestRoutesInfo = MutableLiveData<Boolean>(false)
    val clearSuggestRoutesInfo get() = _clearSuggestRoutesInfo

    var _showMyLocation = MutableLiveData<Boolean>(false)
    val showMyLocation get() = _showMyLocation

    var _shareRouteAppState = MutableLiveData<ShareRouteState>(ShareRouteState.NORMAL)
    val shareRouteAppState get() = _shareRouteAppState

    var _suggestRoutesAppState = MutableLiveData<SuggestRoutesState>(SuggestRoutesState.NORMAL)
    val suggestRoutesAppState get() = _suggestRoutesAppState



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