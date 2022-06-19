package com.bitpunchlab.android.shareroutes


enum class ShareRouteState {
    NORMAL,
    ADD_MARKER,
    ROUTE_TO_BE_CREATED,
    CREATING_ROUTE,
    ROUTE_CREATED,
    TO_BE_SHARED,
    SHARED,
    RESTART
}

enum class SuggestRoutesState {
    NORMAL,
    SEARCHING,
    DISPLAY_ROUTES,
    DISPLAY_CHOSEN,
    RESTART
}