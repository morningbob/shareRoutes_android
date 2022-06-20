package com.bitpunchlab.android.shareroutes


enum class ShareRouteState {
    NORMAL,
    START,
    ADD_MARKER,
    ROUTE_TO_BE_CREATED,
    CREATING_ROUTE,
    ROUTE_CREATED,
    TO_BE_SHARED,
    SAVE_ROUTE,
    CANCEL_SHARING,
    SHARED,
    RESTART
}

enum class SuggestRoutesState {
    NORMAL,
    START,
    PICK_LOCATION,
    CLEAN_CLICK_LISTENER,
    CONFIRMED_LOCATION,
    SEARCHING,
    DISPLAY_ROUTES,
    DISPLAY_CHOSEN,
    CLEAR_ROUTE,
    END,
    RESTART
}