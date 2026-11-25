package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.ConnectionTestResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MainConnectionPresentationTest {
    @Test
    fun initialStateHasNoConfigurationAndNoNetworkResult() {
        val state = MainUiState()
        assertEquals(MainServicePhase.Empty, mainServicePhase(state, false, false))
        assertEquals(MainNetworkPhase.Unchecked, mainNetworkPhase(state))
    }

    @Test
    fun loadingDoesNotFlashAnEmptyState() {
        assertEquals(MainServicePhase.Loading, mainServicePhase(MainUiState(), true, false))
    }

    @Test
    fun visibleConfigurationsRequireSelectionUntilAGuidIsChosen() {
        for (guid in listOf(null, "", " ")) {
            assertEquals(MainServicePhase.SelectionRequired,
                mainServicePhase(MainUiState(selectedGuid = guid), false, true))
        }
    }

    @Test
    fun aSelectedStoppedServiceStaysStoppedDuringBackgroundLoadingAndFiltering() {
        val state = MainUiState(selectedGuid = "selected")
        assertEquals(MainServicePhase.Stopped, mainServicePhase(state, true, false))
        assertEquals(MainServicePhase.Stopped, mainServicePhase(state, false, true))
    }

    @Test
    fun runningServiceDoesNotClaimInternetAccessWithoutATest() {
        val state = MainUiState(isRunning = true, status = MainStatus.Connected)
        assertEquals(MainServicePhase.Running, mainServicePhase(state, true, false))
        assertEquals(MainNetworkPhase.Unchecked, mainNetworkPhase(state))
    }

    @Test
    fun eachPendingTestStateIsPresentedAsTesting() {
        for (status in listOf(MainStatus.Testing, MainStatus.TestProgress("2"))) {
            assertEquals(MainNetworkPhase.Testing, mainNetworkPhase(MainUiState(isRunning = true, status = status)))
        }
        assertEquals(MainNetworkPhase.Testing, mainNetworkPhase(MainUiState(isRunning = true, isTesting = true,
            status = MainStatus.ConnectionTest(ConnectionTestResult(-1)))))
    }

    @Test
    fun successfulTestsIncludeZeroLatency() {
        for (delay in listOf(0L, 54L)) {
            assertEquals(MainNetworkPhase.Available, mainNetworkPhase(MainUiState(isRunning = true,
                status = MainStatus.ConnectionTest(ConnectionTestResult(delay)))))
        }
    }

    @Test
    fun failedNetworkDoesNotMeanThatTheLocalServiceStopped() {
        for (detail in listOf("", "timeout")) {
            val state = MainUiState(isRunning = true, status = MainStatus.ConnectionTest(ConnectionTestResult(-1, detail)))
            assertEquals(MainServicePhase.Running, mainServicePhase(state, false, true))
            assertEquals(MainNetworkPhase.Failed, mainNetworkPhase(state))
        }
    }

    @Test
    fun stoppingHidesStaleSuccessfulFailedAndPendingResults() {
        for (status in listOf(MainStatus.Testing, MainStatus.TestProgress("1"),
            MainStatus.ConnectionTest(ConnectionTestResult(50)), MainStatus.ConnectionTest(ConnectionTestResult(-1)))) {
            val state = MainUiState(selectedGuid = "selected", isTesting = true, status = status)
            assertEquals(MainServicePhase.Stopped, mainServicePhase(state, false, true))
            assertEquals(MainNetworkPhase.Unchecked, mainNetworkPhase(state))
        }
    }
}
