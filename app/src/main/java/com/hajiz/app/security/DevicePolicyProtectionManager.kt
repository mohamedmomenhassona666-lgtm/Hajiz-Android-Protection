package com.hajiz.app.security

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

enum class DeviceManagementState { DEVICE_OWNER, PROFILE_OWNER, PERSONAL_DEVICE }

class DevicePolicyProtectionManager(context: Context) {
    private val manager = context.getSystemService(DevicePolicyManager::class.java)
    private val component = ComponentName(context, HajizDeviceAdminReceiver::class.java)

    fun state(): DeviceManagementState = when {
        manager.isDeviceOwnerApp(context.packageName) -> DeviceManagementState.DEVICE_OWNER
        manager.isProfileOwnerApp(context.packageName) -> DeviceManagementState.PROFILE_OWNER
        else -> DeviceManagementState.PERSONAL_DEVICE
    }

    fun explanation(): String = when (state()) {
        DeviceManagementState.DEVICE_OWNER -> "This device is managed by Hajiz as Device Owner."
        DeviceManagementState.PROFILE_OWNER -> "This work profile is managed by Hajiz as Profile Owner."
        DeviceManagementState.PERSONAL_DEVICE -> "This is a personal device. Android does not allow a normal app to guarantee uninstall prevention."
    }
}