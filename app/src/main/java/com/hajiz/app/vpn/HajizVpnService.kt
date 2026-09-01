package com.hajiz.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.hajiz.app.HajizApplication
import com.hajiz.app.BlockedContentActivity
import com.hajiz.app.R
import com.hajiz.app.data.BlockedDomain
import com.hajiz.app.filtering.BlocklistMatcher
import com.hajiz.app.filtering.DnsPacket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class HajizVpnService : VpnService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val matcher = BlocklistMatcher()
    private var vpnInterface: ParcelFileDescriptor? = null
    private var packetJob: Job? = null
    private var domains: List<BlockedDomain> = emptyList()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val app = application as HajizApplication
        serviceScope.launch {
            app.blocklistProvider.observeBlockedDomains().collectLatest { domains = it }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopProtection()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        if (packetJob?.isActive != true) {
            packetJob = serviceScope.launch { runDnsFilter() }
        }
        return START_STICKY
    }

    private fun runDnsFilter() {
        val builder = Builder()
            .setSession(getString(R.string.app_name))
            .setMtu(1500)
            .addAddress("10.8.0.2", 32)
            // DNS-only routing keeps ordinary app traffic on the system path while
            // giving Hajiz a local inspection point for the system DNS resolver.
            .addRoute("1.1.1.1", 32)
            .addRoute("1.0.0.1", 32)
            .addDnsServer("1.1.1.1")
        vpnInterface = try {
            builder.establish()
        } catch (_: SecurityException) {
            sendState(false)
            return
        }
        val descriptor = vpnInterface ?: return
        sendState(true)
        val input = FileInputStream(descriptor.fileDescriptor)
        val output = FileOutputStream(descriptor.fileDescriptor)
        val buffer = ByteArray(32767)
        try {
            while (!Thread.currentThread().isInterrupted) {
                val length = input.read(buffer)
                if (length <= 0) break
                val question = DnsPacket.parseIpv4Udp(buffer.copyOf(length)) ?: continue
                val host = question.questionName ?: continue
                if (matcher.isBlocked(host, domains)) {
                    output.write(DnsPacket.nxdomain(question))
                    (application as HajizApplication).settingsRepository.recordBlockedAttempt()
                    notifyBlocked()
                } else {
                    val response = resolveThroughUpstream(question.payload)
                    if (response != null) {
                        output.write(
                            DnsPacket.wrapIpv4Udp(
                                payload = response,
                                sourceAddress = question.destinationAddress,
                                destinationAddress = question.sourceAddress,
                                sourcePort = 53,
                                destinationPort = question.sourcePort,
                            ),
                        )
                    }
                }
                output.flush()
            }
        } catch (_: CancellationException) {
            // Normal service shutdown.
        } catch (_: Exception) {
            // The notification and UI expose the service state; no crash loop.
        } finally {
            input.close()
            output.close()
        }
    }

    private fun resolveThroughUpstream(query: ByteArray): ByteArray? {
        return try {
            DatagramSocket().use { socket ->
                protect(socket)
                socket.soTimeout = 3000
                socket.connect(InetSocketAddress(UPSTREAM_DNS, 53))
                socket.send(DatagramPacket(query, query.size))
                val responseBytes = ByteArray(4096)
                val response = DatagramPacket(responseBytes, responseBytes.size)
                socket.receive(response)
                responseBytes.copyOf(response.length)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun stopProtection() {
        packetJob?.cancel()
        packetJob = null
        vpnInterface?.close()
        vpnInterface = null
        sendState(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopProtection()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun notifyBlocked() {
        val openBlockedPage = PendingIntent.getActivity(
            this,
            2000,
            Intent(this, BlockedContentActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        getSystemService(NotificationManager::class.java).notify(
            BLOCKED_NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.content_blocked_notification_title))
                .setContentText(getString(R.string.content_blocked_notification_text))
                .setAutoCancel(true)
                .setContentIntent(openBlockedPage)
                .build(),
        )
    }

    private fun sendState(active: Boolean) {
        sendBroadcast(
            Intent(ACTION_STATE_CHANGED)
                .setPackage(packageName)
                .putExtra(EXTRA_ACTIVE, active),
        )
    }

    companion object {
        const val ACTION_STOP = "com.hajiz.app.action.STOP_PROTECTION"
        const val ACTION_STATE_CHANGED = "com.hajiz.app.action.STATE_CHANGED"
        const val EXTRA_ACTIVE = "active"
        private const val CHANNEL_ID = "hajiz_protection"
        private const val NOTIFICATION_ID = 1001
        private const val BLOCKED_NOTIFICATION_ID = 2000
        private const val UPSTREAM_DNS = "1.1.1.1"
    }
}