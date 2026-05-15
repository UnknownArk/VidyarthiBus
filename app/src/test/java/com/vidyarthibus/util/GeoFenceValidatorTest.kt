package com.vidyarthibus.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeoFenceValidatorTest {
    @Test
    fun acceptsPointInsideRadius() {
        assertTrue(
            GeoFenceValidator.isWithinRadius(
                userLat = 12.9717,
                userLng = 77.5947,
                centerLat = 12.9716,
                centerLng = 77.5946,
                radiusMeters = 50
            )
        )
    }

    @Test
    fun rejectsPointOutsideRadius() {
        assertFalse(
            GeoFenceValidator.isWithinRadius(
                userLat = 13.0500,
                userLng = 77.7000,
                centerLat = 12.9716,
                centerLng = 77.5946,
                radiusMeters = 100
            )
        )
    }
}
