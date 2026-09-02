package com.hajiz.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.hajiz.app.ui.HajizApp
import com.hajiz.app.ui.HajizViewModel
import com.hajiz.app.ui.theme.HajizTheme
import com.hajiz.app.vpn.HajizVpnService

class MainActivity : ComponentActivity() {

    private val viewModel: HajizViewModel by viewModels {
        HajizViewModel.Factory(
            (application as HajizApplication).settingsRepository
        )
    }

    private val vpnPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                startVpn()
            } else {
                viewModel.reportVpnPermissionDenied()
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    private val vpnStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                viewModel.setVpnActive(
                    intent.getBooleanExtra(
                        HajizVpnService.EXTRA_ACTIVE,
                        false,
                    )
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        setContent {
            HajizTheme {
                HajizApp(
                    viewModel = viewModel,

                    onRequestVpnPermission = {
                        val permissionIntent =
                            VpnService.prepare(this)

                        if (permissionIntent == null) {
                            startVpn()
                        } else {
                            vpnPermissionLauncher.launch(
                                permissionIntent
                            )
                        }
                    },

                    onStopProtection = {
                        stopVpn()
                    },

                    onOpenVpnSettings = {
                        startActivity(
                            Intent(
                                "android.settings.VPN_SETTINGS"
                            )
                        )
                    },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val filter =
            IntentFilter(
                HajizVpnService.ACTION_STATE_CHANGED
            )

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                vpnStateReceiver,
                filter,
                RECEIVER_NOT_EXPORTED,
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(
                vpnStateReceiver,
                filter,
            )
        }
    }

    override fun onStop() {
        runCatching {
            unregisterReceiver(vpnStateReceiver)
        }

        super.onStop()
    }

    private fun startVpn() {
        ContextCompat.startForegroundService(
            this,
            Intent(
                this,
                HajizVpnService::class.java,
            ),
        )

        viewModel.startProtection()
    }

    private fun stopVpn() {
        val intent =
            Intent(
                this,
                HajizVpnService::class.java,
            ).apply {
                action = HajizVpnService.ACTION_STOP
            }

        startService(intent)

        viewModel.stopProtection()
    }
}
