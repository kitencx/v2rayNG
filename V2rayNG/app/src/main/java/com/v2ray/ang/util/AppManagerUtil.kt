package com.v2ray.ang.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.v2ray.ang.dto.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppManagerUtil {
    /**
     * Load the list of network applications.
     *
     * @param context The context to use.
     * @return A list of AppInfo objects representing the network applications.
     */
    suspend fun loadNetworkAppList(context: Context): ArrayList<AppInfo> =
        withContext(Dispatchers.IO) {
            val packageManager = context.packageManager
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                installedPackagesBefore33(packageManager)
            }
            if (hasRestrictedAppVisibility(packages.map { it.packageName }, context.packageName)) {
                throw AppListAccessException(
                    if (supportsInstalledAppsPermission(context)) AppListIssue.PermissionRequired
                    else AppListIssue.Restricted
                )
            }
            val apps = ArrayList<AppInfo>()

            for (pkg in packages) {
                val applicationInfo = pkg.applicationInfo ?: continue

                val appName = applicationInfo.loadLabel(packageManager).toString()
                val isSystemApp = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM > 0

                val appInfo = AppInfo(appName, pkg.packageName, isSystemApp, 0)
                apps.add(appInfo)
            }

            return@withContext apps
        }

    const val INSTALLED_APPS_PERMISSION = "com.android.permission.GET_INSTALLED_APPS"

    // MIUI 13+ can return only the calling package when its extra permission is denied.
    // Remove this adapter when Xiaomi no longer requires GET_INSTALLED_APPS.
    private fun supportsInstalledAppsPermission(context: Context): Boolean = try {
        context.packageManager.getPermissionInfo(INSTALLED_APPS_PERMISSION, 0).packageName == "com.lbe.security.miui"
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    @Suppress("DEPRECATION")
    private fun installedPackagesBefore33(packageManager: PackageManager) =
        // PackageInfoFlags is available from API 33; remove when minSdk reaches 33.
        packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)

    class AppListAccessException(val issue: AppListIssue) : Exception("Installed app list unavailable: $issue")

    fun getLastUpdateTime(context: Context): Long =
        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime

}
