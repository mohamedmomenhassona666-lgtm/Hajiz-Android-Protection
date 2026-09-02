package com.hajiz.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlin.math.min

class HajizVpnService : VpnService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val matcher = BlocklistMatcher()
    private var vpnInterface: ParcelFileDescriptor? = null
    private var packetJob: Job? = null
    private var domains: List<BlockedDomain> = emptyList()
    private var consecutiveResolveFailures = 0

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

   private suspend fun runDnsFilter() {
       var upstreamDnsServers = discoverUpstreamDnsServers()
        val builder = Builder()
            .setSession(getString(R.string.app_name))
            .setMtu(1500)
            .addAddress("10.8.0.2", 32)
            // DNS-only routing keeps ordinary app traffic on the system path while
            // giving Hajiz a local inspection point for the system DNS resolver.
            .addRoute("10.8.0.1", 32)
            .addDnsServer("10.8.0.1")
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
                   val response = resolveThroughUpstream(question.payload, upstreamDnsServers)
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
private fun discoverUpstreamDnsServers(): List<InetAddress> {
    val discovered = mutableListOf<InetAddress>()
    val connectivity = getSystemService(ConnectivityManager::class.java)

    connectivity.allNetworks.forEach { network ->
        val capabilities = connectivity.getNetworkCapabilities(network)

        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
            return@forEach
        }

        connectivity.getLinkProperties(network)?.dnsServers?.let(discovered::addAll)
    }

    val publicFallbacks = PUBLIC_DNS_SERVERS.mapNotNull { address ->
        runCatching { InetAddress.getByName(address) }.getOrNull()
    }

    return (discovered + publicFallbacks)
        .distinctBy { it.hostAddress }
        .take(MAX_UPSTREAM_DNS_SERVERS)
}
    private fun resolveThroughUpstream(
    query: ByteArray,
    upstreamDnsServers: List<InetAddress>,
): ByteArray? {
    return try {
        DatagramSocket().use { socket ->
            if (!protect(socket)) return@use null

            upstreamDnsServers.forEach { server ->
                socket.send(
                    DatagramPacket(
                        query,
                        query.size,
                        InetSocketAddress(server, 53),
                    ),
                )
            }

            val deadline = System.nanoTime() + DNS_RESPONSE_WINDOW_MS * 1_000_000L

            while (true) {
                val remainingMs =
                    (deadline - System.nanoTime()) / 1_000_000L

                if (remainingMs <= 0) return@use null

                socket.soTimeout =
                    min(remainingMs.toInt(), DNS_SOCKET_TIMEOUT_MS)

                val responseBytes = ByteArray(4096)
                val response =
                    DatagramPacket(responseBytes, responseBytes.size)

                try {
                    socket.receive(response)
                } catch (_: SocketTimeoutException) {
                    return@use null
                }

                if (
                    response.length >= 2 &&
                    responseBytes[0] == query[0] &&
                    responseBytes[1] == query[1]
                ) {
                    return@use responseBytes.copyOf(response.length)
                }
            }
        }
    } catch (_: Exception) {
        null
    }
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
        private const val DNS_SOCKET_TIMEOUT_MS = 500
        private const val DNS_RESPONSE_WINDOW_MS = 2_000L
        private const val MAX_UPSTREAM_DNS_SERVERS = 8

        private val PUBLIC_DNS_SERVERS = listOf(
            "1.1.1.1",
            "1.0.0.1",
            "8.8.8.8",
            "8.8.4.4",
            "9.9.9.9",
        )
    }
}
