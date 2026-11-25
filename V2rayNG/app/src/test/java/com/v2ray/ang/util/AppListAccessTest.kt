package com.v2ray.ang.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppListAccessTest {
    @Test
    fun aDeniedOemQueryIsNotPresentedAsACompleteList() {
        assertTrue(hasRestrictedAppVisibility(emptyList(), "own.app"))
        assertTrue(hasRestrictedAppVisibility(listOf("own.app"), "own.app"))
    }

    @Test
    fun visibilityOfOtherAppsClearsTheRestriction() {
        assertFalse(hasRestrictedAppVisibility(listOf("own.app", "browser.app"), "own.app"))
        assertFalse(hasRestrictedAppVisibility(listOf("browser.app"), "own.app"))
    }
}
