package com.v2ray.ang.ui.base

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.AppListIssue
import com.v2ray.ang.util.AppManagerUtil
import com.v2ray.ang.util.LogUtil

/** Owns the OS permission round trip shared by both installed-app selection screens. */
abstract class AppListActivity : BaseComponentActivity() {
    private var permissionRequested = false
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        reloadAppList()
    }

    override fun onResume() {
        super.onResume()
        reloadAppList()
    }

    protected abstract fun reloadAppList()

    protected fun requestAppListAccess(issue: AppListIssue?) {
        if (issue == AppListIssue.PermissionRequired && !permissionRequested) {
            permissionRequested = true
            permissionRequest.launch(AppManagerUtil.INSTALLED_APPS_PERMISSION)
        } else {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            } catch (error: SecurityException) {
                LogUtil.e(AppConfig.ANG_PACKAGE, "Installed app permission settings denied", error)
                toast(R.string.app_list_settings_unavailable)
            } catch (error: ActivityNotFoundException) {
                LogUtil.e(AppConfig.ANG_PACKAGE, "Unable to open installed app permission settings", error)
                toast(R.string.app_list_settings_unavailable)
            }
        }
    }
}
