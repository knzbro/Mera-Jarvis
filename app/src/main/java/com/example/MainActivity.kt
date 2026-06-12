package com.example

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
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
                    var apiErrorDetails by remember { mutableStateOf<Pair<String, String>?>(null) }
                    val context = LocalContext.current

                    DisposableEffect(context) {
                        val receiver = object : android.content.BroadcastReceiver() {
                            override fun onReceive(ctx: android.content.Context?, intent: Intent?) {
                                val errorType = intent?.getStringExtra("error_type") ?: "UNKNOWN_ERROR"
                                val errorMessage = intent?.getStringExtra("error_message") ?: "An unexpected error occurred."
                                apiErrorDetails = Pair(errorType, errorMessage)
                            }
                        }
                        val filter = android.content.IntentFilter("com.example.JARVIS_API_ERROR")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
                        } else {
                            context.registerReceiver(receiver, filter)
                        }
                        onDispose {
                            context.unregisterReceiver(receiver)
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(JarvisBackground)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = "dashboard",
                                modifier = Modifier.weight(1f),
                                enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = androidx.compose.animation.core.tween(300)) },
                                exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = androidx.compose.animation.core.tween(300)) },
                                popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = androidx.compose.animation.core.tween(300)) },
                                popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = androidx.compose.animation.core.tween(300)) }
                            ) {
                                composable("dashboard") {
                                    JarvisDashboard(
                                        onOpenAccessibilitySettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                                        onOpenNotificationSettings = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                                    )
                                }
                                composable("gestures") {
                                    GestureJarvisScreen()
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

                        // Neon Cyber Warning Dialog Popup
                        apiErrorDetails?.let { (type, msg) ->
                            androidx.compose.ui.window.Dialog(onDismissRequest = { apiErrorDetails = null }) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .border(2.dp, Color.Red, RoundedCornerShape(24.dp)),
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color(0xFF140505)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Warning",
                                            tint = Color.Red,
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "Kashif Bhai, Error Detected",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Red,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "JARVIS SYSTEMS BLOCKED",
                                            fontSize = 11.sp,
                                            color = JarvisTextMuted,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "CODE: $type\n\n$msg",
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            onClick = { 
                                                apiErrorDetails = null
                                                navController.navigate("settings") {
                                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            Text("FIX IT", fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
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
    onOpenNotificationSettings: () -> Unit
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
                    if (!isJarvisActive) {
                        val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (!hasMicPermission) {
                            android.widget.Toast.makeText(
                                context,
                                "Kashif Bhai, pehle Microphone permission allow kijiye!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                            return@Button
                        }
                    }

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
            DownloadJarvisApkCard()
            PermissionsSection(onOpenAccessibilitySettings, onOpenNotificationSettings)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TextJarvisScreen() {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(emptyList<String>()) }
    val listState = rememberLazyListState()

    // Load initial chat history from persistent storage
    LaunchedEffect(Unit) {
        val history = com.example.util.JarvisPreferences.getChatHistory(context)
        if (history.isEmpty()) {
            val defaultMsg = "Jarvis: Main hazir hoon, Kashif Bhai. Ask me anything!"
            com.example.util.JarvisPreferences.saveChatHistory(context, listOf(defaultMsg))
            chatHistory = listOf(defaultMsg)
        } else {
            chatHistory = history
        }
    }

    // Auto-scroll when size changes
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    // Live update broadcast receiver to sync other modules / spoken returns
    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == "com.example.JARVIS_CHAT_UPDATED") {
                    chatHistory = com.example.util.JarvisPreferences.getChatHistory(context)
                }
            }
        }
        val filter = android.content.IntentFilter("com.example.JARVIS_CHAT_UPDATED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TEXT TO TEXT JARVIS", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = JarvisCyan)
                IconButton(
                    onClick = {
                        com.example.util.JarvisPreferences.clearChatHistory(context)
                        val resetMsg = "Jarvis: Main hazir hoon, Kashif Bhai. Chat logs empty."
                        chatHistory = listOf(resetMsg)
                        com.example.util.JarvisPreferences.saveChatHistory(context, chatHistory)
                    }
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Chat Log", tint = Color.Red.copy(alpha = 0.8f))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                state = listState,
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
                    val commandText = inputText.trim()
                    if (commandText.isNotBlank()) {
                        // Persist user prompt
                        com.example.util.JarvisPreferences.addChatMessage(context, "You: $commandText")
                        
                        // Pass command to service as text intent
                        val intent = Intent(context, com.example.services.JarvisVoiceService::class.java)
                        intent.putExtra("COMMAND", commandText)
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
    var wakeWordSensitivity by remember { mutableStateOf(com.example.util.JarvisPreferences.getString(context, "wake_word_sensitivity", "MEDIUM")) }

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
            SwitchSettingItem("Voice Wake-Word (\"Jarvis\")", "Replies: 'Assalamualaikum Kashif Bhai...'", voiceWakeEnabled) { 
                voiceWakeEnabled = it 
                com.example.util.JarvisPreferences.saveBoolean(context, "voice_wake_enabled", it)
                if (it) {
                    context.startService(Intent(context, com.example.services.JarvisVoiceService::class.java))
                }
            }
            if (voiceWakeEnabled) {
                SensitiveSelector(
                    selected = wakeWordSensitivity,
                    onSelectedChange = {
                        wakeWordSensitivity = it
                        com.example.util.JarvisPreferences.saveString(context, "wake_word_sensitivity", it)
                        // Trigger speech recognizer refresh in service if running
                        if (com.example.util.JarvisPreferences.isJarvisActive(context)) {
                            context.startService(Intent(context, com.example.services.JarvisVoiceService::class.java))
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
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

        // --- CUSTOM OFFLINE VOICE COMMANDS TRAINER ---
        Column(modifier = Modifier.fillMaxWidth().background(JarvisCardBg, RoundedCornerShape(16.dp)).border(1.dp, JarvisCardBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
            Text("CUSTOM OFFLINE VOICE TRAINING", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisTextMuted, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Route raw spoken inputs directly to customized actions and voice answers offline.", color = JarvisTextMuted, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(16.dp))

            var customCommands by remember { mutableStateOf(com.example.util.JarvisPreferences.getTrainedCommands(context)) }
            var newTrigger by remember { mutableStateOf("") }
            var newResponse by remember { mutableStateOf("") }
            var selectedAction by remember { mutableStateOf("voice_reply") }
            var showActionDropdown by remember { mutableStateOf(false) }

            if (customCommands.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp)).padding(14.dp), contentAlignment = Alignment.Center) {
                    Text("No custom trained commands yet. Program one below!", fontSize = 12.sp, color = JarvisTextMuted, fontStyle = FontStyle.Italic)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    customCommands.forEach { cmd ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("\"${cmd.trigger}\"", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = JarvisCyan)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Action: ${cmd.action}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisTextMuted)
                                if (cmd.response.isNotBlank()) {
                                    Text("TTS Reply: ${cmd.response}", fontSize = 12.sp, color = Color.White)
                                }
                            }
                            IconButton(
                                onClick = {
                                    com.example.util.JarvisPreferences.removeTrainedCommand(context, cmd.trigger)
                                    customCommands = com.example.util.JarvisPreferences.getTrainedCommands(context)
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete training", tint = Color.Red.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = JarvisCardBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Text("TRAIN NEW COMMAND", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = JarvisCyan, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = newTrigger,
                onValueChange = { newTrigger = it },
                label = { Text("When I Say (Voice Trigger)", color = JarvisTextMuted) },
                placeholder = { Text("e.g. hello jarvis", color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisCyan, unfocusedBorderColor = JarvisCardBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = newResponse,
                onValueChange = { newResponse = it },
                label = { Text("Jarvis Speaks (Voice Response)", color = JarvisTextMuted) },
                placeholder = { Text("e.g. Aslam o Alaikum Kashif Bhai!", color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisCyan, unfocusedBorderColor = JarvisCardBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Selection dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedAction,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Mechanical Macro Action", color = JarvisTextMuted) },
                    modifier = Modifier.fillMaxWidth().clickable { showActionDropdown = true },
                    trailingIcon = {
                        IconButton(onClick = { showActionDropdown = true }) {
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Select Action", tint = JarvisCyan)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisCyan, unfocusedBorderColor = JarvisCardBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                DropdownMenu(
                    expanded = showActionDropdown,
                    onDismissRequest = { showActionDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.8f).background(Color(0xFF0F0F0F)).border(1.dp, JarvisCardBorder)
                ) {
                    val actionsList = listOf(
                        "Voice Reply Only (No Action)" to "voice_reply",
                        "Turn Flashlight On" to "flashlight_on",
                        "Turn Flashlight Off" to "flashlight_off",
                        "Turn Wi-Fi On" to "wifi_on",
                        "Turn Wi-Fi Off" to "wifi_off",
                        "Turn Bluetooth On" to "bluetooth_on",
                        "Turn Bluetooth Off" to "bluetooth_off",
                        "Navigate to Home Screen" to "go_home",
                        "Show Recent Apps" to "show_recents",
                        "Play Song in Pro Folder" to "play_song",
                        "Activate Airplane Settings" to "toggle_airplane_mode",
                        "Power Controls Context Menu" to "power_dialog",
                        "Lock Screen Device Secure" to "lock_screen",
                        "Open Quick Settings Bar" to "open_quick_settings",
                        "Open Notification Settings" to "open_notifications",
                        "Check Device Battery Power" to "battery_status",
                        "Raise Stream Music Volume" to "volume_up",
                        "Low Stream Music Volume" to "volume_down",
                        "Mute Dynamic Sound Streams" to "mute",
                        "Reset Sound Stream Levels" to "unmute",
                        "Open Primary Android Web" to "open_browser",
                        "Search Engine Query Core" to "search_google",
                        "Get Local Time Clock" to "get_time",
                        "Check Calendar Dates" to "get_date",
                        "Set Device Timers Sync" to "set_timer",
                        "Create Plain Note Record" to "create_note",
                        "Open Files Pro Level Folder" to "open_custom_folder"
                    )

                    actionsList.forEach { (label, actValue) ->
                        DropdownMenuItem(
                            text = { Text(label, color = Color.White, fontSize = 13.sp) },
                            onClick = {
                                selectedAction = actValue
                                showActionDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (newTrigger.isNotBlank()) {
                        val tc = com.example.util.TrainedCommand(
                            trigger = newTrigger.trim(),
                            action = selectedAction,
                            response = newResponse.trim()
                        )
                        com.example.util.JarvisPreferences.addTrainedCommand(context, tc)
                        customCommands = com.example.util.JarvisPreferences.getTrainedCommands(context)
                        // Reset input fields
                        newTrigger = ""
                        newResponse = ""
                        selectedAction = "voice_reply"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SAVE VOCAL PROTOCOL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        
        DownloadJarvisApkCard()
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
fun SensitiveSelector(
    selected: String,
    onSelectedChange: (String) -> Unit
) {
    val options = listOf("LOW", "MEDIUM", "HIGH")
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Wake-Word Sensitivity", color = Color.White, fontSize = 14.sp)
                val description = when (selected.uppercase()) {
                    "LOW" -> "Strict: Triggers only on \"Jarvis\" exact words. Excludes \"bhai\"/\"suno\" to end accidental triggers."
                    "MEDIUM" -> "Balanced: Triggers on \"Jarvis\", or \"bhai\" only if said with a command."
                    else -> "Responsive: Triggers on \"Jarvis\", \"bhai\", or \"suno\" instantly."
                }
                Text(description, color = JarvisTextMuted, fontSize = 10.sp, lineHeight = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(JarvisCardBg, RoundedCornerShape(8.dp))
                .border(1.dp, JarvisCardBorder, RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.forEach { option ->
                val isSelected = option.equals(selected, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) JarvisCyan.copy(alpha = 0.15f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectedChange(option) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) JarvisCyan else JarvisTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
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
            Text("JARVIS_v3.2", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
    val liveLogs by com.example.util.JarvisLogger.logs.collectAsState()
    val consoleScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF070B18), RoundedCornerShape(16.dp))
            .border(1.dp, JarvisCardBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        // Console Titlebar with terminal indicator buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // macOS style terminal windows lights
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFEF4444), CircleShape))
                Spacer(modifier = Modifier.width(5.dp))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFF59E0B), CircleShape))
                Spacer(modifier = Modifier.width(5.dp))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "CON_CORE_LOGSTREAM // ROOT",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = JarvisTextMuted,
                    letterSpacing = 1.sp
                )
            }
            
            // Console Action Control buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CLEAR",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444),
                    modifier = Modifier
                        .background(Color(0xFFEF4444).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .clickable { com.example.util.JarvisLogger.clear() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
                Text(
                    text = "+ TEST SIM",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = JarvisCyan,
                    modifier = Modifier
                        .background(JarvisCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .clickable {
                            val sims = listOf(
                                Triple("SPEECH_REC", "Kashif Bhai, continuous voice recognition standing by.", com.example.util.JarvisLogger.LogLevel.SUCCESS),
                                Triple("WAKE_DET", "Wake-word triggered with high sensitivity.", com.example.util.JarvisLogger.LogLevel.SUCCESS),
                                Triple("API_CALL", "Direct response from Google Gemini compiled.", com.example.util.JarvisLogger.LogLevel.INFO),
                                Triple("SYS_OVERLAY", "Bottom cyber animation overlay loaded.", com.example.util.JarvisLogger.LogLevel.SUCCESS),
                                Triple("CORE_ACC", "Accessibility scraped layout bounds.", com.example.util.JarvisLogger.LogLevel.WARN),
                                Triple("APK_EXP", "Jarvis.apk compiled and initialized for sharing.", com.example.util.JarvisLogger.LogLevel.INFO)
                            )
                            val randomSim = sims.random()
                            com.example.util.JarvisLogger.log(randomSim.first, randomSim.second, randomSim.third)
                        }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Log Entries Display Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .background(Color(0xFF030712), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(consoleScrollState)
            ) {
                if (liveLogs.isEmpty()) {
                    Text(
                        text = "[SYSTEM STALL]: Terminal idle. No active events.",
                        color = Color(0xFF475569),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    liveLogs.forEach { log ->
                        val timeStr = "[${log.timestamp}]"
                        val tagStr = "${log.tag}:"
                        val lvlColor = when (log.level) {
                            com.example.util.JarvisLogger.LogLevel.SUCCESS -> Color(0xFF10B981)
                            com.example.util.JarvisLogger.LogLevel.WARN -> Color(0xFFF59E0B)
                            com.example.util.JarvisLogger.LogLevel.ERROR -> Color(0xFFEF4444)
                            com.example.util.JarvisLogger.LogLevel.INFO -> JarvisCyan
                        }

                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            // Timestamp
                            Text(
                                text = "$timeStr ",
                                color = Color(0xFF475569),
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            // Tag label
                            Text(
                                text = "$tagStr ",
                                color = lvlColor.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            // Message content
                            Text(
                                text = log.message,
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }

                // Breathing interactive prompt line
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("jarvis@core_dashboard:~$ ", color = JarvisCyan.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.width(2.dp))
                    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(500), repeatMode = RepeatMode.Reverse),
                        label = "cursorAlpha"
                    )
                    Box(modifier = Modifier.size(5.dp, 10.dp).background(JarvisCyan.copy(alpha = pulseAlpha)))
                }
            }
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
fun PermissionsSection(
    onOpenAccessibilitySettings: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var isAccessibilityGranted by remember { mutableStateOf(false) }
    var isNotificationGranted by remember { mutableStateOf(false) }
    var isMicrophoneGranted by remember { mutableStateOf(false) }
    var isOverlayGranted by remember { mutableStateOf(false) }
    var isStorageGranted by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isMicrophoneGranted = granted
    }

    LaunchedEffect(Unit) {
        while (true) {
            // Check Accessibility Service status
            val service = context.packageName + java.io.File.separator + "com.example.services.JarvisAccessibilityService"
            val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            isAccessibilityGranted = enabledServices.contains(service)

            // Check Notification Listener status
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
            isNotificationGranted = flat.contains(context.packageName)

            // Check Microphone status
            isMicrophoneGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

            // Check Draw Over Other Apps overlay status
            isOverlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }

            // Check Media/External Storage status
            isStorageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }

            kotlinx.coroutines.delay(1000)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SYSTEM ACCESS DIAGNOSTIC", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisTextMuted, letterSpacing = 2.sp)
        
        // Dynamic Grid rows
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionBox(
                    modifier = Modifier.weight(1f),
                    title = "Accessibility",
                    status = if (isAccessibilityGranted) "ACTIVE" else "TAP TO GRANT",
                    isGranted = isAccessibilityGranted,
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
                PermissionBox(
                    modifier = Modifier.weight(1f),
                    title = "Notifications",
                    status = if (isNotificationGranted) "ACTIVE" else "TAP TO GRANT",
                    isGranted = isNotificationGranted,
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionBox(
                    modifier = Modifier.weight(1f),
                    title = "Microphone",
                    status = if (isMicrophoneGranted) "ACTIVE" else "TAP TO GRANT",
                    isGranted = isMicrophoneGranted,
                    onClick = {
                        if (!isMicrophoneGranted) {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
                PermissionBox(
                    modifier = Modifier.weight(1f),
                    title = "System Overlay",
                    status = if (isOverlayGranted) "ACTIVE" else "TAP TO GRANT",
                    isGranted = isOverlayGranted,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }

            PermissionBox(
                modifier = Modifier.fillMaxWidth(),
                title = "Pro Level Files & Storage",
                status = if (isStorageGranted) "ACTIVE / GRANTED" else "TAP TO GRANT COMPREHENSIVE STORAGE ACCESS",
                isGranted = isStorageGranted,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + context.packageName)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    } else {
                        micLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
            )
        }
    }
}

@Composable
fun PermissionBox(
    modifier: Modifier = Modifier,
    title: String,
    status: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    val activeBorderColor = if (isGranted) JarvisCyan.copy(alpha = 0.4f) else Color.Red.copy(alpha = 0.4f)
    val statusColor = if (isGranted) JarvisCyan else Color.Red

    Column(
        modifier = modifier
            .background(JarvisCardBg, RoundedCornerShape(12.dp))
            .border(1.dp, activeBorderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(title.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = JarvisTextMuted, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(status, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            if (isGranted) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
            } else {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isJarvisActive = com.example.util.JarvisPreferences.isJarvisActive(context)

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isJarvisActive) {
            // Continuous flowing gradient cyan-purple lighting indicator
            val infiniteTransition = rememberInfiniteTransition(label = "bottom_laser")
            val animTranslate by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "translate"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                JarvisCyan,
                                Color(0xFFD500F9), // Neon Magenta
                                JarvisCyan,
                                Color.Transparent
                            ),
                            startX = -200f + (animTranslate * 1500f),
                            endX = 400f + (animTranslate * 1500f)
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(JarvisBackground)
                .border(1.dp, Color(0x0AFFFFFF))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavIcon(Icons.Default.Home, isActive = currentRoute == "dashboard", onClick = {
                navController.navigate("dashboard") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            })
            NavIcon(Icons.Default.Create, isActive = currentRoute == "gestures", onClick = {
                navController.navigate("gestures") {
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

@Composable
fun GestureJarvisScreen() {
    val context = LocalContext.current
    var paths by remember { mutableStateOf(listOf<androidx.compose.ui.graphics.Path>()) }
    var currentPath by remember { mutableStateOf(androidx.compose.ui.graphics.Path()) }
    var actionText by remember { mutableStateOf("Draw shape for workflows...") }
    var showOnboarding by remember { mutableStateOf(false) }

    if (showOnboarding) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showOnboarding = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp).border(2.dp, JarvisCyan, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = JarvisCardBg
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("KASHIF BHAI, GESTURE DIRECTORY", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = JarvisCyan)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Guide Items
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Text("O", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Circle", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Toggle Flashlight / Lights", fontSize = 12.sp, color = JarvisTextMuted)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Text("V", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("V-Shape", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Start Voice Command", fontSize = 12.sp, color = JarvisTextMuted)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Text("Z", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Z-Shape", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Emergency / Full Override", fontSize = 12.sp, color = JarvisTextMuted)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Text("^", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Triangle / Up", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Launch YouTube", fontSize = 12.sp, color = JarvisTextMuted)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showOnboarding = false },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ACKNOWLEDGE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("GESTURE CORE", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = JarvisCyan)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { showOnboarding = true }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Info, contentDescription = "Gesture Guide", tint = JarvisCyan)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(actionText, color = JarvisTextMuted, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(JarvisCardBg, RoundedCornerShape(16.dp))
                .border(2.dp, JarvisCardBorder, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val newPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(offset.x, offset.y)
                            }
                            currentPath = newPath
                        },
                        onDragEnd = {
                            paths = paths + currentPath
                            currentPath = androidx.compose.ui.graphics.Path()
                            actionText = "Gesture processing..."
                            
                            val intent = Intent(context, com.example.services.JarvisVoiceService::class.java)
                            intent.putExtra("COMMAND", "Gesture recognized! Execute generic workflow.")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                            
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                paths = emptyList()
                                actionText = "Awaiting input..."
                            }, 1000)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentPath.lineTo(change.position.x, change.position.y)
                            val updatedPath = androidx.compose.ui.graphics.Path()
                            updatedPath.addPath(currentPath)
                            currentPath = updatedPath
                        }
                    )
                }
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                paths.forEach { path ->
                    drawPath(
                        path = path,
                        color = JarvisCyan,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 12f,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                }
                drawPath(
                    path = currentPath,
                    color = Color.White,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 12f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { paths = emptyList(); actionText = "Cleared" },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("CLEAR GESTURE BUFFER")
        }
    }
}

@Composable
fun DownloadJarvisApkCard() {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFD500F9).copy(alpha = 0.12f),
                        JarvisCyan.copy(alpha = 0.12f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFFD500F9), JarvisCyan)
                ),
                RoundedCornerShape(16.dp)
            )
            .clickable {
                com.example.util.ApkExporter.exportAndShareApk(context)
            }
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "JARVIS INSTALL RESOURCE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisCyan,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFD500F9), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "STABLE APK",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Download & Install Jarvis",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Export running Jarvis.apk package straight to your public Downloads folder, and trigger android share menu immediately for easy remote installation.",
                    fontSize = 11.sp,
                    color = JarvisTextMuted,
                    lineHeight = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFF0F172A), CircleShape)
                    .border(1.dp, JarvisCyan, CircleShape)
                    .clickable {
                        com.example.util.ApkExporter.exportAndShareApk(context)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Export APK",
                    tint = JarvisCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

