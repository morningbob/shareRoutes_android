package com.bitpunchlab.android.shareroutes

enum class AppState {

    APP_NORMAL;

    enum class Share_Route {
        NORMAL,
        ADD_MARKER,
        CREATED_ROUTE,
        SHARED,
        RESTART
    }

    enum class Suggest_Routes {
        NORMAL,
        SEARCHING,
        DISPLAY_ROUTES,
        DISPLAY_CHOSEN,
        RESTART
    }
}

enum class ShareRouteState {
    NORMAL,
    ADD_MARKER,
    CREATED_ROUTE,
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