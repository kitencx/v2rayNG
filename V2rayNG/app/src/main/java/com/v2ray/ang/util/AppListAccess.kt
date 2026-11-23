package com.v2ray.ang.util

enum class AppListIssue { PermissionRequired, Restricted, Failed }

internal fun hasRestrictedAppVisibility(packageNames: Collection<String>, ownPackage: String): Boolean =
    packageNames.isEmpty() || packageNames.all { it == ownPackage }
