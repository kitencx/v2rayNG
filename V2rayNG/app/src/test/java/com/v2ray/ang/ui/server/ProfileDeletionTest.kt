package com.v2ray.ang.ui.server

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProfileDeletionTest {
    @Test
    fun stoppedSelectedProfileCanBeDeleted() {
        assertFalse(isProfileDeletionBlocked("selected", "selected", false))
    }

    @Test
    fun onlyTheActiveProfileIsProtectedWhileRunning() {
        assertTrue(isProfileDeletionBlocked("selected", "selected", true))
        assertFalse(isProfileDeletionBlocked("other", "selected", true))
        assertFalse(isProfileDeletionBlocked("other", null, true))
    }

    @Test
    fun emptyIdentifiersNeverDeleteAnything() {
        for (running in listOf(false, true)) {
            assertTrue(isProfileDeletionBlocked("", null, running))
            assertTrue(isProfileDeletionBlocked("  ", "selected", running))
        }
    }
}
