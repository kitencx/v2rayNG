package com.v2ray.ang.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.key
import androidx.annotation.StringRes
import com.v2ray.ang.ui.compose.AdaptiveContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.VPN
import com.v2ray.ang.R
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.CollapsiblePreferenceGroupHeader
import com.v2ray.ang.ui.compose.NavigationBarsSpacer
import com.v2ray.ang.ui.compose.SettingsEditItem
import com.v2ray.ang.ui.compose.SettingsListItem
import com.v2ray.ang.ui.compose.SettingsMenuItem
import com.v2ray.ang.ui.compose.SettingsSwitchItem
import com.v2ray.ang.ui.compose.ThemeManager
import com.v2ray.ang.ui.compose.verticalScrollbar
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.launch

class SettingsActivity : BaseComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadPreferences()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.refreshSystemVpnSettingsAvailability()
            }
        }
    }

    private fun openSystemVpnSettings() {
        try {
            startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
        } catch (error: ActivityNotFoundException) {
            reportSystemVpnSettingsFailure(error)
        } catch (error: SecurityException) {
            reportSystemVpnSettingsFailure(error)
        }
    }

    private fun reportSystemVpnSettingsFailure(error: RuntimeException) {
        LogUtil.e(AppConfig.TAG, "Cannot open system VPN settings", error)
        toastError(R.string.toast_system_vpn_settings_unavailable)
    }

    @Composable
    override fun ScreenContent() {
        SettingsScreen(
            viewModel = viewModel,
            onBackClick = { finish() },
            onModeHelpClicked = { Utils.openUri(this, AppConfig.APP_WIKI_MODE) },
            onSystemVpnSettingsClicked = ::openSystemVpnSettings
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onModeHelpClicked: () -> Unit,
    onSystemVpnSettingsClicked: () -> Unit
) {
    val scrollState = rememberScrollState()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    var selectedSection by rememberSaveable { mutableStateOf(SettingsSection.Interface) }
    LaunchedEffect(selectedSection) { scrollState.scrollTo(0) }
    if (!preferences.ready) {
        androidx.compose.material3.CircularProgressIndicator(Modifier.padding(24.dp))
        return
    }
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val systemVpnSettingsAvailable by viewModel.systemVpnSettingsAvailable.collectAsStateWithLifecycle()
    var uiSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var vpnSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var coreSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var muxSettingsExpanded by rememberSaveable { mutableStateOf(false) }
    var fragmentSettingsExpanded by rememberSaveable { mutableStateOf(false) }
    var observatorySettingsExpanded by rememberSaveable { mutableStateOf(false) }
    var advancedSettingsExpanded by rememberSaveable { mutableStateOf(false) }
    var modeSettingsExpanded by rememberSaveable { mutableStateOf(false) }

    var localDns by viewModel.preference(preferences, AppConfig.PREF_LOCAL_DNS_ENABLED, false)
    var fakeDns by viewModel.preference(preferences, AppConfig.PREF_FAKE_DNS_ENABLED, false)
    var appendHttpProxy by viewModel.preference(preferences, AppConfig.PREF_APPEND_HTTP_PROXY, false)
    var vpnDns by viewModel.preference(preferences, AppConfig.PREF_VPN_DNS, "")
    var vpnBypassLan by viewModel.preference(preferences, AppConfig.PREF_VPN_BYPASS_LAN, AppConfig.DEFAULT_VPN_BYPASS_LAN)
    var vpnInterfaceAddress by viewModel.preference(preferences, AppConfig.PREF_VPN_INTERFACE_ADDRESS_CONFIG_INDEX, "0")
    var vpnMtu by viewModel.preference(preferences, AppConfig.PREF_VPN_MTU, "")

    var mux by viewModel.preference(preferences, AppConfig.PREF_MUX_ENABLED, false)
    var muxConcurrency by viewModel.preference(preferences, AppConfig.PREF_MUX_CONCURRENCY, "8")
    var muxXudpConcurrency by viewModel.preference(preferences, AppConfig.PREF_MUX_XUDP_CONCURRENCY, AppConfig.DEFAULT_MUX_XUDP_CONCURRENCY)
    var muxXudpQuic by viewModel.preference(preferences, AppConfig.PREF_MUX_XUDP_QUIC, "reject")

    var fragment by viewModel.preference(preferences, AppConfig.PREF_FRAGMENT_ENABLED, false)
    var fragmentPackets by viewModel.preference(preferences, AppConfig.PREF_FRAGMENT_PACKETS, "tlshello")
    var fragmentLength by viewModel.preference(preferences, AppConfig.PREF_FRAGMENT_LENGTH, "50-100")
    var fragmentInterval by viewModel.preference(preferences, AppConfig.PREF_FRAGMENT_INTERVAL, "10-20")
    var fragmentMaxSplit by viewModel.preference(preferences, AppConfig.PREF_FRAGMENT_MAXSPLIT, "10")
    var observatoryLeastPingInterval by viewModel.preference(preferences, AppConfig.PREF_OBSERVATORY_LEAST_PING_INTERVAL, AppConfig.OBSERVATORY_LEAST_PING_INTERVAL)
    var observatoryLeastLoadInterval by viewModel.preference(preferences, AppConfig.PREF_OBSERVATORY_LEAST_LOAD_INTERVAL, AppConfig.OBSERVATORY_LEAST_LOAD_INTERVAL)
    var observatoryLeastLoadMethod by viewModel.preference(preferences, AppConfig.PREF_OBSERVATORY_LEAST_LOAD_METHOD, AppConfig.OBSERVATORY_LEAST_LOAD_METHOD)
    var observatoryLeastLoadSampling by viewModel.preference(preferences, AppConfig.PREF_OBSERVATORY_LEAST_LOAD_SAMPLING, AppConfig.OBSERVATORY_LEAST_LOAD_SAMPLING)
    var observatoryLeastLoadTimeout by viewModel.preference(preferences, AppConfig.PREF_OBSERVATORY_LEAST_LOAD_TIMEOUT, AppConfig.OBSERVATORY_LEAST_LOAD_TIMEOUT)

    var mode by viewModel.preference(preferences, AppConfig.PREF_MODE, VPN)
    var enableRootMode by viewModel.preference(preferences, AppConfig.PREF_ROOT_MODE_ENABLE, false)
    var lanSharing by viewModel.preference(preferences, AppConfig.PREF_ROOT_LAN_SHARING, false)

    var hevTunLogLevel by viewModel.preference(preferences, AppConfig.PREF_HEV_TUNNEL_LOGLEVEL, AppConfig.DEFAULT_HEV_TUNNEL_LOGLEVEL)
    var hevTunRwTimeout by viewModel.preference(preferences, AppConfig.PREF_HEV_TUNNEL_RW_TIMEOUT, "")
    var useHevTun by viewModel.preference(preferences, AppConfig.PREF_USE_HEV_TUNNEL, true)

    var enableLocalProxy by viewModel.preference(preferences, AppConfig.PREF_ENABLE_LOCAL_PROXY, true)
    var socksPort by viewModel.preference(preferences, AppConfig.PREF_SOCKS_PORT, "")
    var dynamicSocksPort by viewModel.preference(preferences, AppConfig.PREF_DYNAMIC_SOCKS_PORT, false)
    var socksUsername by viewModel.preference(preferences, AppConfig.PREF_SOCKS_USERNAME, "")
    var socksPassword by viewModel.preference(preferences, AppConfig.PREF_SOCKS_PASSWORD, "")
    var socksEnableUdp by viewModel.preference(preferences, AppConfig.PREF_SOCKS_ENABLE_UDP, AppConfig.DEFAULT_SOCKS_ENABLE_UDP)
    var proxySharing by viewModel.preference(preferences, AppConfig.PREF_PROXY_SHARING, false)

    var speedEnabled by viewModel.preference(preferences, AppConfig.PREF_SPEED_ENABLED, false)
    var confirmRemove by viewModel.preference(preferences, AppConfig.PREF_CONFIRM_REMOVE, false)
    var doubleColumnDisplay by viewModel.preference(preferences, AppConfig.PREF_DOUBLE_COLUMN_DISPLAY, false)
    var groupAllDisplay by viewModel.preference(preferences, AppConfig.PREF_GROUP_ALL_DISPLAY, false)
    var language by viewModel.preference(preferences, AppConfig.PREF_LANGUAGE, "auto")
    var uiModeNight by viewModel.preference(preferences, AppConfig.PREF_UI_MODE_NIGHT, ThemeManager.DEFAULT_THEME_MODE)
    var dynamicColor by viewModel.preference(preferences, AppConfig.PREF_DYNAMIC_COLOR, ThemeManager.DEFAULT_DYNAMIC_COLOR)

    var ipv6Enabled by viewModel.preference(preferences, AppConfig.PREF_IPV6_ENABLED, false)
    var preferIpv6 by viewModel.preference(preferences, AppConfig.PREF_PREFER_IPV6, false)
    var sniffingEnabled by viewModel.preference(preferences, AppConfig.PREF_SNIFFING_ENABLED, true)
    var routeOnlyEnabled by viewModel.preference(preferences, AppConfig.PREF_ROUTE_ONLY_ENABLED, false)
    var remoteDns by viewModel.preference(preferences, AppConfig.PREF_REMOTE_DNS, "")
    var domesticDns by viewModel.preference(preferences, AppConfig.PREF_DOMESTIC_DNS, "")
    var dnsHosts by viewModel.preference(preferences, AppConfig.PREF_DNS_HOSTS, "")
    var coreLogLevel by viewModel.preference(preferences, AppConfig.PREF_LOGLEVEL, "warning")
    var outboundResolveMethod by viewModel.preference(preferences, AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD, AppConfig.DEFAULT_OUTBOUND_DOMAIN_RESOLVE_METHOD)

    var isBooted by viewModel.preference(preferences, AppConfig.PREF_IS_BOOTED, false)
    var delayTestUrl by viewModel.preference(preferences, AppConfig.PREF_DELAY_TEST_URL, "")
    var realPingConcurrency by viewModel.preference(preferences, AppConfig.PREF_REAL_PING_CONCURRENCY, "16")
    var ipApiUrl by viewModel.preference(preferences, AppConfig.PREF_IP_API_URL, "")

    val isVpn = mode == VPN
    val hevTunEnabled = isVpn && useHevTun
    val localProxyForced = hevTunEnabled
    val effectiveLocalProxy = enableLocalProxy || localProxyForced
    val muxXudpConcurrencyInt = muxXudpConcurrency.toIntOrNull() ?: AppConfig.DEFAULT_MUX_XUDP_CONCURRENCY.toInt()

    val dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val languageEntries = stringArrayResource(R.array.language_select).toList()
    val languageValues = stringArrayResource(R.array.language_select_value).toList()
    val uiModeNightEntries = stringArrayResource(R.array.ui_mode_night).toList()
    val uiModeNightValues = stringArrayResource(R.array.ui_mode_night_value).toList()
    val bypassLanEntries = stringArrayResource(R.array.vpn_bypass_lan).toList()
    val bypassLanValues = stringArrayResource(R.array.vpn_bypass_lan_value).toList()
    val interfaceAddrEntries = stringArrayResource(R.array.vpn_interface_address).toList()
    val interfaceAddrValues = stringArrayResource(R.array.vpn_interface_address_value).toList()
    val hevLogEntries = stringArrayResource(R.array.hev_tunnel_loglevel).toList()
    val hevLogValues = stringArrayResource(R.array.hev_tunnel_loglevel).toList()
    val coreLogLevelEntries = stringArrayResource(R.array.core_loglevel).toList()
    val coreLogLevelValues = stringArrayResource(R.array.core_loglevel).toList()
    val outboundResolveEntries = stringArrayResource(R.array.outbound_domain_resolve_method).toList()
    val outboundResolveValues = stringArrayResource(R.array.outbound_domain_resolve_method_value).toList()
    val xudpQuicEntries = stringArrayResource(R.array.mux_xudp_quic_entries).toList()
    val xudpQuicValues = stringArrayResource(R.array.mux_xudp_quic_value).toList()
    val fragmentPacketsEntries = stringArrayResource(R.array.fragment_packets).toList()
    val fragmentPacketsValues = stringArrayResource(R.array.fragment_packets).toList()
    val observatoryLeastLoadMethodEntries = stringArrayResource(R.array.observatory_least_load_method).toList()
    val observatoryLeastLoadMethodValues = stringArrayResource(R.array.observatory_least_load_method).toList()
    val modeEntries = stringArrayResource(R.array.mode_entries).toList()
    val modeValues = stringArrayResource(R.array.mode_value).toList()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.title_settings),
                onBackClick = onBackClick,
                isLoading = isLoading
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)) {
            val wide = maxWidth >= 840.dp
            Row(Modifier.fillMaxSize()) {
                if (wide) {
                    Column(Modifier.width(220.dp).verticalScroll(rememberScrollState()).padding(12.dp)) {
                        SettingsSection.entries.forEach { section ->
                            key(section) {
                                NavigationDrawerItem(
                                    label = { Text(stringResource(section.title)) },
                                    selected = selectedSection == section,
                                    onClick = { selectedSection = section },
                                )
                            }
                        }
                    }
                }
                AdaptiveContent(Modifier.weight(1f), maxWidth = 720.dp) {
                Column(
                    modifier = Modifier.fillMaxSize().imePadding()
                        .verticalScrollbar(scrollState).verticalScroll(scrollState)
                        .padding(vertical = 8.dp),
                ) {
            if (!wide || selectedSection == SettingsSection.Interface) {
            if (wide) {
                com.v2ray.ang.ui.compose.PreferenceGroupHeader(stringResource(R.string.title_ui_settings))
            } else {
                CollapsiblePreferenceGroupHeader(
                    title = stringResource(R.string.title_ui_settings), expanded = uiSettingsExpanded,
                    onExpandedChange = { uiSettingsExpanded = it },
                )
            }
            if (wide || uiSettingsExpanded) {
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_speed_enabled),
                    summary = stringResource(R.string.summary_pref_speed_enabled),
                    checked = speedEnabled,
                    onCheckedChange = { speedEnabled = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_confirm_remove),
                    summary = stringResource(R.string.summary_pref_confirm_remove),
                    checked = confirmRemove,
                    onCheckedChange = { confirmRemove = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_double_column_display),
                    summary = stringResource(R.string.summary_pref_double_column_display),
                    checked = doubleColumnDisplay,
                    onCheckedChange = {
                        doubleColumnDisplay = it
                    }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_group_all_display),
                    summary = stringResource(R.string.summary_pref_group_all_display),
                    checked = groupAllDisplay,
                    onCheckedChange = {
                        groupAllDisplay = it
                    }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_dynamic_color),
                    summary = stringResource(R.string.summary_pref_dynamic_color),
                    checked = dynamicColor,
                    enabled = dynamicColorSupported,
                    onCheckedChange = {
                        dynamicColor = it
                    }
                )
                SettingsListItem(
                    title = stringResource(R.string.title_language),
                    entries = languageEntries,
                    values = languageValues,
                    selectedValue = language,
                    onSelected = {
                        language = it
                    }
                )
                SettingsListItem(
                    title = stringResource(R.string.title_pref_ui_mode_night),
                    entries = uiModeNightEntries,
                    values = uiModeNightValues,
                    selectedValue = uiModeNight,
                    onSelected = {
                        uiModeNight = it
                    }
                )
            }

            }
            if (!wide || selectedSection == SettingsSection.Vpn) {
            if (wide) {
                com.v2ray.ang.ui.compose.PreferenceGroupHeader(stringResource(R.string.title_vpn_settings))
            } else {
                CollapsiblePreferenceGroupHeader(
                    title = stringResource(R.string.title_vpn_settings), expanded = vpnSettingsExpanded,
                    onExpandedChange = { vpnSettingsExpanded = it },
                )
            }
            if (wide || vpnSettingsExpanded) {
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_ipv6_enabled),
                    summary = stringResource(R.string.summary_pref_ipv6_enabled),
                    checked = ipv6Enabled,
                    onCheckedChange = { ipv6Enabled = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_prefer_ipv6),
                    summary = stringResource(R.string.summary_pref_prefer_ipv6),
                    checked = preferIpv6,
                    onCheckedChange = { preferIpv6 = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_local_dns_enabled),
                    summary = stringResource(R.string.summary_pref_local_dns_enabled),
                    checked = localDns,
                    enabled = isVpn,
                    onCheckedChange = { localDns = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_fake_dns_enabled),
                    summary = stringResource(R.string.summary_pref_fake_dns_enabled),
                    checked = fakeDns,
                    enabled = isVpn && localDns,
                    onCheckedChange = { fakeDns = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_vpn_dns),
                    value = vpnDns,
                    enabled = isVpn && !localDns,
                    onValueChanged = { vpnDns = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_append_http_proxy),
                    summary = stringResource(R.string.summary_pref_append_http_proxy),
                    checked = appendHttpProxy,
                    enabled = effectiveLocalProxy,
                    onCheckedChange = { appendHttpProxy = it }
                )
                SettingsListItem(
                    title = stringResource(R.string.title_pref_vpn_bypass_lan),
                    entries = bypassLanEntries,
                    values = bypassLanValues,
                    selectedValue = vpnBypassLan,
                    enabled = isVpn,
                    onSelected = { vpnBypassLan = it }
                )
                SettingsListItem(
                    title = stringResource(R.string.title_pref_vpn_interface_address),
                    entries = interfaceAddrEntries,
                    values = interfaceAddrValues,
                    selectedValue = vpnInterfaceAddress,
                    enabled = isVpn,
                    onSelected = { vpnInterfaceAddress = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_vpn_mtu),
                    value = vpnMtu,
                    enabled = isVpn,
                    keyboardNumber = true,
                    onValueChanged = { vpnMtu = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_use_hev_tunnel),
                    summary = stringResource(R.string.summary_pref_use_hev_tunnel),
                    checked = useHevTun,
                    enabled = isVpn,
                    onCheckedChange = {
                        useHevTun = it
                        if (it && !enableLocalProxy) {
                            enableLocalProxy = true
                        }
                    }
                )
                SettingsListItem(
                    title = stringResource(R.string.title_pref_hev_tunnel_loglevel),
                    entries = hevLogEntries,
                    values = hevLogValues,
                    selectedValue = hevTunLogLevel,
                    enabled = hevTunEnabled,
                    onSelected = { hevTunLogLevel = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_hev_tunnel_rw_timeout),
                    value = hevTunRwTimeout,
                    enabled = hevTunEnabled,
                    keyboardNumber = true,
                    onValueChanged = { hevTunRwTimeout = it }
                )
            }

            }
            if (!wide || selectedSection == SettingsSection.Core) {
            if (wide) {
                com.v2ray.ang.ui.compose.PreferenceGroupHeader(stringResource(R.string.title_core_settings))
            } else {
                CollapsiblePreferenceGroupHeader(
                    title = stringResource(R.string.title_core_settings), expanded = coreSettingsExpanded,
                    onExpandedChange = { coreSettingsExpanded = it },
                )
            }
            if (wide || coreSettingsExpanded) {
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_sniffing_enabled),
                    summary = stringResource(R.string.summary_pref_sniffing_enabled),
                    checked = sniffingEnabled,
                    onCheckedChange = { sniffingEnabled = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_route_only_enabled),
                    summary = stringResource(R.string.summary_pref_route_only_enabled),
                    checked = routeOnlyEnabled,
                    onCheckedChange = { routeOnlyEnabled = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_enable_local_proxy),
                    summary = stringResource(R.string.summary_pref_enable_local_proxy),
                    checked = enableLocalProxy,
                    enabled = !localProxyForced,
                    onCheckedChange = {
                        if (!localProxyForced) {
                            enableLocalProxy = it
                            if (!it && appendHttpProxy) {
                                appendHttpProxy = false
                            }
                        }
                    }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_proxy_sharing_enabled),
                    summary = stringResource(R.string.summary_pref_proxy_sharing_enabled),
                    checked = proxySharing,
                    enabled = effectiveLocalProxy,
                    onCheckedChange = { proxySharing = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_dynamic_socks_port),
                    summary = stringResource(R.string.summary_pref_dynamic_socks_port),
                    checked = dynamicSocksPort,
                    enabled = effectiveLocalProxy,
                    onCheckedChange = { dynamicSocksPort = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_socks_port),
                    value = socksPort,
                    enabled = effectiveLocalProxy && !dynamicSocksPort,
                    keyboardNumber = true,
                    onValueChanged = { socksPort = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_socks_username),
                    value = socksUsername,
                    enabled = effectiveLocalProxy,
                    onValueChanged = { socksUsername = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_socks_password),
                    value = socksPassword,
                    enabled = effectiveLocalProxy,
                    isPassword = true,
                    onValueChanged = { socksPassword = it }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_socks_enable_udp),
                    summary = stringResource(R.string.summary_pref_socks_enable_udp),
                    checked = socksEnableUdp,
                    enabled = effectiveLocalProxy,
                    onCheckedChange = { socksEnableUdp = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_remote_dns),
                    value = remoteDns,
                    onValueChanged = { remoteDns = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_domestic_dns),
                    value = domesticDns,
                    onValueChanged = { domesticDns = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_dns_hosts),
                    value = dnsHosts,
                    onValueChanged = { dnsHosts = it }
                )
                SettingsListItem(
                    title = stringResource(R.string.title_core_loglevel),
                    entries = coreLogLevelEntries,
                    values = coreLogLevelValues,
                    selectedValue = coreLogLevel,
                    onSelected = { coreLogLevel = it }
                )
                SettingsListItem(
                    title = stringResource(R.string.title_outbound_domain_resolve_method),
                    entries = outboundResolveEntries,
                    values = outboundResolveValues,
                    selectedValue = outboundResolveMethod,
                    onSelected = { outboundResolveMethod = it }
                )
            }

            }
            if (!wide || selectedSection == SettingsSection.Mux) {
            if (wide) {
                com.v2ray.ang.ui.compose.PreferenceGroupHeader(stringResource(R.string.title_mux_settings))
            } else {
                CollapsiblePreferenceGroupHeader(
                    title = stringResource(R.string.title_mux_settings), expanded = muxSettingsExpanded,
                    onExpandedChange = { muxSettingsExpanded = it },
                )
            }
            if (wide || muxSettingsExpanded) {
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_mux_enabled),
                    summary = stringResource(R.string.summary_pref_mux_enabled),
                    checked = mux,
                    onCheckedChange = { mux = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_mux_concurrency),
                    value = muxConcurrency,
                    enabled = mux,
                    keyboardNumber = true,
                    onValueChanged = { muxConcurrency = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_mux_xudp_concurrency),
                    value = muxXudpConcurrency,
                    enabled = mux,
                    keyboardNumber = true,
                    onValueChanged = { muxXudpConcurrency = it }
                )
                SettingsListItem(
                    title = stringResource(R.string.title_pref_mux_xudp_quic),
                    entries = xudpQuicEntries,
                    values = xudpQuicValues,
                    selectedValue = muxXudpQuic,
                    enabled = mux && muxXudpConcurrencyInt >= 0,
                    onSelected = { muxXudpQuic = it }
                )
            }

            }
            if (!wide || selectedSection == SettingsSection.Fragment) {
            if (wide) {
                com.v2ray.ang.ui.compose.PreferenceGroupHeader(stringResource(R.string.title_fragment_settings))
            } else {
                CollapsiblePreferenceGroupHeader(
                    title = stringResource(R.string.title_fragment_settings), expanded = fragmentSettingsExpanded,
                    onExpandedChange = { fragmentSettingsExpanded = it },
                )
            }
            if (wide || fragmentSettingsExpanded) {
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_fragment_enabled),
                    checked = fragment,
                    onCheckedChange = { fragment = it }
                )
                SettingsListItem(
                    title = stringResource(R.string.title_pref_fragment_packets),
                    entries = fragmentPacketsEntries,
                    values = fragmentPacketsValues,
                    selectedValue = fragmentPackets,
                    enabled = fragment,
                    onSelected = { fragmentPackets = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_fragment_length),
                    value = fragmentLength,
                    enabled = fragment,
                    onValueChanged = { fragmentLength = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_fragment_interval),
                    value = fragmentInterval,
                    enabled = fragment,
                    onValueChanged = { fragmentInterval = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_fragment_maxsplit),
                    value = fragmentMaxSplit,
                    enabled = fragment,
                    keyboardNumber = true,
                    onValueChanged = { fragmentMaxSplit = it }
                )
            }

            }
            if (!wide || selectedSection == SettingsSection.Observatory) {
            if (wide) {
                com.v2ray.ang.ui.compose.PreferenceGroupHeader(stringResource(R.string.title_observatory_settings))
            } else {
                CollapsiblePreferenceGroupHeader(
                    title = stringResource(R.string.title_observatory_settings), expanded = observatorySettingsExpanded,
                    onExpandedChange = { observatorySettingsExpanded = it },
                )
            }
            if (wide || observatorySettingsExpanded) {
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_observatory_least_ping_interval),
                    value = observatoryLeastPingInterval,
                    onValueChanged = {
                        viewModel.validateObservatoryDuration(it)?.let { value ->
                            observatoryLeastPingInterval = value
                        }
                    }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_observatory_least_load_interval),
                    value = observatoryLeastLoadInterval,
                    onValueChanged = {
                        viewModel.validateObservatoryDuration(it)?.let { value ->
                            observatoryLeastLoadInterval = value
                        }
                    }
                )
                SettingsListItem(
                    title = stringResource(R.string.title_pref_observatory_least_load_method),
                    entries = observatoryLeastLoadMethodEntries,
                    values = observatoryLeastLoadMethodValues,
                    selectedValue = observatoryLeastLoadMethod,
                    onSelected = { observatoryLeastLoadMethod = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_observatory_least_load_sampling),
                    value = observatoryLeastLoadSampling,
                    keyboardNumber = true,
                    onValueChanged = {
                        viewModel.validateObservatorySampling(it)?.let { value ->
                            observatoryLeastLoadSampling = value
                        }
                    }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_observatory_least_load_timeout),
                    value = observatoryLeastLoadTimeout,
                    onValueChanged = {
                        viewModel.validateObservatoryDuration(it)?.let { value ->
                            observatoryLeastLoadTimeout = value
                        }
                    }
                )
            }

            }
            if (!wide || selectedSection == SettingsSection.Advanced) {
            if (wide) {
                com.v2ray.ang.ui.compose.PreferenceGroupHeader(stringResource(R.string.title_advanced))
            } else {
                CollapsiblePreferenceGroupHeader(
                    title = stringResource(R.string.title_advanced), expanded = advancedSettingsExpanded,
                    onExpandedChange = { advancedSettingsExpanded = it },
                )
            }
            if (wide || advancedSettingsExpanded) {
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_is_booted),
                    summary = stringResource(R.string.summary_pref_is_booted),
                    checked = isBooted,
                    onCheckedChange = { isBooted = it }
                )
                if (systemVpnSettingsAvailable) {
                    SettingsMenuItem(
                        title = stringResource(R.string.title_system_vpn_settings),
                        subtitle = stringResource(R.string.summary_system_vpn_settings),
                        onClick = onSystemVpnSettingsClicked
                    )
                }
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_delay_test_url),
                    value = delayTestUrl,
                    onValueChanged = { delayTestUrl = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_real_ping_concurrency),
                    value = realPingConcurrency,
                    keyboardNumber = true,
                    onValueChanged = { realPingConcurrency = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_ip_api_url),
                    value = ipApiUrl,
                    onValueChanged = { ipApiUrl = it }
                )
            }

            }
            if (!wide || selectedSection == SettingsSection.Mode) {
            if (wide) {
                com.v2ray.ang.ui.compose.PreferenceGroupHeader(stringResource(R.string.title_mode_settings))
            } else {
                CollapsiblePreferenceGroupHeader(
                    title = stringResource(R.string.title_mode_settings), expanded = modeSettingsExpanded,
                    onExpandedChange = { modeSettingsExpanded = it },
                )
            }
            if (wide || modeSettingsExpanded) {
                SettingsListItem(
                    title = stringResource(R.string.title_mode),
                    entries = modeEntries,
                    values = modeValues,
                    selectedValue = mode,
                    onSelected = { mode = it }
                )
                SettingsMenuItem(
                    title = stringResource(R.string.title_mode_help),
                    onClick = onModeHelpClicked
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_root_mode_enabled),
                    summary = stringResource(R.string.summary_root_mode_enabled),
                    checked = enableRootMode,
                    onCheckedChange = { viewModel.setRootPreference(AppConfig.PREF_ROOT_MODE_ENABLE, it) }
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.title_root_lan_sharing),
                    summary = stringResource(R.string.summary_root_lan_sharing),
                    checked = lanSharing,
                    onCheckedChange = { viewModel.setRootPreference(AppConfig.PREF_ROOT_LAN_SHARING, it) }
                )
            }

            }
            Spacer(modifier = Modifier.height(24.dp))
            NavigationBarsSpacer()
                }
                }
            }
        }
    }
}

private enum class SettingsSection(@StringRes val title: Int) {
    Interface(R.string.settings_section_interface),
    Vpn(R.string.settings_section_vpn),
    Core(R.string.settings_section_core),
    Mux(R.string.settings_section_mux),
    Fragment(R.string.settings_section_fragment),
    Observatory(R.string.settings_section_observatory),
    Advanced(R.string.settings_section_advanced),
    Mode(R.string.settings_section_mode)
}
