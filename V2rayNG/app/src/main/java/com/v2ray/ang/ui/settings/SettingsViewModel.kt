package com.v2ray.ang.ui.settings

import android.app.Application
import android.content.Intent
import android.provider.Settings
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.root.RootManager
import com.v2ray.ang.ui.base.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.v2ray.ang.AppConfig.VPN
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsChangeManager
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.ui.compose.ThemeManager
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : BaseViewModel(application) {


    private val _preferences = MutableStateFlow(SettingsPreferences())
    internal val preferences = _preferences.asStateFlow()
    private val preferenceWrites = Mutex()

    fun loadPreferences() {
        viewModelScope.launch {
            try {
                val values = withContext(Dispatchers.IO) {
                    preferenceDefaults.mapValues { (key, default) ->
                        when (default) {
                            is Boolean -> MmkvManager.decodeSettingsBool(key, default)
                            is String -> MmkvManager.decodeSettingsString(key, default) ?: default
                            else -> error("Unsupported preference type")
                        }
                    }
                }
                _preferences.value = SettingsPreferences(ready = true, values = values)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to load settings", error)
                toastError(R.string.toast_failure)
            }
        }
    }

    internal fun <T : Any> preference(snapshot: SettingsPreferences, key: String, default: T): ReadWriteProperty<Any?, T> =
        object : ReadWriteProperty<Any?, T> {
            @Suppress("UNCHECKED_CAST")
            override fun getValue(thisRef: Any?, property: KProperty<*>): T = snapshot.values[key] as? T ?: default
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = setPreference(key, value)
        }

    private fun setPreference(key: String, value: Any) {
        val before = _preferences.value.values[key] ?: return
        if (before == value) return
        _preferences.update { it.copy(values = it.values + (key to value)) }
        viewModelScope.launch {
            preferenceWrites.withLock {
                try {
                    withContext(Dispatchers.IO) {
                        val saved = when (value) {
                            is Boolean -> MmkvManager.encodeSettings(key, value)
                            is String -> MmkvManager.encodeSettings(key, value)
                            else -> false
                        }
                        check(saved) { "Preference write failed: $key" }
                        when (key) {
                            AppConfig.PREF_DOUBLE_COLUMN_DISPLAY, AppConfig.PREF_GROUP_ALL_DISPLAY -> SettingsChangeManager.makeSetupGroupTab()
                            AppConfig.PREF_DYNAMIC_COLOR -> ThemeManager.setDynamicColorEnabled(value as Boolean)
                            AppConfig.PREF_UI_MODE_NIGHT -> ThemeManager.setThemeMode(value as String)
                        }
                    }
                    if (key == AppConfig.PREF_LANGUAGE) AppLocaleManager.setApplicationLanguage(value as String)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    LogUtil.e(AppConfig.TAG, "Failed to save setting: $key", error)
                    _preferences.update {
                        if (it.values[key] == value) it.copy(values = it.values + (key to before)) else it
                    }
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    fun setRootPreference(key: String, enabled: Boolean) {
        if (enabled && !RootManager.cachedRoot()) {
            checkAndRequestRoot { setPreference(key, true) }
        } else setPreference(key, enabled)
    }

    private val preferenceDefaults: Map<String, Any> = mapOf(
        AppConfig.PREF_LOCAL_DNS_ENABLED to false,
        AppConfig.PREF_FAKE_DNS_ENABLED to false,
        AppConfig.PREF_APPEND_HTTP_PROXY to false,
        AppConfig.PREF_VPN_DNS to "",
        AppConfig.PREF_VPN_BYPASS_LAN to AppConfig.DEFAULT_VPN_BYPASS_LAN,
        AppConfig.PREF_VPN_INTERFACE_ADDRESS_CONFIG_INDEX to "0",
        AppConfig.PREF_VPN_MTU to "",
        AppConfig.PREF_MUX_ENABLED to false,
        AppConfig.PREF_MUX_CONCURRENCY to "8",
        AppConfig.PREF_MUX_XUDP_CONCURRENCY to AppConfig.DEFAULT_MUX_XUDP_CONCURRENCY,
        AppConfig.PREF_MUX_XUDP_QUIC to "reject",
        AppConfig.PREF_FRAGMENT_ENABLED to false,
        AppConfig.PREF_FRAGMENT_PACKETS to "tlshello",
        AppConfig.PREF_FRAGMENT_LENGTH to "50-100",
        AppConfig.PREF_FRAGMENT_INTERVAL to "10-20",
        AppConfig.PREF_FRAGMENT_MAXSPLIT to "10",
        AppConfig.PREF_OBSERVATORY_LEAST_PING_INTERVAL to AppConfig.OBSERVATORY_LEAST_PING_INTERVAL,
        AppConfig.PREF_OBSERVATORY_LEAST_LOAD_INTERVAL to AppConfig.OBSERVATORY_LEAST_LOAD_INTERVAL,
        AppConfig.PREF_OBSERVATORY_LEAST_LOAD_METHOD to AppConfig.OBSERVATORY_LEAST_LOAD_METHOD,
        AppConfig.PREF_OBSERVATORY_LEAST_LOAD_SAMPLING to AppConfig.OBSERVATORY_LEAST_LOAD_SAMPLING,
        AppConfig.PREF_OBSERVATORY_LEAST_LOAD_TIMEOUT to AppConfig.OBSERVATORY_LEAST_LOAD_TIMEOUT,
        AppConfig.PREF_MODE to VPN,
        AppConfig.PREF_ROOT_MODE_ENABLE to false,
        AppConfig.PREF_ROOT_LAN_SHARING to false,
        AppConfig.PREF_HEV_TUNNEL_LOGLEVEL to AppConfig.DEFAULT_HEV_TUNNEL_LOGLEVEL,
        AppConfig.PREF_HEV_TUNNEL_RW_TIMEOUT to "",
        AppConfig.PREF_USE_HEV_TUNNEL to true,
        AppConfig.PREF_ENABLE_LOCAL_PROXY to true,
        AppConfig.PREF_SOCKS_PORT to "",
        AppConfig.PREF_DYNAMIC_SOCKS_PORT to false,
        AppConfig.PREF_SOCKS_USERNAME to "",
        AppConfig.PREF_SOCKS_PASSWORD to "",
        AppConfig.PREF_SOCKS_ENABLE_UDP to AppConfig.DEFAULT_SOCKS_ENABLE_UDP,
        AppConfig.PREF_PROXY_SHARING to false,
        AppConfig.PREF_SPEED_ENABLED to false,
        AppConfig.PREF_CONFIRM_REMOVE to false,
        AppConfig.PREF_DOUBLE_COLUMN_DISPLAY to false,
        AppConfig.PREF_GROUP_ALL_DISPLAY to false,
        AppConfig.PREF_UI_MODE_NIGHT to ThemeManager.DEFAULT_THEME_MODE,
        AppConfig.PREF_DYNAMIC_COLOR to ThemeManager.DEFAULT_DYNAMIC_COLOR,
        AppConfig.PREF_IPV6_ENABLED to false,
        AppConfig.PREF_PREFER_IPV6 to false,
        AppConfig.PREF_SNIFFING_ENABLED to true,
        AppConfig.PREF_ROUTE_ONLY_ENABLED to false,
        AppConfig.PREF_REMOTE_DNS to "",
        AppConfig.PREF_DOMESTIC_DNS to "",
        AppConfig.PREF_DNS_HOSTS to "",
        AppConfig.PREF_LOGLEVEL to "warning",
        AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD to AppConfig.DEFAULT_OUTBOUND_DOMAIN_RESOLVE_METHOD,
        AppConfig.PREF_IS_BOOTED to false,
        AppConfig.PREF_DELAY_TEST_URL to "",
        AppConfig.PREF_REAL_PING_CONCURRENCY to "16",
        AppConfig.PREF_IP_API_URL to "",
        AppConfig.PREF_LANGUAGE to "auto"
    )

    private val _systemVpnSettingsAvailable = MutableStateFlow(false)
    val systemVpnSettingsAvailable = _systemVpnSettingsAvailable.asStateFlow()

    suspend fun refreshSystemVpnSettingsAvailability() {
        _systemVpnSettingsAvailable.value = withContext(Dispatchers.IO) {
            // Android exposes the VPN page, not a direct link to the Always-on VPN switch.
            Intent(Settings.ACTION_VPN_SETTINGS).resolveActivity(getApplication<Application>().packageManager) != null
        }
    }

    /**
     * Checks for root access and requests it if necessary.
     * Updates [isLoading] during the process.
     */
    fun checkAndRequestRoot(onSuccess: () -> Unit) {
        launchLoading {
            val hasRoot = withContext(Dispatchers.IO) {
                RootManager.refresh()
            }
            if (hasRoot) {
                onSuccess()
            } else {
                toastError(R.string.toast_root_required)
            }
        }
    }

    /**
     * Validates if the given string is a valid observatory duration.
     * Shows error toast if invalid.
     * @return The trimmed value if valid, null otherwise.
     */
    fun validateObservatoryDuration(value: String): String? {
        val duration = value.trim()
        return if (AppConfig.OBSERVATORY_DURATION_PATTERN.matches(duration)) {
            duration
        } else {
            toastError(R.string.toast_invalid_observatory_duration)
            null
        }
    }

    /**
     * Validates if the given string is a valid observatory sampling value.
     * Shows error toast if invalid.
     * @return The value if valid, null otherwise.
     */
    fun validateObservatorySampling(value: String): String? {
        val sampling = value.trim().toIntOrNull()?.takeIf { it > 0 }
        return if (sampling != null) {
            sampling.toString()
        } else {
            toastError(R.string.toast_invalid_observatory_sampling)
            null
        }
    }
}

internal data class SettingsPreferences(val ready: Boolean = false, val values: Map<String, Any> = emptyMap())
