package com.v2ray.ang.ui.server

internal fun isProfileDeletionBlocked(guid: String, selectedGuid: String?, isRunning: Boolean): Boolean =
    guid.isBlank() || isRunning && guid == selectedGuid
