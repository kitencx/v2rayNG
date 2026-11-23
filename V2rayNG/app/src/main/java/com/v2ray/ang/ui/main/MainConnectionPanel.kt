package com.v2ray.ang.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.ProfileItem

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
    actionModifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val needsConfig = !state.isRunning && !isLoading && state.selectedGuid.isNullOrBlank()
    val needsSelection = needsConfig && hasVisibleProfiles
    val waitingForConfig = needsConfig && !hasVisibleProfiles
    val profileName = profile?.remarks.orEmpty()
    val title = when {
        waitingForConfig -> stringResource(R.string.home_no_profiles)
        needsSelection -> stringResource(R.string.home_select_profile)
        state.isRunning -> stringResource(R.string.home_service_running)
        else -> stringResource(R.string.connection_not_connected)
    }
    val description = when {
        waitingForConfig -> stringResource(R.string.home_import_hint)
        state.status is MainStatus.Testing || state.status is MainStatus.TestProgress ||
            state.status is MainStatus.ConnectionTest -> displayText
        else -> profile?.let(::serverProtocolDescription).orEmpty()
    }
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = colors.surfaceContainer,
    ) {
        Row(
            Modifier.padding(if (tv) 24.dp else 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!waitingForConfig && !state.isRunning && profileName.isNotBlank()) {
                    Text(profileName, style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (description.isNotBlank()) Text(description, color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
                if (waitingForConfig) {
                    MainControl(stringResource(R.string.acc_add_subscription), onAddSubscription,
                        modifier = actionModifier, emphasized = true, tv = tv, enabled = !isLoading)
                }
                if (state.isRunning) {
                    MainControl(stringResource(R.string.connection_test_pending),
                        { onAction(MainAction.TestCurrentServer) }, tv = tv,
                        icon = R.drawable.ic_fab_check, iconOnly = true)
                }
            }
            if (!waitingForConfig && !needsSelection) {
                MainControl(
                    label = stringResource(if (state.isRunning) R.string.acc_stop else R.string.acc_start),
                    onClick = { onAction(MainAction.ToggleService) },
                    modifier = actionModifier.size(if (tv) 80.dp else 64.dp),
                    tv = tv, emphasized = true, enabled = state.isRunning || !isLoading,
                    icon = R.drawable.ic_qu_start_24dp, iconOnly = true,
                )
            }
        }
    }
}
