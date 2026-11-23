package com.v2ray.ang.ui.main

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MainTvFocusTest {
    @Test
    fun removedRowMovesFocusToItsSuccessor() {
        assertEquals("c", tvFocusAfterRemoval(listOf("a", "b", "c"), listOf("a", "c"), "b"))
    }

    @Test
    fun removedLastRowMovesFocusToTheLastRemainingRow() {
        assertEquals("a", tvFocusAfterRemoval(listOf("a", "b"), listOf("a"), "b"))
    }

    @Test
    fun noRowsLeavesFocusRestorationToTheScreen() {
        assertNull(tvFocusAfterRemoval(listOf("a"), emptyList(), "a"))
    }

    @Test
    fun missingPreviousRowStartsAtTheFirstVisibleRow() {
        assertEquals("a", tvFocusAfterRemoval(emptyList(), listOf("a"), "b"))
    }
}
