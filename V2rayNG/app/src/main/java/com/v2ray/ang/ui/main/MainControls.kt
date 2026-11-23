package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val background = when {
        focused && tv -> colors.inverseSurface
        emphasized && enabled -> colors.primary
        selected == true -> colors.primaryContainer
        else -> colors.surfaceContainerHigh
    }
    val foreground = when {
        !enabled -> colors.onSurface.copy(alpha = 0.48f)
        focused && tv -> colors.inverseOnSurface
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
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { selected?.let { this.selected = it } }
            .padding(horizontal = if (tv) 15.dp else 12.dp, vertical = 9.dp),
        horizontalArrangement = if (iconOnly) Arrangement.Center else Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                painterResource(icon),
                contentDescription = label.takeIf { iconOnly },
                tint = foreground,
                modifier = Modifier.size(if (tv) 24.dp else 20.dp),
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
