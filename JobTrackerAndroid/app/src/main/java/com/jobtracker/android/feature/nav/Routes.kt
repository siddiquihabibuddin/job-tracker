package com.jobtracker.android.feature.nav

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val APPLICATIONS = "applications"
    const val NEW_APPLICATION = "applications/new"
    const val APPLICATION_DETAIL = "applications/{id}"
    const val DASHBOARD = "dashboard"
    const val PROFILE = "profile"
    const val DEBUG_OVERRIDE = "profile/debug-override"

    fun applicationDetail(id: String) = "applications/$id"
}
