package com.v2ray.ang.ui.main

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MainImportResultTest {
    @Test
    fun emptyAndUnrecognizedInputRetainsTheErrorState() {
        assertEquals(MainImportState.Invalid, importResultState(0, 0))
        assertEquals(MainImportState.Invalid, importResultState(-1, 0))
    }

    @Test
    fun configurationsOrSubscriptionsCanCompleteAnImport() {
        for ((profiles, subscriptions) in listOf(1 to 0, 0 to 1, 2 to 3)) {
            assertEquals(MainImportState.Success, importResultState(profiles, subscriptions))
        }
    }
}
