package com.hajiz.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutesTest {
    @Test
    fun everyRouteRoundTripsThroughPath() {
        HajizRoute.all.forEach { route ->
            assertEquals(route, HajizRoute.fromPath(route.path))
        }
    }

    @Test
    fun unknownPathDoesNotSilentlyNavigate() {
        assertNull(HajizRoute.fromPath("not-a-hajiz-screen"))
    }

    @Test
    fun bottomBarContainsOnlyPrimaryDestinations() {
        assertEquals(
            listOf("home", "protection", "progress", "settings"),
            HajizRoute.bottomNavigation.map { it.path },
        )
    }
}