package com.insys.player.exoplayer.drm.kotlin.drmplaybackkotlin.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: ComponentActivity) {
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var onPermissionGranted: (() -> Unit)? = null
    private var onPermissionDenied: (() -> Unit)? = null

    init {
        setupLauncher()
    }

    private fun setupLauncher() {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                onPermissionGranted?.invoke()
            } else {
                onPermissionDenied?.invoke()
            }
        }
    }

    /**
     * Checks and asks for notification permission (Android 13+).
     */
    fun checkNotificationPermission(
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        this.onPermissionGranted = onGranted
        this.onPermissionDenied = onDenied

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (status == PackageManager.PERMISSION_GRANTED) {
                onGranted()
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            onGranted()
        }
    }
}