package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

val JarvisBackground = Color(0xFF050505)
val JarvisCyan = Color(0xFF00E5FF)
val JarvisTextMuted = Color(0xFF94A3B8)
val JarvisCardBg = Color(0x0CFFFFFF)
val JarvisCardBorder = Color(0x1AFFFFFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    JarvisDashboard(
                        modifier = Modifier.padding(innerPadding),
                        onOpenAccessibilitySettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        onOpenNotificationSettings = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                        onOpenOverlaySettings = { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) }
                    )
                }
            }
        }
    }
}

@Composable
fun JarvisDashboard(
    modifier: Modifier = Modifier,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
    ) {
        JarvisHeader()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            CoreLoadIndicator()
            SystemLogsCard()
            ActiveMandateCard()
            PermissionsSection(onOpenAccessibilitySettings, onOpenNotificationSettings, onOpenOverlaySettings)
            Spacer(modifier = Modifier.height(24.dp))
        }
        BottomNavBar()
    }
}

@Composable
fun JarvisHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("RUNTIME STATUS", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = JarvisCyan.copy(alpha = 0.6f), letterSpacing = 2.sp)
            Text("JARVIS_v1.0", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                label = "alpha"
            )
            Box(modifier = Modifier.size(8.dp).background(JarvisCyan.copy(alpha = pulseAlpha), CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text("OS_HOOKED", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = JarvisCyan)
        }
    }
}

@Composable
fun CoreLoadIndicator() {
    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(140.dp).background(Brush.radialGradient(listOf(JarvisCyan.copy(alpha = 0.2f), Color.Transparent)), CircleShape))
        Box(modifier = Modifier.size(110.dp).border(1.dp, JarvisCyan.copy(alpha = 0.3f), CircleShape).background(JarvisCyan.copy(alpha = 0.05f), CircleShape), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(86.dp).border(2.dp, JarvisCyan, CircleShape), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("88%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("CORE LOAD", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = JarvisCyan.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun SystemLogsCard() {
    Column(modifier = Modifier.fillMaxWidth().background(JarvisCardBg, RoundedCornerShape(16.dp)).border(1.dp, JarvisCardBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Build, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SYSTEM LOGS", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisTextMuted, letterSpacing = 2.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        val logLines = listOf("[14:22:04] BIND_ACCESSIBILITY: SUCCESS", "[14:22:08] MAPS_SCRAPE: 12 LEADS FOUND", "[14:23:15] BLOCK_UI: Instagram.exe TERMINATED")
        logLines.forEach { line ->
            val time = line.substringBefore("]") + "]"
            val message = line.substringAfter("]")
            Row {
                Text(time, color = Color(0xFF475569), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.width(4.dp))
                Text(message, color = JarvisCyan.copy(alpha = 0.8f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("> ", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            val infiniteTransition = rememberInfiniteTransition(label = "cursor")
            val pulseAlpha by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(500), repeatMode = RepeatMode.Reverse), label = "cursorAlpha")
            Box(modifier = Modifier.size(6.dp, 12.dp).background(JarvisCyan.copy(alpha = pulseAlpha)))
        }
    }
}

@Composable
fun ActiveMandateCard() {
    Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(JarvisCyan.copy(alpha = 0.2f), Color.Transparent)), RoundedCornerShape(16.dp)).border(1.dp, JarvisCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).padding(20.dp)) {
        Column {
            Text("ACTIVE MANDATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = JarvisCyan, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("\"Execution is the only metric. Sentiment is overhead.\"", fontSize = 18.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, color = Color.White, lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("- JARVIS CORE PROMPT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = JarvisTextMuted)
        }
    }
}

@Composable
fun PermissionsSection(onAuth: () -> Unit, onNotif: () -> Unit, onOverlay: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SYSTEM ACCESS", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisTextMuted, letterSpacing = 2.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PermissionBox(modifier = Modifier.weight(1f), title = "Accessibility", status = "Mandatory", indicatorType = 1, onClick = onAuth)
            PermissionBox(modifier = Modifier.weight(1f), title = "Notifications", status = "Required", indicatorType = 1, onClick = onNotif)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PermissionBox(modifier = Modifier.weight(1f), title = "Overlay View", status = "Sideloaded", indicatorType = 2, onClick = onOverlay)
            Box(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun PermissionBox(modifier: Modifier = Modifier, title: String, status: String, indicatorType: Int, onClick: () -> Unit) {
    Column(modifier = modifier.background(JarvisCardBg, RoundedCornerShape(12.dp)).border(1.dp, JarvisCardBorder, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(12.dp)) {
        Text(title.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = JarvisTextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(status, fontSize = 12.sp, color = Color.White)
            if (indicatorType == 1) {
                Box(modifier = Modifier.size(24.dp, 12.dp).background(JarvisCyan, CircleShape), contentAlignment = Alignment.CenterEnd) {
                    Box(modifier = Modifier.padding(2.dp).size(8.dp).background(Color.Black, CircleShape))
                }
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun BottomNavBar() {
    Row(modifier = Modifier.fillMaxWidth().height(80.dp).background(JarvisBackground).border(1.dp, Color(0x0AFFFFFF)).padding(16.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
        NavIcon(Icons.Default.Home, isActive = true)
        NavIcon(Icons.Default.Menu, isActive = false)
        NavIcon(Icons.Default.Lock, isActive = false)
        NavIcon(Icons.Default.Settings, isActive = false)
    }
}

@Composable
fun NavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean) {
    val color = if (isActive) JarvisCyan else Color.White
    val alpha = if (isActive) 1f else 0.4f
    val bgColor = if (isActive) JarvisCyan.copy(alpha = 0.1f) else Color.Transparent
    Box(modifier = Modifier.size(40.dp).background(bgColor, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = color.copy(alpha = alpha), modifier = Modifier.size(24.dp))
    }
}
