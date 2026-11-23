package com.v2ray.ang.ui

import android.app.Application
import androidx.activity.viewModels
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.ui.base.BaseViewModel
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.toArgb
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.v2ray.ang.ui.compose.AdaptiveContent
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.NavigationBarsSpacer
import com.v2ray.ang.ui.compose.SettingsMenuItem
import com.v2ray.ang.ui.compose.VersionInfoBlock
import com.v2ray.ang.util.Utils

class AboutActivity : BaseComponentActivity() {
    private val viewModel: AboutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        val versionText by viewModel.versionText.collectAsStateWithLifecycle()
        val licensesHtml by viewModel.licensesHtml.collectAsStateWithLifecycle()
        AboutScreen(
            versionText = versionText,
            licensesHtml = licensesHtml,
            onBackClick = { finish() },
            onTranslatorsClick = {
                startActivity(Intent(this, TranslatorsActivity::class.java))
            }
        )
    }
}

@Composable
fun AboutScreen(
    versionText: String,
    licensesHtml: String?,
    onBackClick: () -> Unit,
    onTranslatorsClick: () -> Unit
) {
    val context = LocalContext.current
    var showOssDialog by remember { mutableStateOf(false) }

    val appIdText = BuildConfig.APPLICATION_ID

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.title_about),
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        AdaptiveContent(Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)) {
        Column(
            modifier = Modifier
                .fillMaxSize()

                .verticalScroll(rememberScrollState())
        ) {
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_source_code_24dp),
                title = stringResource(R.string.title_source_code),
                onClick = { Utils.openUri(context, AppConfig.APP_URL) }
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.license_24px),
                title = stringResource(R.string.title_oss_license),
                onClick = { showOssDialog = true }
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_translate_24dp),
                title = stringResource(R.string.title_translators),
                onClick = onTranslatorsClick
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_feedback_24dp),
                title = stringResource(R.string.title_pref_feedback),
                onClick = { Utils.openUri(context, AppConfig.APP_ISSUES_URL) }
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_telegram_24dp),
                title = stringResource(R.string.title_tg_channel),
                onClick = { Utils.openUri(context, AppConfig.TG_CHANNEL_URL) }
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_privacy_24dp),
                title = stringResource(R.string.title_privacy_policy),
                onClick = { Utils.openUri(context, AppConfig.APP_PRIVACY_POLICY) }
            )
            VersionInfoBlock(
                versionText = versionText,
                appIdText = appIdText
            )
            NavigationBarsSpacer()
        }
            }
    }

    if (showOssDialog) {
        val colors = MaterialTheme.colorScheme
        fun cssColor(color: androidx.compose.ui.graphics.Color) = "#%06x".format(color.toArgb() and 0xffffff)
        val html = remember(licensesHtml, colors) {
            licensesHtml?.replace("</head>", "<style>html,body{background:${cssColor(colors.surface)}!important;color:${cssColor(colors.onSurface)}!important}a{color:${cssColor(colors.primary)}!important}pre{background:${cssColor(colors.surfaceContainer)}!important;color:${cssColor(colors.onSurface)}!important;white-space:pre-wrap;overflow-wrap:anywhere}</style></head>")
        }
        AlertDialog(
            onDismissRequest = { showOssDialog = false },
            title = { Text(stringResource(R.string.title_oss_license)) },
            text = {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(colors.surface.toArgb())
                            html?.let { loadDataWithBaseURL("file:///android_asset/", it, "text/html", "UTF-8", null) }
                        }
                    },
                    update = { webView ->
                        if (webView.tag != html && html != null) {
                            webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
                            webView.tag = html
                        }
                    },
                    onRelease = { it.destroy() },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp, max = 480.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showOssDialog = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(bottom = 60.dp)
        )
    }
}

class AboutViewModel(application: Application) : BaseViewModel(application) {
    private val _versionText = MutableStateFlow("v${BuildConfig.VERSION_NAME}")
    val versionText = _versionText.asStateFlow()
    private val _licensesHtml = MutableStateFlow<String?>(null)
    val licensesHtml = _licensesHtml.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    CoreNativeManager.getLibVersion() to application.assets.open("open_source_licenses.html").bufferedReader().use { it.readText() }
                }
                _versionText.value = "v${BuildConfig.VERSION_NAME} (${loaded.first})"
                _licensesHtml.value = loaded.second
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to load app information", error)
                toastError(R.string.toast_failure)
            }
        }
    }
}
