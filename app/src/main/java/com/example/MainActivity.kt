package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
                    val navController = rememberNavController()
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(JarvisBackground)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = "dashboard",
                            modifier = Modifier.weight(1f)
                        ) {
                            composable("dashboard") {
                                JarvisDashboard(
                                    onOpenAccessibilitySettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                                    onOpenNotificationSettings = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                                    onOpenOverlaySettings = { 
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                                        startActivity(intent)
                                    }
                                )
                            }
                            composable("text_jarvis") {
                                TextJarvisScreen()
                            }
                            composable("settings") {
                                JarvisSettings()
                            }
                        }
                        BottomNavBar(navController = navController)
                    }
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
    val context = LocalContext.current
    var isJarvisActive by remember { mutableStateOf(com.example.util.JarvisPreferences.isJarvisActive(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle post permission logic if needed
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            val missing = permissionsToRequest.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) {
                permissionLauncher.launch(missing.toTypedArray())
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        JarvisHeader(isJarvisActive)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            CoreLoadIndicator(isJarvisActive)
            
            // Activation Button
            Button(
                onClick = { 
                    isJarvisActive = !isJarvisActive 
                    com.example.util.JarvisPreferences.setJarvisActive(context, isJarvisActive)
                    val serviceIntent = Intent(context, com.example.services.JarvisVoiceService::class.java)
                    if (isJarvisActive) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } else {
                        context.stopService(serviceIntent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, if (isJarvisActive) Color.Red else JarvisCyan, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isJarvisActive) Color(0x20FF0000) else JarvisCyan.copy(alpha = 0.1f),
                    contentColor = if (isJarvisActive) Color.Red else JarvisCyan
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isJarvisActive) "DEACTIVATE JARVIS" else "ACTIVATE JARVIS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            // Test Jarvis Button
            Button(
                onClick = { 
                    val intent = Intent(context, com.example.services.JarvisVoiceService::class.java)
                    intent.putExtra("COMMAND", "Jarvis Flash Light On")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, JarvisCardBorder, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisCardBg,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "TEST VOICE COMMAND",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 12.sp
                )
            }

            SystemLogsCard()
            ActiveMandateCard()
            PermissionsSection(onOpenAccessibilitySettings, onOpenNotificationSettings, onOpenOverlaySettings)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TextJarvisScreen() {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(listOf("Jarvis: I am online. How can I help you, Kashif Bhai?")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("TEXT TO TEXT JARVIS", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = JarvisCyan)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(JarvisCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, JarvisCardBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatHistory) { msg ->
                    Text(
                        text = msg,
                        color = if (msg.startsWith("You:")) Color.White else JarvisCyan,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Command...", color = JarvisTextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = JarvisCardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        chatHistory = chatHistory + "You: $inputText"
                        val lowerCmd = inputText.lowercase()
                        val response = when {
                            lowerCmd.contains("flash light on") || lowerCmd.contains("flashlight on") -> "Jarvis: Flashlight on kar di hai."
                            lowerCmd.contains("flash light off") || lowerCmd.contains("flashlight off") -> "Jarvis: Flashlight off kar di hai."
                            else -> "Jarvis: Command received. Processing..."
                        }
                        chatHistory = chatHistory + response
                        
                        // Pass command to service
                        val intent = Intent(context, com.example.services.JarvisVoiceService::class.java)
                        intent.putExtra("COMMAND", inputText)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(JarvisCyan, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
            }
        }
    }
}

@Composable
fun JarvisSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var startTime by remember { mutableStateOf(com.example.util.JarvisPreferences.getString(context, "start_time", "09:00")) }
    var endTime by remember { mutableStateOf(com.example.util.JarvisPreferences.getString(context, "end_time", "17:00")) }
    var apiKey by remember { mutableStateOf(com.example.util.JarvisPreferences.getString(context, "api_key", "sk-or-v1-...")) }
    var model by remember { mutableStateOf(com.example.util.JarvisPreferences.getString(context, "model", "google/gemini-2.0-flash-exp:free")) }
    var prompt by remember { mutableStateOf(com.example.util.JarvisPreferences.getString(context, "prompt", "ROLE: Autonomous Android Agent named 'Jarvis'.\nTONE: Ruthless, high-discipline...")) }
    var voiceWakeEnabled by remember { mutableStateOf(com.example.util.JarvisPreferences.getBoolean(context, "voice_wake_enabled", true)) }
    var notificationReadEnabled by remember { mutableStateOf(com.example.util.JarvisPreferences.getBoolean(context, "notification_read_enabled", true)) }
    var voiceToVoiceEnabled by remember { mutableStateOf(com.example.util.JarvisPreferences.getBoolean(context, "voice_to_voice_enabled", true)) }
    var overlayEnabled by remember { mutableStateOf(com.example.util.JarvisPreferences.getBoolean(context, "overlay_enabled", true)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("SYSTEM CONFIGURATION", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = JarvisCyan)
        
        Text("Configure AI parameters, communication, and work protocols.", color = JarvisTextMuted, fontSize = 14.sp)
        
        // AI Configuration
        Column(modifier = Modifier.fillMaxWidth().background(JarvisCardBg, RoundedCornerShape(16.dp)).border(1.dp, JarvisCardBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
            Text("OPENROUTER AI ENGINE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisTextMuted, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { 
                    apiKey = it 
                    com.example.util.JarvisPreferences.saveString(context, "api_key", it)
                },
                label = { Text("API Key", color = JarvisTextMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisCyan, unfocusedBorderColor = JarvisCardBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = model,
                onValueChange = { 
                    model = it 
                    com.example.util.JarvisPreferences.saveString(context, "model", it)
                },
                label = { Text("Model", color = JarvisTextMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisCyan, unfocusedBorderColor = JarvisCardBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                singleLine = true
            )
        }

        // System Prompt
        Column(modifier = Modifier.fillMaxWidth().background(JarvisCardBg, RoundedCornerShape(16.dp)).border(1.dp, JarvisCardBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
            Text("JARVIS CORE PROMPT", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisTextMuted, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = prompt,
                onValueChange = { 
                    prompt = it 
                    com.example.util.JarvisPreferences.saveString(context, "prompt", it)
                },
                modifier = Modifier.fillMaxWidth().height(140.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisCyan, unfocusedBorderColor = JarvisCardBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { /* Upgrade logic */ }, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black), shape = RoundedCornerShape(8.dp)) {
                    Text("UPGRADE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(onClick = { 
                    prompt = "ROLE: Autonomous Android Agent named 'Jarvis'.\nTONE: Ruthless, high-discipline..."
                    com.example.util.JarvisPreferences.saveString(context, "prompt", prompt)
                }, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red), shape = RoundedCornerShape(8.dp)) {
                    Text("RESET", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Voice & Automation
        Column(modifier = Modifier.fillMaxWidth().background(JarvisCardBg, RoundedCornerShape(16.dp)).border(1.dp, JarvisCardBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
            Text("AUTOMATION & VOICE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisTextMuted, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(16.dp))
            SwitchSettingItem("Display Over Other Apps", "Enforce lock screen & tasks globally", overlayEnabled) { 
                overlayEnabled = it 
                com.example.util.JarvisPreferences.saveBoolean(context, "overlay_enabled", it)
            }
            SwitchSettingItem("Voice Wake-Word (\"Jarvis\")", "Replies: 'Assalamualaikum Kashif Bhai...'", voiceWakeEnabled) { 
                voiceWakeEnabled = it 
                com.example.util.JarvisPreferences.saveBoolean(context, "voice_wake_enabled", it)
                if (it) {
                    context.startService(Intent(context, com.example.services.JarvisVoiceService::class.java))
                }
            }
            SwitchSettingItem("Voice-to-Voice Comms", "Continuous vocal interaction mode", voiceToVoiceEnabled) { 
                voiceToVoiceEnabled = it 
                com.example.util.JarvisPreferences.saveBoolean(context, "voice_to_voice_enabled", it)
            }
            SwitchSettingItem("Notification Announcer", "Reads incoming notifications aloud", notificationReadEnabled) { 
                notificationReadEnabled = it 
                com.example.util.JarvisPreferences.saveBoolean(context, "notification_read_enabled", it)
            }
        }

        // Time Configuration
        Column(modifier = Modifier.fillMaxWidth().background(JarvisCardBg, RoundedCornerShape(16.dp)).border(1.dp, JarvisCardBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
            Text("WORK PROTOCOL", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisTextMuted, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { 
                        startTime = it 
                        com.example.util.JarvisPreferences.saveString(context, "start_time", it)
                    },
                    label = { Text("Start Time") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisCyan, unfocusedBorderColor = JarvisCardBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    singleLine = true
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { 
                        endTime = it 
                        com.example.util.JarvisPreferences.saveString(context, "end_time", it)
                    },
                    label = { Text("End Time") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisCyan, unfocusedBorderColor = JarvisCardBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    singleLine = true
                )
            }
        }
        
        // Allowed Apps
        Column(modifier = Modifier.fillMaxWidth().background(JarvisCardBg, RoundedCornerShape(16.dp)).border(1.dp, JarvisCardBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
            Text("ALLOWED APPLICATIONS", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisTextMuted, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            var appsList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
            
            LaunchedEffect(Unit) {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                val resolvedInfos = pm.queryIntentActivities(intent, 0)
                appsList = resolvedInfos.map {
                    val appName = it.loadLabel(pm).toString()
                    val packageName = it.activityInfo.packageName
                    appName to packageName
                }.distinctBy { it.second }.sortedBy { it.first }
            }
            
            Column(modifier = Modifier.height(300.dp).verticalScroll(rememberScrollState())) {
                if (appsList.isEmpty()) {
                    Text("Loading Apps...", color = JarvisTextMuted)
                } else {
                    appsList.forEach { (appName, pkgName) ->
                        AllowedAppItem(appName, Icons.Default.Info, pkgName)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AllowedAppItem(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, packageName: String) {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(com.example.util.JarvisPreferences.getAllowedApps(context).contains(packageName)) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                checked = !checked 
                com.example.util.JarvisPreferences.setAppAllowed(context, packageName, checked)
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (checked) JarvisCyan else JarvisTextMuted, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(name, color = if (checked) Color.White else JarvisTextMuted, fontSize = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = { 
                checked = it 
                com.example.util.JarvisPreferences.setAppAllowed(context, packageName, it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = JarvisCyan,
                uncheckedThumbColor = JarvisTextMuted,
                uncheckedTrackColor = JarvisCardBg
            )
        )
    }
}

@Composable
fun SwitchSettingItem(title: String, subTitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp)
            Text(subTitle, color = JarvisTextMuted, fontSize = 10.sp, lineHeight = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = JarvisCyan,
                uncheckedThumbColor = JarvisTextMuted,
                uncheckedTrackColor = JarvisCardBg
            )
        )
    }
}

@Composable
fun JarvisHeader(isJarvisActive: Boolean) {
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
            Box(modifier = Modifier.size(8.dp).background(if (isJarvisActive) Color.Red else JarvisCyan.copy(alpha = pulseAlpha), CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isJarvisActive) "ENFORCING" else "STANDBY", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = if (isJarvisActive) Color.Red else JarvisCyan)
        }
    }
}

@Composable
fun CoreLoadIndicator(isJarvisActive: Boolean) {
    val activeColor = if (isJarvisActive) Color.Red else JarvisCyan
    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(140.dp).background(Brush.radialGradient(listOf(activeColor.copy(alpha = 0.2f), Color.Transparent)), CircleShape))
        Box(modifier = Modifier.size(110.dp).border(1.dp, activeColor.copy(alpha = 0.3f), CircleShape).background(activeColor.copy(alpha = 0.05f), CircleShape), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(86.dp).border(2.dp, activeColor, CircleShape), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isJarvisActive) "100%" else "88%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("CORE LOAD", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = activeColor.copy(alpha = 0.8f))
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
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Row(modifier = Modifier.fillMaxWidth().height(80.dp).background(JarvisBackground).border(1.dp, Color(0x0AFFFFFF)).padding(16.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
        NavIcon(Icons.Default.Home, isActive = currentRoute == "dashboard", onClick = {
            navController.navigate("dashboard") {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        })
        NavIcon(Icons.Default.MailOutline, isActive = currentRoute == "text_jarvis", onClick = {
            navController.navigate("text_jarvis") {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        })
        NavIcon(Icons.Default.Settings, isActive = currentRoute == "settings", onClick = {
            navController.navigate("settings") {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        })
    }
}

@Composable
fun NavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean, onClick: () -> Unit = {}) {
    val color = if (isActive) JarvisCyan else Color.White
    val alpha = if (isActive) 1f else 0.4f
    val bgColor = if (isActive) JarvisCyan.copy(alpha = 0.1f) else Color.Transparent
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(bgColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = color.copy(alpha = alpha), modifier = Modifier.size(24.dp))
    }
}

