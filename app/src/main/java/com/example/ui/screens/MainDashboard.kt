package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.ActiveLockEntity
import com.example.data.local.QuestEntity
import com.example.data.local.UserStats
import com.example.data.local.HabitLogEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(viewModel: GameViewModel) {
    val stats by viewModel.userStats.collectAsState()
    val quests by viewModel.quests.collectAsState()
    val habitLogs by viewModel.habitLogs.collectAsState()
    val activeLocks by viewModel.activeLocks.collectAsState()
    val isAILoading by viewModel.isAILoading.collectAsState()
    val systemNotification by viewModel.systemNotification.collectAsState()

    // Screen navigation state (0: Status, 1: Quests, 2: Shop, 3: Stats, 4: AI System)
    var activeTab by remember { mutableStateOf(0) }
    var showCreateQuestDialog by remember { mutableStateOf(false) }

    // Active lock simulator overlay state
    val simulatedApp by viewModel.simulatedCurrentOpenedApp.collectAsState()
    val now = remember { mutableStateOf(System.currentTimeMillis()) }

    // Tick system clock time for real-time timer calculations
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now.value = System.currentTimeMillis()
        }
    }

    // Determine if the current simulated app is locked out
    val isCurrentAppLocked = remember(simulatedApp, activeLocks, now.value) {
        val app = simulatedApp
        if (app != null) {
            val matchingLock = activeLocks.find { it.appName == app }
            matchingLock == null || matchingLock.expiresAt <= now.value
        } else {
            false
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SYSTEM // LIFE RPG",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = Color.White
                ),
                actions = {
                    // Quick stats indicators
                    stats?.let { s ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Paid,
                                contentDescription = "Gold",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${s.gold}G",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = {
                        activeTab = 0
                        viewModel.playClickSound()
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Status") },
                    label = { Text("Status", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonBlue,
                        selectedTextColor = NeonBlue,
                        unselectedIconColor = GrayText,
                        unselectedTextColor = GrayText,
                        indicatorColor = DarkSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = {
                        activeTab = 1
                        viewModel.playClickSound()
                    },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Quests") },
                    label = { Text("Quests", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonBlue,
                        selectedTextColor = NeonBlue,
                        unselectedIconColor = GrayText,
                        unselectedTextColor = GrayText,
                        indicatorColor = DarkSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = {
                        activeTab = 2
                        viewModel.playClickSound()
                    },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Shop") },
                    label = { Text("Shop", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonBlue,
                        selectedTextColor = NeonBlue,
                        unselectedIconColor = GrayText,
                        unselectedTextColor = GrayText,
                        indicatorColor = DarkSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = {
                        activeTab = 3
                        viewModel.playClickSound()
                    },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                    label = { Text("Stats", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonBlue,
                        selectedTextColor = NeonBlue,
                        unselectedIconColor = GrayText,
                        unselectedTextColor = GrayText,
                        indicatorColor = DarkSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = {
                        activeTab = 4
                        viewModel.playClickSound()
                    },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = "AI System") },
                    label = { Text("AI", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonBlue,
                        selectedTextColor = NeonBlue,
                        unselectedIconColor = GrayText,
                        unselectedTextColor = GrayText,
                        indicatorColor = DarkSurfaceVariant
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkBackground, CyberBlack)
                    )
                )
        ) {
            // Main views routing
            Crossfade(targetState = activeTab, label = "ScreenTransition") { tab ->
                when (tab) {
                    0 -> StatusPanel(viewModel, stats, activeLocks, now.value)
                    1 -> QuestsPanel(viewModel, quests, onAddQuest = { showCreateQuestDialog = true })
                    2 -> ShopPanel(viewModel, stats, activeLocks, now.value)
                    3 -> StatsPanel(viewModel, quests, habitLogs, stats)
                    4 -> AISystemPanel(viewModel, quests, isAILoading)
                }
            }

            // Floating Custom RPG-Style System notification overlay
            systemNotification?.let { note ->
                SystemNotificationOverlay(note, onDismiss = { viewModel.dismissNotification() })
            }

            // Custom Dialog to create Custom Quests
            if (showCreateQuestDialog) {
                CreateQuestDialog(
                    onDismiss = { showCreateQuestDialog = false },
                    onConfirm = { name, category, diff, dur ->
                        viewModel.createQuest(name, category, diff, dur)
                        showCreateQuestDialog = false
                    }
                )
            }

            // Active Lock Screen Simulator Overlay (triggers when inside simulator and access expires)
            if (simulatedApp != null) {
                if (isCurrentAppLocked) {
                    SystemLockScreenOverlay(
                        viewModel = viewModel,
                        appName = simulatedApp!!,
                        stats = stats,
                        onCloseSimulator = { viewModel.simulateOpeningApp(null) }
                    )
                } else {
                    // Running app header warning banner
                    val locks = activeLocks
                    val currentLock = locks.find { it.appName == simulatedApp }
                    val remainingMs = (currentLock?.expiresAt ?: 0) - now.value
                    val remainingSecs = maxOf(0, remainingMs / 1000)
                    val minutesLeft = remainingSecs / 60
                    val secondsLeft = remainingSecs % 60

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xE600E5FF))
                            .padding(8.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = CyberBlack,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Menjalankan $simulatedApp...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyberBlack,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format("%02d:%02d", minutesLeft, secondsLeft),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyberBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.simulateOpeningApp(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Exit App",
                                        tint = CyberBlack
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SUB-PANEL 0: STATUS & USER PROFILE
// -------------------------------------------------------------
@Composable
fun StatusPanel(
    viewModel: GameViewModel,
    stats: UserStats?,
    locks: List<ActiveLockEntity>,
    now: Long
) {
    val simulatedApp by viewModel.simulatedCurrentOpenedApp.collectAsState()

    if (stats == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonBlue)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Player header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Brush.linearGradient(listOf(NeonBlue, NeonPurple)))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Character Title and Name
                Text(
                    text = "[ ${stats.title.uppercase()} ]",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonPurple,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "MONARCH LEVEL ${stats.level}",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "RANK: ${stats.rank}",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonBlue,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                // CUSTOM CANVAS DRIVEN AVATAR CHARACTERS
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .border(2.dp, NeonBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarDrawing(stats.level, stats.balanceScore)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // EXP BAR
                val requiredExp = stats.level * 100
                val progress = (stats.exp.toFloat() / requiredExp).coerceIn(0f, 1f)
                val animatedProgress by animateFloatAsState(targetValue = progress, label = "ExpProgress")

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "EXP BAR",
                            style = MaterialTheme.typography.labelLarge,
                            color = GrayText,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${stats.exp} / ${requiredExp} EXP",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonBlue,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color.DarkGray, RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(listOf(NeonBlue, NeonCyan)),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }
        }

        // Sub currencies row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyberGray)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = NeonPurple)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("STREAK", style = MaterialTheme.typography.bodySmall, color = GrayText, fontFamily = FontFamily.Monospace)
                    Text("${stats.streak} HARI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyberGray)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HeartBroken, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("PENALTI TOKO", style = MaterialTheme.typography.bodySmall, color = GrayText, fontFamily = FontFamily.Monospace)
                    Text("+${stats.shopPenaltyPercent}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (stats.shopPenaltyPercent > 0) Color.Red else Color.Green)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyberGray)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AirplaneTicket, contentDescription = null, tint = NeonBlue)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("EMERGENCY PASS", style = MaterialTheme.typography.bodySmall, color = GrayText, fontFamily = FontFamily.Monospace)
                    Text("${stats.emergencyPasses} UNIT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Aura and Cosmetics Styles
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CyberGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "KOSMETIK & ATRIBUT",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonBlue,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Aura Pancaran", style = MaterialTheme.typography.bodyMedium, color = GrayText)
                    Text(stats.aura, style = MaterialTheme.typography.bodyMedium, color = NeonPurple, fontWeight = FontWeight.Bold)
                }
                Divider(color = CyberGray)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Skin Kosmetik", style = MaterialTheme.typography.bodyMedium, color = GrayText)
                    Text(stats.skin, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Divider(color = CyberGray)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Balance Score", style = MaterialTheme.typography.bodyMedium, color = GrayText)
                    Text("${stats.balanceScore} / 100", style = MaterialTheme.typography.bodyMedium, color = NeonBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        // APP LOCK SIMULATOR WIDGET (Mandatory for streaming preview testing!)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, NeonBlue)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhonelinkSetup, contentDescription = null, tint = NeonBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "APP MONITOR SIMULATOR (PENGUJI)",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonBlue,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Gunakan widget ini untuk menguji kunci aplikasi Accessibility secara instan di browser streaming! Pilih aplikasi untuk mensimulasikan pembukaannya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayText
                )

                Spacer(modifier = Modifier.height(12.dp))

                val apps = viewModel.getDefaultLockedApps()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    apps.chunked(3).forEach { rowApps ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowApps.forEach { app ->
                                Button(
                                    onClick = { viewModel.simulateOpeningApp(app) },
                                    modifier = Modifier.weight(1f).testTag("sim_app_$app"),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (simulatedApp == app) NeonBlue else Color.Transparent)
                                ) {
                                    Text(
                                        text = app.substringBefore(" "), // Short name
                                        fontSize = 11.sp,
                                        color = if (simulatedApp == app) NeonBlue else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        fontFamily = FontFamily.Monospace
                                    )
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
fun AvatarDrawing(level: Int, balanceScore: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "AvatarAnim")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotateAngle"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2.3f

        // Draw background rings
        val pulseRingCount = if (level >= 15) 3 else 1
        for (i in 1..pulseRingCount) {
            drawCircle(
                color = if (balanceScore >= 60) NeonPurple.copy(alpha = 0.15f / i) else NeonBlue.copy(alpha = 0.15f / i),
                radius = radius * (1.0f + (angle / 360f) * 0.15f * i),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Draw rotating magic circle
        val circlePath = Path()
        val numPoints = 6
        for (i in 0 until numPoints) {
            val theta = (angle + (i * 360f / numPoints)) * Math.PI / 180f
            val x = center.x + radius * cos(theta).toFloat()
            val y = center.y + radius * sin(theta).toFloat()
            if (i == 0) circlePath.moveTo(x, y) else circlePath.lineTo(x, y)
        }
        circlePath.close()

        drawPath(
            path = circlePath,
            color = if (balanceScore >= 60) NeonPurple.copy(alpha = 0.4f) else NeonBlue.copy(alpha = 0.4f),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Draw central player avatar silhouette
        val playerPath = Path()
        playerPath.moveTo(center.x, center.y - radius * 0.5f) // top
        playerPath.lineTo(center.x + radius * 0.3f, center.y + radius * 0.1f) // shoulder right
        playerPath.lineTo(center.x + radius * 0.15f, center.y + radius * 0.6f) // leg right
        playerPath.lineTo(center.x - radius * 0.15f, center.y + radius * 0.6f) // leg left
        playerPath.lineTo(center.x - radius * 0.3f, center.y + radius * 0.1f) // shoulder left
        playerPath.close()

        drawPath(
            path = playerPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    if (balanceScore >= 80) NeonPurple else NeonBlue,
                    CyberGray
                )
            )
        )

        // Draw dynamic crown
        if (level >= 10) {
            val crownPath = Path()
            crownPath.moveTo(center.x - radius * 0.15f, center.y - radius * 0.55f)
            crownPath.lineTo(center.x - radius * 0.08f, center.y - radius * 0.7f)
            crownPath.lineTo(center.x, center.y - radius * 0.6f)
            crownPath.lineTo(center.x + radius * 0.08f, center.y - radius * 0.7f)
            crownPath.lineTo(center.x + radius * 0.15f, center.y - radius * 0.55f)
            crownPath.close()

            drawPath(
                path = crownPath,
                color = Color(0xFFFFD700)
            )
        }
    }
}

// -------------------------------------------------------------
// SUB-PANEL 1: QUESTS & TIMER
// -------------------------------------------------------------
@Composable
fun QuestsPanel(
    viewModel: GameViewModel,
    quests: List<QuestEntity>,
    onAddQuest: () -> Unit
) {
    val activeQuest by viewModel.activeTimerQuest.collectAsState()
    val isCountingDown by viewModel.isCountingDown.collectAsState()
    val timerRemainingSeconds by viewModel.timerRemainingSeconds.collectAsState()
    val isTimerPaused by viewModel.isTimerPaused.collectAsState()
    val countdownSeconds by viewModel.countdownSeconds.collectAsState()

    if (activeQuest != null) {
        // TIMER ACTIVE VIEW OVERRIDES Standard lists
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "QUEST AKTIF",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonPurple,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = activeQuest!!.name.uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Kategori: ${activeQuest!!.category} • Kesulitan: ${activeQuest!!.difficulty}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayText
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (isCountingDown) {
                    // Pre-start 3-second countdown
                    Text("SISTEM DISETTING DALAM", style = MaterialTheme.typography.bodyLarge, color = NeonBlue, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "$countdownSeconds",
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 80.sp,
                        color = NeonPurple,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    // Running Timer Progress
                    val totalSecs = activeQuest!!.durationMinutes * 60
                    val currentProgress = if (totalSecs > 0) timerRemainingSeconds.toFloat() / totalSecs else 0f
                    val animatedProgress by animateFloatAsState(targetValue = currentProgress, label = "Progress")

                    Box(
                        modifier = Modifier.size(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = NeonBlue,
                            strokeWidth = 12.dp,
                            trackColor = CyberGray,
                            strokeCap = StrokeCap.Round,
                        )

                        // Timer text
                        val mins = timerRemainingSeconds / 60
                        val secs = timerRemainingSeconds % 60
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%02d:%02d", mins, secs),
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TERSISA",
                                style = MaterialTheme.typography.labelLarge,
                                color = GrayText,
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isTimerPaused) {
                            Button(
                                onClick = { viewModel.resumeQuestTimer() },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyberBlack)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("RESUME", color = CyberBlack, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.pauseQuestTimer() },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                border = BorderStroke(1.dp, NeonBlue)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null, tint = NeonBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PAUSE", color = NeonBlue, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Button(
                            onClick = { viewModel.cancelQuestTimer() },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("BATAL", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tab Header & Action Button
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DAFTAR QUEST",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )

            Button(
                onClick = {
                    viewModel.playClickSound()
                    onAddQuest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = CyberBlack, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("BUAT QUEST", fontSize = 11.sp, color = CyberBlack, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        // Filters / Mandatory warning
        val incompleteMandatory = quests.filter { it.isMandatory && !it.isCompleted }
        if (incompleteMandatory.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x33FF3D00)),
                border = BorderStroke(1.dp, Color.Red)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "PERINGATAN: Ada ${incompleteMandatory.size} Mandatory Quest yang harus diselesaikan hari ini sebelum rollover (besok) agar harga toko tidak naik!",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (quests.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AssignmentLate, contentDescription = null, tint = GrayText, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Belum ada Quest aktif harian.", color = GrayText, style = MaterialTheme.typography.bodyLarge)
                    Text("Gunakan 'BUAT QUEST' atau AI Analyzer untuk memicu Quest baru.", color = GrayText, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(quests.size) { index ->
                    val quest = quests[index]
                    QuestItem(quest = quest, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun QuestItem(quest: QuestEntity, viewModel: GameViewModel) {
    val isMandatory = quest.isMandatory

    Card(
        modifier = Modifier.fillMaxWidth().testTag("quest_item_${quest.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (quest.isCompleted) CyberGray.copy(alpha = 0.5f) else DarkSurface
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                quest.isCompleted -> Color.DarkGray
                isMandatory -> Color.Red
                else -> NeonBlue.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isMandatory) {
                            Box(
                                modifier = Modifier
                                    .background(Color.Red, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("MANDATORY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Box(
                            modifier = Modifier
                                .background(CyberGray, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(quest.category.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonBlue)
                        }
                        if (quest.durationMinutes > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("${quest.durationMinutes} MENIT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = quest.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (quest.isCompleted) GrayText else Color.White
                    )
                    if (quest.deadlineTime != null && !quest.isCompleted) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "DEADLINE: ${quest.deadlineTime}",
                            fontSize = 11.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Delete Button for Custom (non-mandatory)
                if (!isMandatory && !quest.isCompleted) {
                    IconButton(
                        onClick = { viewModel.deleteQuest(quest.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reward badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = "EXP", tint = NeonBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("+${quest.expReward} EXP", fontSize = 11.sp, color = NeonBlue, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Paid, contentDescription = "Gold", tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("+${quest.goldReward}G", fontSize = 11.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                if (quest.isCompleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Selesai", tint = Color.Green, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SELESAI", fontSize = 11.sp, color = Color.Green, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (quest.durationMinutes > 0) {
                            Button(
                                onClick = {
                                    viewModel.playClickSound()
                                    viewModel.startQuestTimer(quest)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("MULAI TIMER", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.quickCompleteQuest(quest.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp).testTag("quick_complete_${quest.id}")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = CyberBlack, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SELESAIKAN", fontSize = 10.sp, color = CyberBlack, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SUB-PANEL 2: SHOP (Gold buying hours)
// -------------------------------------------------------------
@Composable
fun ShopPanel(
    viewModel: GameViewModel,
    stats: UserStats?,
    locks: List<ActiveLockEntity>,
    now: Long
) {
    if (stats == null) return

    val multiplier = 1.0 + (stats.shopPenaltyPercent / 100.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SYSTEM SHOP",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberGray),
                border = BorderStroke(1.dp, NeonPurple)
            ) {
                Text(
                    text = "${stats.gold} GOLD AVAILABLE",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Active Penalty Multiplier Info
        if (stats.shopPenaltyPercent > 0) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x33FF3D00)),
                border = BorderStroke(1.dp, Color.Red)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "PENALTI TOKO AKTIF: +${stats.shopPenaltyPercent}%",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Karena Anda gagal menyelesaikan Mandatory Quests, seluruh item di toko berharga lebih tinggi dari standar. Selesaikan Mandatory Quests 3 hari berturut-turut untuk menghapus penalti ini.",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }

        Text(
            text = "BELI WAKTU HIBURAN (APP UNLOCK)",
            style = MaterialTheme.typography.labelLarge,
            color = NeonBlue,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val items = listOf(
            ShopItemData("Mobile Legends", 15, 10),
            ShopItemData("Mobile Legends", 30, 18),
            ShopItemData("Mobile Legends", 60, 32),
            ShopItemData("Free Fire", 30, 16),
            ShopItemData("TikTok", 15, 8),
            ShopItemData("TikTok", 30, 14),
            ShopItemData("YouTube", 15, 8),
            ShopItemData("YouTube", 30, 15),
            ShopItemData("Chess.com", 30, 10),
            ShopItemData("ANIMEIN", 30, 12),
            ShopItemData("Wibuku", 30, 12)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { item ->
                val finalPrice = (item.baseCostGold * multiplier).toInt()
                val matchingLock = locks.find { it.appName == item.appName }
                val remainingMs = if (matchingLock != null) matchingLock.expiresAt - now else 0
                val remainingSecs = maxOf(0, remainingMs / 1000)
                val isUnlocked = remainingSecs > 0

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("shop_item_${item.appName}_${item.minutes}"),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, if (isUnlocked) NeonBlue else CyberGray)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isUnlocked) NeonBlue else GrayText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Durasi Akses: ${item.minutes} Menit",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GrayText
                            )
                            if (isUnlocked) {
                                val m = remainingSecs / 60
                                val s = remainingSecs % 60
                                Text(
                                    text = String.format("Terbuka: %02d:%02d tersisa", m, s),
                                    fontSize = 11.sp,
                                    color = NeonBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.purchaseEntertainment(item.appName, item.minutes, item.baseCostGold) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Paid, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$finalPrice GOLD",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ShopItemData(val appName: String, val minutes: Int, val baseCostGold: Int)

// -------------------------------------------------------------
// SUB-PANEL 3: STATISTICS & CHARTS
// -------------------------------------------------------------
@Composable
fun StatsPanel(
    viewModel: GameViewModel,
    quests: List<QuestEntity>,
    habitLogs: List<HabitLogEntity>,
    stats: UserStats?
) {
    if (stats == null) return

    val totalCompleted = quests.count { it.isCompleted }
    val totalQuests = quests.size
    val completionRate = if (totalQuests > 0) (totalCompleted.toFloat() / totalQuests * 100).toInt() else 0

    // Compute workout/study hours from logs
    val totalStudyMins = habitLogs.filter { it.category == "Belajar" }.sumOf { it.durationMinutes }
    val totalWorkoutMins = habitLogs.filter { it.category == "Olahraga" }.sumOf { it.durationMinutes }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "STATISTIK & ANALISIS",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Streak & rate summary
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, CyberGray)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LAJU PENYELESAIAN", fontSize = 11.sp, color = GrayText, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$completionRate%", style = MaterialTheme.typography.displayMedium, color = NeonBlue, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalCompleted Selesai dari $totalQuests Quest", fontSize = 10.sp, color = GrayText)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, CyberGray)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL DURASI KERJA", fontSize = 11.sp, color = GrayText, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    val totalHrs = (totalStudyMins + totalWorkoutMins) / 60
                    val totalMins = (totalStudyMins + totalWorkoutMins) % 60
                    Text(
                        text = "${totalHrs}J ${totalMins}M",
                        style = MaterialTheme.typography.displayMedium,
                        color = NeonPurple,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Belajar: ${totalStudyMins}m • Olahraga: ${totalWorkoutMins}m", fontSize = 10.sp, color = GrayText)
                }
            }
        }

        // Custom drawn Canvas radar/bar habits breakdown chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, CyberGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BREAKDOWN DIMENSI HIDUP",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val categories = listOf("Belajar", "Olahraga", "Membaca", "Meditasi", "Kebersihan", "Kesehatan")
                val categoryColors = listOf(NeonBlue, NeonPurple, NeonCyan, Color.Green, Color.Yellow, Color.Magenta)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEachIndexed { idx, cat ->
                        val count = habitLogs.count { it.category == cat }
                        val maxCount = maxOf(1, habitLogs.groupBy { it.category }.values.maxOfOrNull { it.size } ?: 1)
                        val widthFactor = count.toFloat() / maxCount
                        val animatedWidth by animateFloatAsState(targetValue = widthFactor, label = "BarWidth")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cat,
                                modifier = Modifier.width(90.dp),
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(14.dp)
                                    .background(CyberGray, RoundedCornerShape(7.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedWidth.coerceIn(0.01f, 1.0f))
                                        .fillMaxHeight()
                                        .background(categoryColors[idx], RoundedCornerShape(7.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "$count kali",
                                fontSize = 11.sp,
                                color = categoryColors[idx],
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Streak stats logs
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, CyberGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LOG AKTIVITAS TERBARU",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (habitLogs.isEmpty()) {
                    Text("Belum ada log aktivitas. Selesaikan Quest Anda hari ini!", color = GrayText, fontSize = 12.sp)
                } else {
                    habitLogs.take(5).forEach { log ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(log.category, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(
                                text = "Durasi: ${log.durationMinutes} Menit",
                                color = GrayText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Divider(color = CyberGray)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SUB-PANEL 4: AI SYSTEM HABIT EVALUATION
// -------------------------------------------------------------
@Composable
fun AISystemPanel(
    viewModel: GameViewModel,
    quests: List<QuestEntity>,
    loading: Boolean
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, NeonBlue)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = NeonBlue,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ADAPTIVE SYSTEM AI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sistem cerdas menganalisis kebiasaan dan keseimbangan hidup Anda di bidang Belajar, Olahraga, Membaca, Meditasi, Kebersihan, dan Kesehatan harian. AI secara otomatis merumuskan 1-3 Mandatory Quests yang wajib diselesaikan sebelum rollover hari berikutnya.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (loading) {
                    CircularProgressIndicator(color = NeonBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "MENGANALISIS STATISTIK KEBIASAAN...",
                        fontSize = 11.sp,
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Button(
                        onClick = { viewModel.runAIEvaluation() },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("trigger_ai_evaluation"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = CyberBlack)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HUBUNGKAN & GENERATE MANDATORY QUESTS",
                            color = CyberBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Active Mandatory Quests Overview
        val mandatoryQuests = quests.filter { it.isMandatory }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, CyberGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MANDATORY QUESTS HARI INI",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonPurple,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (mandatoryQuests.isEmpty()) {
                    Text(
                        text = "Belum ada Mandatory Quest aktif. Jalankan evaluasi AI di atas untuk mendapatkan target harian Anda!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrayText
                    )
                } else {
                    mandatoryQuests.forEach { quest ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quest.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Deadline: ${quest.deadlineTime ?: "Rollover"} • ${quest.category}",
                                    fontSize = 11.sp,
                                    color = GrayText
                                )
                            }
                            if (quest.isCompleted) {
                                Text("COMPLETED", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            } else {
                                Text("PENDING", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Divider(color = CyberGray)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// REUSABLE POPUP DIALOGS & OVERLAYS
// -------------------------------------------------------------

// Immersive RPG System notifications
@Composable
fun SystemNotificationOverlay(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onDismiss() }
            .testTag("system_notification_card"),
        colors = CardDefaults.cardColors(containerColor = CyberBlack.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, NeonBlue)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SYSTEM MESSAGE",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = GrayText, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Dialog to create Quests
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuestDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, difficulty: String, duration: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Belajar") }
    var difficulty by remember { mutableStateOf("Medium") }
    var durationText by remember { mutableStateOf("15") }

    val categories = listOf("Belajar", "Olahraga", "Membaca", "Meditasi", "Kebersihan", "Kesehatan", "Custom")
    val difficulties = listOf("Easy", "Medium", "Hard")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().testTag("create_quest_dialog"),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, NeonBlue)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "BUAT QUEST BARU",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonBlue,
                    fontFamily = FontFamily.Monospace
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Quest") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = CyberGray,
                        focusedLabelColor = NeonBlue
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("quest_name_input")
                )

                // Category options
                Text("Kategori", style = MaterialTheme.typography.bodySmall, color = GrayText, fontFamily = FontFamily.Monospace)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonBlue,
                                selectedLabelColor = CyberBlack,
                                containerColor = CyberGray,
                                labelColor = Color.White
                            )
                        )
                    }
                }

                // Difficulty options
                Text("Tingkat Kesulitan", style = MaterialTheme.typography.bodySmall, color = GrayText, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    difficulties.forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { difficulty = diff },
                            label = { Text(diff) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPurple,
                                selectedLabelColor = Color.White,
                                containerColor = CyberGray,
                                labelColor = Color.White
                            )
                        )
                    }
                }

                // Duration field
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("Durasi Timer (Menit, 0 jika tanpa timer)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = CyberGray,
                        focusedLabelColor = NeonBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGray)
                    ) {
                        Text("BATAL", color = Color.White, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(
                                    name,
                                    category,
                                    difficulty,
                                    durationText.toIntOrNull() ?: 0
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("confirm_create_quest"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                    ) {
                        Text("BUAT", color = CyberBlack, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// APP LOCK SYSTEM OVERLAY SCREEN
// -------------------------------------------------------------
@Composable
fun SystemLockScreenOverlay(
    viewModel: GameViewModel,
    appName: String,
    stats: UserStats?,
    onCloseSimulator: () -> Unit
) {
    if (stats == null) return

    val multiplier = 1.0 + (stats.shopPenaltyPercent / 100.0)
    // Find matching shop cost for 15 minutes
    val baseCost = 8 // cost of TikTok/Youtube is 8 Emas standard
    val finalCost = (baseCost * multiplier).toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack.copy(alpha = 0.98f))
            .clickable(enabled = false) {} // block clicks below
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SYSTEM RESTRICTION",
                style = MaterialTheme.typography.displayMedium,
                color = Color.Red,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Akses ke '$appName' telah TERKUNCI. Anda melebihi jatah harian produktivitas Anda.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pilihlah untuk menukarkan Emergency Pass Anda atau beli durasi buka kunci di Shop menggunakan Emas yang diperoleh dari Quests!",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, Color.Red)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "EMERGENCY ACTION",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.useEmergencyPass(appName) },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("use_emergency_pass"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text(
                                text = "Gunakan Pass (${stats.emergencyPasses} Unit)",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = {
                                // Purchase instantly if they have gold
                                viewModel.purchaseEntertainment(appName, 15, baseCost)
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                        ) {
                            Text(
                                text = "Beli 15m ($finalCost Gold)",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onCloseSimulator,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberGray)
            ) {
                Text("KEMBALI KE SYSTEM DASHBOARD", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
