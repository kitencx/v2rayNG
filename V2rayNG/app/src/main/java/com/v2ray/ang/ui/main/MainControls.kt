package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalContext
import com.v2ray.ang.extension.toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v2ray.ang.ui.compose.LocalDarkTheme

internal enum class MainControlTone { Neutral, Positive, Info, Warning, Danger }

internal data class MainControlPalette(val container: Color, val content: Color)

/** Main-screen semantic colors stay recognizable with both static and dynamic themes. */
@Composable
internal fun mainControlPalette(tone: MainControlTone): MainControlPalette {
    val colors = MaterialTheme.colorScheme
    val dark = LocalDarkTheme.current
    return when (tone) {
        MainControlTone.Positive -> if (dark) MainControlPalette(Color(0xFF173E2B), Color(0xFFA1E4B7))
            else MainControlPalette(Color(0xFFDEF5E7), Color(0xFF146C43))
        MainControlTone.Info -> if (dark) MainControlPalette(Color(0xFF1C3554), Color(0xFFABCFFF))
            else MainControlPalette(Color(0xFFE1EDFF), Color(0xFF165AA0))
        MainControlTone.Warning -> if (dark) MainControlPalette(Color(0xFF44361C), Color(0xFFF1CF82))
            else MainControlPalette(Color(0xFFFFF0C2), Color(0xFF795300))
        MainControlTone.Danger -> MainControlPalette(colors.errorContainer, colors.onErrorContainer)
        MainControlTone.Neutral -> MainControlPalette(colors.surfaceContainerHigh, colors.onSurfaceVariant)
    }
}

@Composable
internal fun MainControl(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tv: Boolean = false,
    emphasized: Boolean = false,
    selected: Boolean? = null,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    iconOnly: Boolean = false,
    tone: MainControlTone = MainControlTone.Neutral,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val palette = mainControlPalette(tone)
    val background = when {
        !enabled -> colors.surfaceContainerHigh
        focused && tv -> colors.inverseSurface
        tone != MainControlTone.Neutral -> palette.container
        emphasized && enabled -> colors.primary
        selected == true -> colors.primaryContainer
        else -> colors.surfaceContainerHigh
    }
    val foreground = when {
        !enabled -> colors.onSurface.copy(alpha = 0.48f)
        focused && tv -> colors.inverseOnSurface
        tone != MainControlTone.Neutral -> palette.content
        emphasized -> colors.onPrimary
        selected == true -> colors.onPrimaryContainer
        else -> colors.onSurface
    }
    Row(
        modifier = modifier
            .heightIn(min = if (tv) 52.dp else 48.dp)
            .widthIn(min = if (tv) 56.dp else 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .then(if (focused) Modifier.border(2.dp, colors.primary, RoundedCornerShape(12.dp)) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .combinedClickable(enabled = enabled, role = Role.Button, onClick = onClick,
                onLongClick = if (iconOnly) ({ context.toast(label) }) else null)
            .semantics(mergeDescendants = true) { selected?.let { this.selected = it } }
            .padding(horizontal = if (tv) 15.dp else 12.dp, vertical = 9.dp),
        horizontalArrangement = if (iconOnly) Arrangement.Center else Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                painterResource(icon),
                contentDescription = label.takeIf { iconOnly },
                tint = foreground,
                modifier = Modifier.size(if (emphasized && iconOnly) 28.dp else if (tv || iconOnly) 24.dp else 20.dp),
            )
        }
        if (!iconOnly) Text(
            label,
            color = foreground,
            style = if (tv) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
