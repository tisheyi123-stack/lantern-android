package com.lantern.clone

import android.app.Activity
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private var isConnectedState = mutableStateOf(LanternVpnService.isRunning)

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            triggerVpnService(true)
        }
    }

    private val vpnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LanternVpnService.BROADCAST_VPN_STATE) {
                val state = intent.getBooleanExtra(LanternVpnService.EXTRA_STATE, false)
                isConnectedState.value = state
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter(LanternVpnService.BROADCAST_VPN_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(vpnReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(vpnReceiver, filter)
        }

        setContent {
            LanternTheme {
                LanternMainScreen(
                    isConnected = isConnectedState.value,
                    onToggleConnect = { shouldConnect ->
                        if (shouldConnect) {
                            val vpnIntent = VpnService.prepare(this)
                            if (vpnIntent != null) {
                                vpnPermissionLauncher.launch(vpnIntent)
                            } else {
                                triggerVpnService(true)
                            }
                        } else {
                            triggerVpnService(false)
                        }
                    }
                )
            }
        }
    }

    private fun triggerVpnService(connect: Boolean) {
        val intent = Intent(this, LanternVpnService::class.java).apply {
            action = if (connect) LanternVpnService.ACTION_CONNECT else LanternVpnService.ACTION_DISCONNECT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(vpnReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}

@Composable
fun LanternTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = LanternBackground,
            surface = LanternCardBg,
            primary = LanternBlue
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanternMainScreen(
    isConnected: Boolean,
    onToggleConnect: (Boolean) -> Unit
) {
    var selectedServer by remember { mutableStateOf("Fastest Location (Auto)") }
    var uploadSpeed by remember { mutableStateOf("0 KB/s") }
    var downloadSpeed by remember { mutableStateOf("0 KB/s") }
    var dataUsed by remember { mutableStateOf("48.5 MB / 500 MB") }

    // Simulated traffic counter when connected
    LaunchedEffect(isConnected) {
        if (isConnected) {
            while (true) {
                delay(1000)
                downloadSpeed = "${Random.nextInt(120, 850)} KB/s"
                uploadSpeed = "${Random.nextInt(30, 240)} KB/s"
            }
        } else {
            downloadSpeed = "0 KB/s"
            uploadSpeed = "0 KB/s"
        }
    }

    Scaffold(
        containerColor = LanternBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Lantern Logo",
                            tint = LanternGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lantern",
                            color = LanternTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Pro Version",
                            tint = LanternGold
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = LanternTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LanternBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Top Status Badge
            Surface(
                color = if (isConnected) LanternConnectedGreen.copy(alpha = 0.15f) else LanternCardBg,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.dp,
                    if (isConnected) LanternConnectedGreen else LanternTextSecondary.copy(alpha = 0.3f)
                ),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) LanternConnectedGreen else LanternDisconnectedRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) "Lantern is ON" else "Lantern is OFF",
                        color = LanternTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Big Iconic Lantern Glowing Power Button
            LanternPowerButton(
                isConnected = isConnected,
                onClick = { onToggleConnect(!isConnected) }
            )

            // Server Selector Card
            Surface(
                color = LanternCardBg,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* Select Server Dialog */ }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = LanternBlue.copy(alpha = 0.2f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "Location",
                                    tint = LanternCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Server Location",
                                fontSize = 12.sp,
                                color = LanternTextSecondary
                            )
                            Text(
                                text = selectedServer,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LanternTextPrimary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Select",
                        tint = LanternTextSecondary
                    )
                }
            }

            // Stats Card (Speed & Free Data Quota)
            Card(
                colors = CardDefaults.cardColors(containerColor = LanternCardBg),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(
                            icon = Icons.Default.ArrowDownward,
                            label = "Download",
                            value = downloadSpeed,
                            iconColor = LanternCyan
                        )
                        Divider(
                            modifier = Modifier
                                .height(40.dp)
                                .width(1.dp),
                            color = LanternTextSecondary.copy(alpha = 0.2f)
                        )
                        StatItem(
                            icon = Icons.Default.ArrowUpward,
                            label = "Upload",
                            value = uploadSpeed,
                            iconColor = LanternGold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Lantern Data Quota Indicator
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Free High-Speed Data",
                                fontSize = 12.sp,
                                color = LanternTextSecondary
                            )
                            Text(
                                text = dataUsed,
                                fontSize = 12.sp,
                                color = LanternTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { 0.1f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = LanternBlue,
                            trackColor = LanternBackground
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LanternPowerButton(
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnected) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isConnected) LanternConnectedGreen else LanternBlue,
        animationSpec = tween(500)
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        // Outer Glow Rings
        if (isConnected) {
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(LanternConnectedGreen.copy(alpha = 0.15f))
            )
        }

        Box(
            modifier = Modifier
                .size(170.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            buttonColor,
                            LanternDarkBlue
                        )
                    )
                )
                .border(
                    width = 4.dp,
                    color = if (isConnected) LanternConnectedGreen.copy(alpha = 0.8f) else LanternCyan.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Connect",
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isConnected) "CONNECTED" else "CONNECT",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = LanternTextSecondary)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LanternTextPrimary)
        }
    }
}
