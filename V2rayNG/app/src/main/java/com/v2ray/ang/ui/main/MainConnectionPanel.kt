package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.ProfileItem

internal enum class MainServicePhase(@StringRes val label: Int, val tone: MainControlTone) {
    Loading(R.string.home_status_loading, MainControlTone.Info),
    Empty(R.string.home_status_empty, MainControlTone.Warning),
    SelectionRequired(R.string.home_status_select, MainControlTone.Warning),
    Stopped(R.string.home_status_stopped, MainControlTone.Neutral),
    Running(R.string.home_status_running, MainControlTone.Positive),
}

internal enum class MainNetworkPhase { Unchecked, Testing, Available, Failed }

// A running service is not evidence of internet access; only a current connection test supplies that result.
internal fun mainServicePhase(state: MainUiState, loading: Boolean, hasProfiles: Boolean): MainServicePhase = when {
    state.isRunning -> MainServicePhase.Running
    !state.selectedGuid.isNullOrBlank() -> MainServicePhase.Stopped
    hasProfiles -> MainServicePhase.SelectionRequired
    loading -> MainServicePhase.Loading
    else -> MainServicePhase.Empty
}

internal fun mainNetworkPhase(state: MainUiState): MainNetworkPhase = when {
    !state.isRunning -> MainNetworkPhase.Unchecked
    state.isTesting || state.status is MainStatus.Testing || state.status is MainStatus.TestProgress -> MainNetworkPhase.Testing
    state.status is MainStatus.ConnectionTest -> if (state.status.result.delayMillis >= 0) MainNetworkPhase.Available else MainNetworkPhase.Failed
    else -> MainNetworkPhase.Unchecked
}

@Composable
internal fun MainConnectionPanel(
    state: MainUiState,
    profile: ProfileItem?,
    displayText: String,
    isLoading: Boolean,
    hasVisibleProfiles: Boolean,
    tv: Boolean,
    onAction: (MainAction) -> Unit,
    onAddSubscription: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    actionModifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val phase = mainServicePhase(state, isLoading, hasVisibleProfiles)
    val network = mainNetworkPhase(state)
    val hasSelection = !state.selectedGuid.isNullOrBlank()
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = colors.surfaceContainer) {
        Column(
            Modifier.padding(if (tv) 24.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.weight(1f)) {
                    MainStatusBadge(stringResource(phase.label), phase.tone,
                        pending = phase == MainServicePhase.Loading)
                }
                if (onEdit != null) {
                    MainControl(stringResource(R.string.action_edit), onEdit, tv = tv,
                        icon = R.drawable.ic_edit_24dp, iconOnly = true, tone = MainControlTone.Info)
                }
                if (onMore != null) {
                    MainControl(stringResource(R.string.acc_more), onMore, tv = tv,
                        icon = R.drawable.ic_more_horiz_24dp, iconOnly = true)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val title = when (phase) {
                        MainServicePhase.Empty -> stringResource(R.string.home_no_profiles)
                        MainServicePhase.SelectionRequired -> stringResource(R.string.home_select_profile)
                        MainServicePhase.Loading -> ""
                        else -> profile?.remarks.orEmpty()
                    }
                    if (title.isNotBlank()) Text(title, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val description = profile?.let(::serverProtocolDescription).orEmpty()
                    if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant)
                    if (phase == MainServicePhase.Empty) {
                        Text(stringResource(R.string.home_import_hint), style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant)
                        MainControl(stringResource(R.string.acc_add_subscription), onAddSubscription,
                            modifier = actionModifier, tv = tv, icon = R.drawable.ic_add_24dp,
                            tone = MainControlTone.Info)
                    }
                }
                if (hasSelection || state.isRunning) {
                    MainControl(
                        stringResource(if (state.isRunning) R.string.acc_stop else R.string.acc_start),
                        { onAction(MainAction.ToggleService) },
                        modifier = actionModifier.size(if (tv) 80.dp else 64.dp),
                        tv = tv, emphasized = true, enabled = state.isRunning || !isLoading,
                        icon = if (state.isRunning) R.drawable.ic_stop_24dp else R.drawable.ic_service_power_24dp,
                        iconOnly = true, tone = if (state.isRunning) MainControlTone.Danger else MainControlTone.Positive,
                    )
                }
            }
            if (state.isRunning) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (network) {
                            MainNetworkPhase.Unchecked -> Text(stringResource(R.string.home_network_unchecked),
                                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            MainNetworkPhase.Testing -> MainStatusBadge(stringResource(R.string.connection_test_testing),
                                MainControlTone.Info, pending = true)
                            MainNetworkPhase.Available -> MainStatusBadge(stringResource(R.string.home_network_available),
                                MainControlTone.Positive, icon = R.drawable.ic_action_done)
                            MainNetworkPhase.Failed -> MainStatusBadge(stringResource(R.string.home_network_failed),
                                MainControlTone.Danger, icon = R.drawable.ic_status_alert_24dp)
                        }
                        val showDetails = network == MainNetworkPhase.Available || network == MainNetworkPhase.Failed ||
                            network == MainNetworkPhase.Testing && state.status is MainStatus.TestProgress
                        if (showDetails && displayText.isNotBlank()) {
                            Text(displayText, style = MaterialTheme.typography.bodySmall,
                                color = if (network == MainNetworkPhase.Failed) colors.error else colors.onSurfaceVariant,
                                maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    MainControl(stringResource(R.string.connection_test_pending),
                        { onAction(MainAction.TestCurrentServer) }, tv = tv,
                        enabled = network != MainNetworkPhase.Testing,
                        icon = R.drawable.ic_speedometer_24dp, iconOnly = true, tone = MainControlTone.Info)
                }
            }
        }
    }
}

@Composable
private fun MainStatusBadge(
    label: String,
    tone: MainControlTone,
    modifier: Modifier = Modifier,
    pending: Boolean = false,
    @DrawableRes icon: Int? = null,
) {
    val palette = mainControlPalette(tone)
    Surface(modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(10.dp), color = palette.container,
        border = BorderStroke(1.dp, palette.content.copy(alpha = 0.16f))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            when {
                pending -> CircularProgressIndicator(Modifier.size(14.dp).clearAndSetSemantics {},
                    color = palette.content, strokeWidth = 2.dp)
                icon != null -> Icon(painterResource(icon), null, Modifier.size(16.dp), tint = palette.content)
                else -> Box(Modifier.size(7.dp).background(palette.content, CircleShape))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = palette.content,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
