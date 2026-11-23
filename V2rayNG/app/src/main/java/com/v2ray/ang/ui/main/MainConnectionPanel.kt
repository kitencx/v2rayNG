package com.v2ray.ang.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        needsSelection -> stringResource(R.string.title_file_chooser)
        state.isRunning -> profileName.takeIf(String::isNotBlank)
            ?: stringResource(R.string.home_service_running)
        else -> stringResource(R.string.connection_not_connected)
    }
    val description = when {
        waitingForConfig -> stringResource(R.string.home_import_hint)
        state.status is MainStatus.Testing || state.status is MainStatus.TestProgress ||
            state.status is MainStatus.ConnectionTest -> displayText
        else -> profile?.let(::serverProtocolDescription).orEmpty()
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainer,
    ) {
        Box {
            ConnectionOrbit(state.isRunning, Modifier.matchParentSize())
            Column(
                Modifier.padding(if (tv) 24.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(if (tv) 16.dp else 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(7.dp).background(
                        if (state.isRunning) colors.primary else colors.outline, CircleShape
                    ))
                    Text(
                        if (state.isRunning) stringResource(R.string.home_service_running)
                        else stringResource(R.string.title_server),
                        color = if (state.isRunning) colors.primary else colors.onSurfaceVariant,
                        style = if (tv) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                    )
                }
                Text(
                    title,
                    modifier = Modifier.widthIn(max = if (tv) 350.dp else 270.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = if (tv) 34.sp else 28.sp,
                    lineHeight = if (tv) 41.sp else 34.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (description.isNotBlank()) {
                    Text(description, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                if (!state.isRunning && !waitingForConfig && profileName.isNotBlank()) {
                    Text(profileName, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (needsSelection) return@Column
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MainControl(
                        label = stringResource(
                            if (waitingForConfig) R.string.acc_add_subscription
                            else if (state.isRunning) R.string.acc_stop else R.string.acc_start
                        ),
                        onClick = if (waitingForConfig) onAddSubscription else ({ onAction(MainAction.ToggleService) }),
                        modifier = actionModifier,
                        tv = tv,
                        emphasized = !state.isRunning,
                        enabled = state.isRunning || !isLoading,
                        iconOnly = !waitingForConfig,
                        icon = if (waitingForConfig) null else if (state.isRunning) {
                            R.drawable.ic_stop_24dp
                        } else {
                            R.drawable.ic_play_24dp
                        },
                    )
                    if (state.isRunning) {
                        MainControl(
                            label = stringResource(R.string.connection_test_pending),
                            onClick = { onAction(MainAction.TestCurrentServer) },
                            tv = tv,
                            icon = R.drawable.ic_fab_check,
                            iconOnly = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionOrbit(running: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val color = if (running) colors.primary else colors.outline
    Canvas(modifier) {
        val center = Offset(size.width * 0.79f, size.height * 0.3f)
        val radius = minOf(size.width * 0.28f, size.height * 0.51f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(color.copy(alpha = 0.23f), color.copy(alpha = 0.06f), color.copy(alpha = 0f)),
                center, radius * 1.7f,
            ),
            radius = radius * 1.7f,
            center = center,
        )
        for (ring in 1..4) {
            drawCircle(
                color.copy(alpha = 0.23f - ring * 0.035f),
                radius * (0.31f + ring * 0.19f),
                center,
                style = Stroke(1.dp.toPx()),
            )
        }
        drawCircle(color.copy(alpha = 0.12f), radius * 0.34f, center)
        val glyph = radius * 0.16f
        drawArc(
            color.copy(alpha = 0.75f), -45f, 270f, false,
            topLeft = center - Offset(glyph, glyph), size = Size(glyph * 2, glyph * 2),
            style = Stroke(1.6.dp.toPx()),
        )
        drawLine(
            color.copy(alpha = 0.75f),
            center - Offset(0f, glyph * 1.3f),
            center - Offset(0f, glyph * 0.15f),
            strokeWidth = 1.6.dp.toPx(),
        )
    }
}
