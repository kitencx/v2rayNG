package com.v2ray.ang.ui.subscription

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.extension.toast
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.DeleteConfirmDialog
import com.v2ray.ang.ui.compose.QRCodeDialog
import com.v2ray.ang.ui.compose.ReorderableListItem
import com.v2ray.ang.ui.compose.SelectListDialog
import com.v2ray.ang.ui.compose.SettingsSwitchItem
import com.v2ray.ang.ui.compose.verticalScrollbar
import com.v2ray.ang.util.Utils
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private enum class SubscriptionShareAction(@StringRes val labelRes: Int) {
    QRCode(R.string.share_subscription_qrcode),
    Clipboard(R.string.share_subscription_clipboard)
}

private data class SubscriptionDeleteTarget(val guid: String, val name: String)

class SubSettingActivity : BaseComponentActivity() {
    private val viewModel: SubscriptionsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.refreshProfileSuggestions()
    }

    @Composable
    override fun ScreenContent() {
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val subscriptions by viewModel.subsFlow.collectAsStateWithLifecycle()
        val suggestions by viewModel.profileSuggestions.collectAsStateWithLifecycle()
        val confirmRemove by viewModel.confirmRemove.collectAsStateWithLifecycle()
        var editorId by rememberSaveable { mutableStateOf<String?>(null) }
        val editorState = rememberSaveableStateHolder()
        val id = editorId
        val item = if (id == "") remember { SubscriptionItem() } else {
            subscriptions.firstOrNull { it.guid == id }?.subscription
        }
        val closeEditor = {
            if (id != null) editorState.removeState(id)
            editorId = null
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 680.dp && maxWidth > maxHeight
            BackHandler(id != null) { closeEditor() }
            val editor: @Composable () -> Unit = {
                if (id != null && item != null) {
                    editorState.SaveableStateProvider(id) {
                        SubEditScreen(
                            editSubId = id,
                            initial = item,
                            profileSuggestions = suggestions,
                            onBackClick = closeEditor,
                            onSave = {
                                viewModel.saveFromEditor(id, it) {
                                    if (editorId == id) closeEditor()
                                }
                            },
                            onDelete = { removeSub(id); closeEditor() },
                            embedded = wide,
                            confirmRemove = confirmRemove,
                        )
                    }
                }
            }
            SubSettingScreen(
                viewModel = viewModel,
                isLoading = isLoading,
                onBackClick = { if (id != null) closeEditor() else finish() },
                onAddClick = {
                    if (wide) editorId = "" else startActivity(Intent(this@SubSettingActivity, SubEditActivity::class.java))
                },
                onSubUpdate = { viewModel.updateSubscriptions() },
                onEditSub = { groupId ->
                    if (wide) editorId = groupId
                    else startActivity(Intent(this@SubSettingActivity, SubEditActivity::class.java).putExtra("subId", groupId))
                },
                onRemoveSub = { subId ->
                    removeSub(subId)
                    if (id == subId) closeEditor()
                },
                onShareQRCode = viewModel::shareQRCode,
                onShareClipboard = { url ->
                    Utils.setClipboard(this@SubSettingActivity, url)
                    toast(getString(R.string.toast_success))
                },
                wideLayout = wide,
                editorId = editorId.takeIf { wide },
                editor = editor.takeIf { id != null && item != null },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    private fun removeSub(subId: String) {
        viewModel.remove(subId)
    }
}

@Composable
fun SubSettingScreen(
    viewModel: SubscriptionsViewModel,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onSubUpdate: () -> Unit,
    onEditSub: (String) -> Unit,
    onRemoveSub: (String) -> Unit,
    onShareQRCode: (String) -> Unit,
    onShareClipboard: (String) -> Unit,
    wideLayout: Boolean = false,
    editorId: String? = null,
    editor: (@Composable () -> Unit)? = null,
) {
    val subscriptions by viewModel.subsFlow.collectAsStateWithLifecycle()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<SubscriptionDeleteTarget?>(null) }
    val confirmRemove by viewModel.confirmRemove.collectAsStateWithLifecycle()
    val updateOptions by viewModel.updateOptions.collectAsStateWithLifecycle()

    var shareTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    val qrCodeBitmap by viewModel.qrCode.collectAsStateWithLifecycle()

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.move(from.index, to.index)
    }

    Scaffold(
        topBar = {
            if (wideLayout || editor == null) {
                AppTopBar(
                    title = stringResource(R.string.title_sub_setting),
                    onBackClick = onBackClick,
                    isLoading = isLoading,
                    actions = {
                        IconButton(onClick = onAddClick) {
                            Icon(painterResource(R.drawable.ic_add_24dp), contentDescription = stringResource(R.string.acc_add_subscription))
                        }
                        IconButton(onClick = { showUpdateDialog = true }) {
                            Icon(painterResource(R.drawable.ic_restore_24dp), contentDescription = stringResource(R.string.acc_update_subscriptions))
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Row(
            Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)
                .padding(horizontal = if (wideLayout || editor == null) 16.dp else 0.dp,
                    vertical = if (wideLayout || editor == null) 8.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (wideLayout || editor == null) {
            LazyColumn(
                state = lazyListState,
                modifier = (if (wideLayout) Modifier.width(280.dp) else Modifier.weight(1f))
                    .fillMaxHeight().verticalScrollbar(lazyListState),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(items = subscriptions, key = { _, item -> item.guid }) { _, subCache ->
                    ReorderableItem(reorderableState, key = subCache.guid) { isDragging ->
                        ReorderableListItem(scope = this, isDragging = isDragging) {
                            SubscriptionGroupRow(
                                name = subCache.subscription.remarks,
                                lastUpdated = subCache.subscription.lastUpdated,
                                enabled = subCache.subscription.enabled,
                                selected = subCache.guid == editorId,
                                onOpen = { onEditSub(subCache.guid) },
                                onToggle = { viewModel.update(subCache.guid, subCache.subscription.copy(enabled = it)) },
                                onShare = if (subCache.subscription.url.isNotEmpty()) {
                                    { shareTarget = Pair(subCache.guid, subCache.subscription.url) }
                                } else null,
                                onDelete = {
                                    if (confirmRemove) removeTarget = SubscriptionDeleteTarget(subCache.guid, subCache.subscription.remarks)
                                    else onRemoveSub(subCache.guid)
                                },
                            )
                        }
                    }
                }
            }
            }
            if (wideLayout || editor != null) {
                if (wideLayout) VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                // Keep one editor call site across orientations so saveable draft keys stay stable.
                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    content = {
                        if (editor != null) editor() else {
                            Column(Modifier.fillMaxSize().padding(32.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.sub_editor_hint), style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = onAddClick) { Text(stringResource(R.string.acc_add_subscription)) }
                            }
                        }
                    },
                )
            }
        }
    }

    if (shareTarget != null) {
        val (_, url) = shareTarget!!
        SelectListDialog(
            options = SubscriptionShareAction.entries,
            optionText = { stringResource(it.labelRes) },
            onSelected = { action ->
                shareTarget = null
                when (action) {
                    SubscriptionShareAction.QRCode -> onShareQRCode(url)
                    SubscriptionShareAction.Clipboard -> onShareClipboard(url)
                }
            },
            onDismiss = { shareTarget = null }
        )
    }

    // QR Code Dialog
    if (qrCodeBitmap != null) {
        QRCodeDialog(
            bitmap = qrCodeBitmap,
            onDismiss = viewModel::dismissQRCode
        )
    }

    removeTarget?.let { target ->
        DeleteConfirmDialog(
            message = stringResource(R.string.confirm_delete_subscription_group),
            itemName = target.name,
            onConfirm = {
                removeTarget = null
                onRemoveSub(target.guid)
            },
            onDismiss = { removeTarget = null }
        )
    }

    if (showUpdateDialog) {

        val updateSubscription = updateOptions[AppConfig.PREF_UPDATE_SUBSCRIPTION] ?: false
        val autoTestAfterUpdateSubscription = updateOptions[AppConfig.PREF_AUTO_TEST_AFTER_UPDATE_SUBSCRIPTION] ?: false
        val autoRemoveInvalidAfterTest = updateOptions[AppConfig.PREF_AUTO_REMOVE_INVALID_AFTER_TEST] ?: false
        val autoSortAfterTest = updateOptions[AppConfig.PREF_AUTO_SORT_AFTER_TEST] ?: false

        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            text = {
                Column {
                    SettingsSwitchItem(
                        title = stringResource(R.string.title_sub_update),
                        checked = updateSubscription,
                        onCheckedChange = { viewModel.setUpdateOption(AppConfig.PREF_UPDATE_SUBSCRIPTION, it) }
                    )
                    SettingsSwitchItem(
                        title = stringResource(R.string.title_pref_auto_test_after_update_subscription),
                        summary = stringResource(R.string.summary_pref_auto_test_after_update_subscription),
                        checked = autoTestAfterUpdateSubscription,
                        onCheckedChange = { viewModel.setUpdateOption(AppConfig.PREF_AUTO_TEST_AFTER_UPDATE_SUBSCRIPTION, it) }
                    )
                    SettingsSwitchItem(
                        title = stringResource(R.string.title_pref_auto_remove_invalid_after_test),
                        summary = stringResource(R.string.summary_pref_auto_remove_invalid_after_test),
                        checked = autoRemoveInvalidAfterTest,
                        enabled = autoTestAfterUpdateSubscription,
                        onCheckedChange = { viewModel.setUpdateOption(AppConfig.PREF_AUTO_REMOVE_INVALID_AFTER_TEST, it) }
                    )
                    SettingsSwitchItem(
                        title = stringResource(R.string.title_pref_auto_sort_after_test),
                        summary = stringResource(R.string.summary_pref_auto_sort_after_test),
                        checked = autoSortAfterTest,
                        enabled = autoTestAfterUpdateSubscription,
                        onCheckedChange = { viewModel.setUpdateOption(AppConfig.PREF_AUTO_SORT_AFTER_TEST, it) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateDialog = false
                    onSubUpdate()
                }) {
                    Text(text = stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }
}


@Composable
private fun SubscriptionGroupRow(
    name: String,
    lastUpdated: Long,
    enabled: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onShare: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val colors = MaterialTheme.colorScheme
    val switchLabel = stringResource(R.string.sub_setting_enable)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) colors.primaryContainer else colors.surfaceContainerLow)
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f).clip(shape)
                .onFocusChanged { focused = it.isFocused }
                .then(if (focused) Modifier.border(2.dp, colors.primary, shape) else Modifier)
                .selectable(selected, role = Role.Button, onClick = onOpen)
                .padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (lastUpdated > 0) {
                Spacer(Modifier.height(2.dp))
                Text(Utils.formatTimestamp(lastUpdated), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.semantics { contentDescription = switchLabel },
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onSecondary,
                checkedTrackColor = colors.secondary,
            ),
        )
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(painterResource(R.drawable.ic_more_vert_24dp), stringResource(R.string.acc_more))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (onShare != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.acc_share_subscription)) },
                        onClick = { showMenu = false; onShare() },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.acc_delete)) },
                    onClick = { showMenu = false; onDelete() },
                )
            }
        }
    }
}
