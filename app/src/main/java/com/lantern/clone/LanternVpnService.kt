package com.lantern.clone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class LanternVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_CONNECT = "com.lantern.clone.CONNECT"
        const val ACTION_DISCONNECT = "com.lantern.clone.DISCONNECT"
        const val BROADCAST_VPN_STATE = "com.lantern.clone.VPN_STATE"
        const val EXTRA_STATE = "vpn_state"
        const val NOTIFICATION_CHANNEL_ID = "lantern_vpn_channel"
        const val NOTIFICATION_ID = 1001

        var isRunning = false
            private set
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> startVpn()
            ACTION_DISCONNECT -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) return

        createNotificationChannel()
        val notification = buildNotification("Lantern is connected", "Securing your connection...")
        startForeground(NOTIFICATION_ID, notification)

        try {
            val builder = Builder()
                .setSession("Lantern")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)

            vpnInterface = builder.establish()
            isRunning = true
            broadcastState(true)

            // Start background network loop simulation / packet forwarder
            serviceJob = scope.launch {
                val vpnInput = FileInputStream(vpnInterface?.fileDescriptor)
                val vpnOutput = FileOutputStream(vpnInterface?.fileDescriptor)
                val buffer = ByteArray(32767)

                try {
                    while (isActive && isRunning) {
                        val length = vpnInput.read(buffer)
                        if (length > 0) {
                            // Traffic loop & tunnel handler
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            stopVpn()
        }
    }

    private fun stopVpn() {
        isRunning = false
        broadcastState(false)
        serviceJob?.cancel()
        serviceJob = null

        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun broadcastState(connected: Boolean) {
        val intent = Intent(BROADCAST_VPN_STATE).apply {
            putExtra(EXTRA_STATE, connected)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Lantern VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows Lantern VPN active connection state"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        scope.cancel()
        super.onDestroy()
    }
}
