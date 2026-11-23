package com.v2ray.ang.ui.main

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import com.v2ray.ang.dto.LocateTarget
import com.v2ray.ang.ui.compose.verticalScrollbar

@Composable
internal fun MainTvServerList(
    rows: List<ServerRowUiModel>,
    selectedGuid: String?,
    locateTarget: LocateTarget?,
    state: LazyListState,
    onLocateHandled: () -> Unit,
    onSelect: (String) -> Unit,
    fallback: FocusRequester,
    onRowFocus: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val targetRequester = remember { FocusRequester() }
    var focusedGuid by rememberSaveable { mutableStateOf<String?>(null) }
    var requestedGuid by remember { mutableStateOf<String?>(null) }
    var previousOrder by remember { mutableStateOf(emptyList<String>()) }
    var listHasFocus by remember { mutableStateOf(false) }
    val rowIds = remember(rows) { rows.map { it.guid } }

    LaunchedEffect(rowIds, locateTarget) {
        val previouslyFocused = focusedGuid
        val target = locateTarget?.serverGuid?.takeIf { it in rowIds }
        val shouldRestore = listHasFocus && previouslyFocused != null && previouslyFocused !in rowIds
        val next = target ?: if (shouldRestore) {
            tvFocusAfterRemoval(previousOrder, rowIds, previouslyFocused)
        } else if (previousOrder.isEmpty() && selectedGuid.isNullOrBlank()) {
            rowIds.firstOrNull()
        } else null
        previousOrder = rowIds
        if (next != null) {
            requestedGuid = next
            val position = rowIds.indexOf(next)
            if (state.layoutInfo.visibleItemsInfo.none { it.key == next }) state.scrollToItem(position)
        } else if (shouldRestore) {
            focusedGuid = null
            fallback.requestFocus()
        }
    }

    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize()
            .onFocusChanged { listHasFocus = it.hasFocus }
            .focusRestorer()
            .focusGroup()
            .verticalScrollbar(state),
        contentPadding = contentPadding,
    ) {
        items(rows, key = { it.guid }) { row ->
            if (requestedGuid == row.guid) {
                LaunchedEffect(row.guid) {
                    if (targetRequester.requestFocus() && locateTarget?.serverGuid == row.guid) {
                        onLocateHandled()
                    }
                    requestedGuid = null
                }
            }
            MainServerRow(
                row = row,
                isSelected = selectedGuid == row.guid,
                onSelect = { onSelect(row.guid) },
                modifier = (if (requestedGuid == row.guid) Modifier.focusRequester(targetRequester) else Modifier)
                    .onFocusChanged {
                        if (it.isFocused) {
                            focusedGuid = row.guid
                            onRowFocus(row.guid)
                        }
                    },
                tv = true,
            )
        }
    }
}

internal fun tvFocusAfterRemoval(
    oldOrder: List<String>,
    newOrder: List<String>,
    focusedGuid: String?,
): String? {
    val oldIndex = oldOrder.indexOf(focusedGuid)
    return newOrder.getOrNull(oldIndex.coerceIn(0, newOrder.lastIndex.coerceAtLeast(0)))
}
