package com.v2ray.ang.ui.main

import android.content.res.Configuration
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import com.v2ray.ang.ui.compose.AdaptiveContent
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.ui.compose.QRCodeDialog
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    hasLeanback: Boolean,
    hasCamera: Boolean,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val groups = uiState.groups
    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    val displayText = mainViewModel.formatStatus(uiState.status)
    val selectedGuid = uiState.selectedGuid
    val doubleColumnDisplay = uiState.doubleColumnDisplay
    val confirmRemove = uiState.confirmRemove
    val shareQRCodeBitmap = uiState.shareQRCodeBitmap

    val uiMode = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK
    val tv = hasLeanback || uiMode == Configuration.UI_MODE_TYPE_TELEVISION
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDelAllConfirm by remember { mutableStateOf(false) }
    var showDelDuplicateConfirm by remember { mutableStateOf(false) }
    var showDelInvalidConfirm by remember { mutableStateOf(false) }
    var showRemoveConfirm by rememberSaveable(stateSaver = ServerDeleteTarget.Saver) {
        mutableStateOf<ServerDeleteTarget?>(null)
    }

    var shareTarget by remember { mutableStateOf<Triple<String, ProfileItem, Boolean>?>(null) }
    val removeServer: (String, String) -> Unit = { guid, profileName ->
        if (confirmRemove) {
            showRemoveConfirm = ServerDeleteTarget(guid, profileName)
        } else {
            onAction(MainAction.RemoveServer(guid))
        }
    }

    val pagerState = rememberPagerState(
        initialPage = groups.indexOfFirst { it.id == uiState.selectedGroupId }.coerceAtLeast(0),
        pageCount = { groups.size.coerceAtLeast(1) }
    )

    val lazyListStates = remember { mutableStateMapOf<String, LazyListState>() }
    val lazyGridStates = remember { mutableStateMapOf<String, LazyGridState>() }

    LaunchedEffect(groups) {
        val validGroupIds = groups.map { it.id }.toSet()
        lazyListStates.keys.retainAll(validGroupIds)
        lazyGridStates.keys.retainAll(validGroupIds)
    }

    LaunchedEffect(tv, groups, uiState.selectedGroupId) {
        if (tv || groups.isEmpty()) return@LaunchedEffect
        val selectedIndex = groups.indexOfFirst { it.id == uiState.selectedGroupId }
            .takeIf { it >= 0 } ?: 0
        if (!pagerState.isScrollInProgress && pagerState.settledPage != selectedIndex) {
            pagerState.scrollToPage(selectedIndex)
        }
    }

    val latestGroups by rememberUpdatedState(groups)
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var importSubscription by rememberSaveable { mutableStateOf(true) }
    var inspectedGuid by rememberSaveable(uiState.selectedGroupId) { mutableStateOf<String?>(null) }
    val currentGroupFlow = remember(mainViewModel, uiState.selectedGroupId) {
        mainViewModel.serverGroupState(uiState.selectedGroupId)
    }
    val currentGroup by currentGroupFlow.collectAsStateWithLifecycle()

    LaunchedEffect(pagerState, tv) {
        if (tv) return@LaunchedEffect
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val currentGroups = latestGroups
                if (page in currentGroups.indices) {
                    onAction(MainAction.SelectGroup(currentGroups[page].id))
                }
            }
    }

    MainDialogs(
        showDelAllConfirm = showDelAllConfirm,
        onDismissDelAll = { showDelAllConfirm = false },
        onConfirmDelAll = { showDelAllConfirm = false; onAction(MainAction.RemoveAllServers) },
        showDelDuplicateConfirm = showDelDuplicateConfirm,
        onDismissDelDuplicate = { showDelDuplicateConfirm = false },
        onConfirmDelDuplicate = { showDelDuplicateConfirm = false; onAction(MainAction.RemoveDuplicateServers) },
        showDelInvalidConfirm = showDelInvalidConfirm,
        onDismissDelInvalid = { showDelInvalidConfirm = false },
        onConfirmDelInvalid = { showDelInvalidConfirm = false; onAction(MainAction.RemoveInvalidServers) },
        showRemoveConfirm = showRemoveConfirm,
        onDismissRemove = { showRemoveConfirm = null },
        onConfirmRemove = { guid -> showRemoveConfirm = null; onAction(MainAction.RemoveServer(guid)) }
    )

    if (shareTarget != null) {
        val (guid, profile, more) = shareTarget!!
        ShareMethodDialog(
            guid = guid,
            profile = profile,
            more = more,
            onDismiss = { shareTarget = null },
            onAction = onAction,
            onRemove = removeServer,
        )
    }
    if (shareQRCodeBitmap != null) {
        QRCodeDialog(bitmap = shareQRCodeBitmap, onDismiss = { onAction(MainAction.DismissQRCodeDialog) })
    }

    LaunchedEffect(uiState.importState) {
        if (uiState.importState == MainImportState.Success) {
            showImportDialog = false
            onAction(MainAction.DismissImportResult)
        }
    }
    if (showImportDialog) {
        MainImportDialog(
            isSubscription = importSubscription,
            pending = uiState.importState == MainImportState.Working,
            errorMessage = when (uiState.importState) {
                MainImportState.Invalid -> stringResource(R.string.import_failure_detail)
                MainImportState.InvalidUrl -> stringResource(R.string.toast_invalid_url)
                MainImportState.Failed -> stringResource(R.string.toast_failure)
                else -> null
            },
            onDismiss = { showImportDialog = false; onAction(MainAction.DismissImportResult) },
            onAction = onAction,
        )
    }

    ModalNavigationDrawer(
        gesturesEnabled = !tv,
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                drawerState = drawerState,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    onNavigate(route)
                }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
            topBar = {
                MainTopBar(
                    isLoading = isLoading,
                    hasCamera = hasCamera,
                    onAddSubscription = {
                        importSubscription = true
                        showImportDialog = true
                    },
                    onImportText = {
                        importSubscription = false
                        showImportDialog = true
                    },
                    showSearch = showSearch,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { query: String ->
                        searchQuery = query
                        onAction(MainAction.Search(query))
                    },
                    onSearchClose = {
                        searchQuery = ""
                        onAction(MainAction.Search(""))
                        showSearch = false
                    },
                    onSearchToggle = { show: Boolean -> showSearch = show },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onAction = onAction,
                    onMoreMenuAction = { action ->
                        when (action) {
                            MainMoreMenuAction.RestartService -> onAction(MainAction.RestartService)
                            MainMoreMenuAction.DeleteAll -> showDelAllConfirm = true
                            MainMoreMenuAction.DeleteDuplicate -> showDelDuplicateConfirm = true
                            MainMoreMenuAction.DeleteInvalid -> showDelInvalidConfirm = true
                            MainMoreMenuAction.ExportAll -> onAction(MainAction.ExportAll)
                            MainMoreMenuAction.LocateSelected -> onAction(MainAction.LocateSelectedServer)
                            MainMoreMenuAction.SortByTestResults -> onAction(MainAction.SortByTestResults)
                            MainMoreMenuAction.TestAll -> onAction(MainAction.TestAllServers)
                            MainMoreMenuAction.TestAllRealPing -> onAction(MainAction.TestRealAllServers)
                            MainMoreMenuAction.UpdateSubscriptions -> onAction(MainAction.UpdateSubscriptions)
                        }
                    }
                )
            },
        ) { innerPadding ->
            BoxWithConstraints(
                Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding),
            ) {
                val wide = tv || maxWidth >= 840.dp && maxWidth > maxHeight
                val horizontalPadding = if (tv) 24.dp else 16.dp
                val actionFocus = remember { FocusRequester() }
                var initialFocusSet by remember { mutableStateOf(false) }
                val waitingForNodeSelection = selectedGuid.isNullOrBlank() && currentGroup.rows.isNotEmpty()
                LaunchedEffect(tv, isLoading, waitingForNodeSelection) {
                    if (tv && !isLoading && !initialFocusSet && !waitingForNodeSelection) {
                        if (actionFocus.requestFocus()) initialFocusSet = true
                    }
                }

                val connection: @Composable () -> Unit = {
                    MainConnectionPanel(
                        state = uiState,
                        profile = uiState.selectedProfile,
                        displayText = displayText,
                        isLoading = isLoading,
                        hasVisibleProfiles = currentGroup.rows.isNotEmpty(),
                        tv = tv,
                        onAction = onAction,
                        onAddSubscription = {
                            importSubscription = true
                            showImportDialog = true
                        },
                        actionModifier = if (tv) Modifier.focusRequester(actionFocus) else Modifier,
                    )
                }
                val serverPage: @Composable (GroupMapItem) -> Unit = { group ->
                    GroupPagerPage(
                        groupId = group.id,
                        mainViewModel = mainViewModel,
                        selectedGuid = selectedGuid,
                        locateTarget = uiState.locateTarget,
                        doubleColumnDisplay = doubleColumnDisplay,
                        searchQuery = searchQuery,
                        lazyListStates = lazyListStates,
                        lazyGridStates = lazyGridStates,
                        onSelectServer = { onAction(MainAction.SelectServer(it)) },
                        onMoreServer = { guid, profile -> shareTarget = Triple(guid, profile, true) },
                        usageInfo = group.usageInfo,
                        groupTitle = group.remarks,
                        onOpenLink = { onAction(MainAction.OpenSubscriptionLink(it)) },
                        contentPadding = PaddingValues(bottom = 8.dp),
                        tv = tv,
                        listFocusFallback = actionFocus,
                        onTvFocus = { inspectedGuid = it },
                    )
                }
                val showImportShortcuts = !isLoading && selectedGuid.isNullOrBlank() && currentGroup.rows.isEmpty() &&
                    searchQuery.isEmpty()
                val profiles: @Composable () -> Unit = {
                    Column(Modifier.fillMaxSize()) {
                        Text(
                            stringResource(R.string.title_server),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                        if (groups.size > 1) {
                            if (tv) {
                                Row(
                                    Modifier.horizontalScroll(rememberScrollState()).focusGroup()
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    groups.forEach { group ->
                                        key(group.id) {
                                            MainControl(
                                                label = group.remarks,
                                                onClick = { onAction(MainAction.SelectGroup(group.id)) },
                                                tv = true,
                                                selected = group.id == uiState.selectedGroupId,
                                            )
                                        }
                                    }
                                }
                            } else {
                                GroupTabBar(
                                    groups = groups,
                                    selectedTabIndex = pagerState.currentPage.coerceIn(groups.indices),
                                    mainViewModel = mainViewModel,
                                    onTabClick = { target ->
                                        scope.launch { pagerState.navigateToPageOptimized(target, true) }
                                    },
                                )
                            }
                        }
                        if (showImportShortcuts && !wide) {
                            MainImportShortcuts(
                                tv = false,
                                hasCamera = hasCamera,
                                onAction = onAction,
                                onImportText = {
                                    importSubscription = false
                                    showImportDialog = true
                                },
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                            )
                        } else if (tv) {
                            groups.firstOrNull { it.id == uiState.selectedGroupId }?.let { group ->
                                key(group.id) { serverPage(group) }
                            }
                        } else if (groups.isNotEmpty()) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                beyondViewportPageCount = 1,
                                key = { page -> groups.getOrNull(page)?.id ?: "group-page-$page" },
                            ) { page ->
                                groups.getOrNull(page)?.let { serverPage(it) }
                            }
                        }
                    }
                }

                if (wide && showImportShortcuts && !tv) {
                    Row(Modifier.fillMaxSize().padding(horizontal = horizontalPadding, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MainNavigation(false, onNavigate, Modifier.width(88.dp),
                            onMenu = { scope.launch { drawerState.open() } },
                            onProfiles = { onAction(MainAction.LocateSelectedServer) })
                        AdaptiveContent(Modifier.weight(1f), maxWidth = 600.dp) {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 28.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                connection()
                                MainImportShortcuts(false, hasCamera, onAction, onImportText = {
                                    importSubscription = false
                                    showImportDialog = true
                                })
                            }
                        }
                    }
                } else if (wide) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = horizontalPadding, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (tv) 20.dp else 16.dp),
                    ) {
                        MainNavigation(tv, onNavigate, Modifier.width(if (tv) 140.dp else 88.dp),
                            onMenu = { scope.launch { drawerState.open() } },
                            onProfiles = { onAction(MainAction.LocateSelectedServer) })
                        Box(Modifier.weight(1f).fillMaxHeight()) { profiles() }
                        Column(
                            Modifier.weight(if (tv) 1f else 0.65f).fillMaxHeight()
                                .verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            connection()
                            val inspected = if (tv) currentGroup.rows.firstOrNull { it.guid == inspectedGuid } else null
                            val profile = inspected?.profile ?: uiState.selectedProfile
                            val guid = inspected?.guid ?: uiState.selectedGuid
                            if (profile != null && guid != null) {
                                if (inspected != null) {
                                    Text(
                                        inspected.remarks,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!tv) {
                                        MainControl(
                                            label = stringResource(R.string.action_edit),
                                            onClick = { onAction(MainAction.EditServer(guid, profile)) },
                                            icon = R.drawable.ic_edit_24dp,
                                            iconOnly = true,
                                        )
                                    }
                                    MainControl(
                                        label = stringResource(R.string.acc_more),
                                        onClick = { shareTarget = Triple(guid, profile, true) },
                                        tv = tv,
                                        icon = R.drawable.ic_more_vert_24dp,
                                        iconOnly = true,
                                    )
                                }
                                if (inspected != null && searchQuery.isEmpty() && uiState.selectedGroupId.isNotEmpty()) {
                                    val index = currentGroup.rows.indexOf(inspected)
                                    val groupId = uiState.selectedGroupId
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        MainControl(
                                            label = stringResource(R.string.home_move_up),
                                            onClick = { onAction(MainAction.MoveServer(groupId, guid, -1)) },
                                            modifier = Modifier.weight(1f),
                                            tv = true,
                                            enabled = index > 0,
                                        )
                                        MainControl(
                                            label = stringResource(R.string.home_move_down),
                                            onClick = { onAction(MainAction.MoveServer(groupId, guid, 1)) },
                                            modifier = Modifier.weight(1f),
                                            tv = true,
                                            enabled = index < currentGroup.rows.lastIndex,
                                        )
                                    }
                                }
                            } else if (showImportShortcuts) {
                                MainImportShortcuts(
                                    tv = tv,
                                    hasCamera = hasCamera,
                                    onAction = onAction,
                                    onImportText = {
                                        importSubscription = false
                                        showImportDialog = true
                                    },
                                )
                            }
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.padding(horizontal = horizontalPadding).padding(top = 4.dp)) { connection() }
                        Box(Modifier.weight(1f).padding(horizontal = horizontalPadding)) { profiles() }
                        MainNavigation(
                            tv = false,
                            onNavigate = onNavigate,
                            onProfiles = { onAction(MainAction.LocateSelectedServer) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainNavigation(
    tv: Boolean,
    onNavigate: (MainDestination) -> Unit,
    modifier: Modifier = Modifier,
    onMenu: (() -> Unit)? = null,
    onProfiles: () -> Unit = {},
) {
    val destinations = listOf(MainDestination.Subscriptions, MainDestination.Settings)
    if (onMenu != null && !tv) {
        NavigationRail(modifier.fillMaxHeight(), containerColor = MaterialTheme.colorScheme.surface) {
            NavigationRailItem(selected = true, onClick = onProfiles,
                icon = { Icon(painterResource(R.drawable.ic_select_all_24dp), null) },
                label = { Text(stringResource(R.string.title_server)) })
            listOf(MainDestination.Subscriptions, MainDestination.PerAppProxy, MainDestination.Routing,
                MainDestination.UserAssets, MainDestination.Settings).forEach { destination ->
                key(destination) {
                    NavigationRailItem(selected = false, onClick = { onNavigate(destination) },
                        icon = { Icon(painterResource(destination.iconRes), null) },
                        label = { Text(stringResource(destination.labelRes), maxLines = 1) })
                }
            }
        }
    } else if (onMenu != null) {
        Column(modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MainControl(stringResource(R.string.title_server), onClick = onProfiles, tv = true, selected = true,
                modifier = Modifier.fillMaxWidth())
            destinations.forEach { destination ->
                key(destination) {
                    MainControl(stringResource(destination.labelRes), { onNavigate(destination) }, tv = true,
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }
    } else {
        NavigationBar(modifier, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            NavigationBarItem(
                selected = true,
                onClick = onProfiles,
                icon = { Icon(painterResource(R.drawable.ic_select_all_24dp), contentDescription = null) },
                label = { Text(stringResource(R.string.title_server)) },
            )
            destinations.forEach { destination ->
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigate(destination) },
                    icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                    label = { Text(stringResource(destination.labelRes)) },
                )
            }
        }
    }
}

@Composable
private fun MainImportShortcuts(
    tv: Boolean,
    hasCamera: Boolean,
    onAction: (MainAction) -> Unit,
    onImportText: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.home_import_options), style = MaterialTheme.typography.titleMedium)
        if (hasCamera) {
            MainControl(
                stringResource(R.string.menu_item_import_config_qrcode),
                { onAction(MainAction.ImportQRcode) },
                Modifier.fillMaxWidth(), tv = tv,
            )
        }
        MainControl(
            stringResource(R.string.menu_item_import_config_clipboard),
            { onAction(MainAction.ImportClipboard) },
            Modifier.fillMaxWidth(), tv = tv,
        )
        MainControl(
            stringResource(R.string.menu_item_import_config_local),
            { onAction(MainAction.ImportConfigLocal) },
            Modifier.fillMaxWidth(), tv = tv,
        )
        MainControl(
            stringResource(R.string.home_import_text), onImportText,
            Modifier.fillMaxWidth(), tv = tv,
        )
    }
}
