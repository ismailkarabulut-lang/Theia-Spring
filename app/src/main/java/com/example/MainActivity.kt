@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
package com.example

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.DatePicker
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.HealthProbe
import com.example.viewmodel.TheiaOverlay
import com.example.viewmodel.TheiaViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 42)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: TheiaViewModel = viewModel()
                TheiaMainPortalScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TheiaMainPortalScreen(viewModel: TheiaViewModel) {
    val activeOverlay by viewModel.activeOverlay.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordText by viewModel.voiceRecordingText.collectAsStateWithLifecycle()

    var systemTime by remember { mutableStateOf("00:00:00") }
    LaunchedEffect(Unit) {
        while (true) {
            val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            systemTime = formatter.format(Date())
            delay(1000)
        }
    }

    val lastInteractionTime by viewModel.lastInteractionTime.collectAsStateWithLifecycle()

    LaunchedEffect(lastInteractionTime, activeOverlay) {
        while (activeOverlay != TheiaOverlay.SCREENSAVER) {
            val elapsed = System.currentTimeMillis() - lastInteractionTime
            if (elapsed >= 15000) {
                viewModel.setOverlay(TheiaOverlay.SCREENSAVER)
                break
            }
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        viewModel.updateInteraction()
                    }
                }
            }
            .background(ThemeBg)
            .drawBehind {
                // Background matrix-cyberpunk glowing radial circles
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PurpleNeon.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.2f),
                        radius = size.minDimension * 0.6f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(TealNeon.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.8f),
                        radius = size.minDimension * 0.5f
                    )
                )
            }
    ) {
        // TOP HUD BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "THEIA",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Default
                )
                Text(
                    text = "·PORTAL",
                    color = PurpleNeon,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Default
                )
                Spacer(modifier = Modifier.width(12.dp))
                // Online pulsating status indicator
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(GreenNeon)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { viewModel.setOverlay(TheiaOverlay.TOPOLOGY) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, BorderColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "MİMARİ HARİTA ↗",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.setOverlay(TheiaOverlay.SCREENSAVER) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, BorderColor),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "MATRIX ⚡",
                        color = GreenNeon,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = systemTime,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        // CENTRAL PORTAL ORBITS AND INTERACTIVE CORE
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 90.dp, bottom = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            PortalOrbitDashboard(
                onCoreClick = { viewModel.setOverlay(TheiaOverlay.CHAT) },
                onNodeClick = { overlay -> viewModel.setOverlay(overlay) }
            )
        }

        // VOICE RECORDING OVERLAY BANNER
        androidx.compose.animation.AnimatedVisibility(
            visible = isRecording,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xE603020A))
                    .border(BorderStroke(1.dp, RedNeon.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "SES MODÜLÜ AKTİF", color = RedNeon, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = recordText, color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.stopVoiceRecordingAndSend() },
                        colors = ButtonDefaults.buttonColors(containerColor = RedNeon)
                    ) {
                        Text(text = "GÖNDER", color = Color.White)
                    }
                }
            }
        }

        // ENCAPSULATED MODULAR SYSTEM OVERLAYS
        androidx.compose.animation.AnimatedVisibility(
            visible = activeOverlay != TheiaOverlay.NONE,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ThemeBg)
            ) {
                when (activeOverlay) {
                    TheiaOverlay.CHAT -> ChatOverlayScreen(viewModel = viewModel)
                    TheiaOverlay.PERSONA -> PersonaOverlayScreen(viewModel = viewModel)
                    TheiaOverlay.VAULT -> VaultOverlayScreen(viewModel = viewModel)
                    TheiaOverlay.SAGLIK -> SaglikOverlayScreen(viewModel = viewModel)
                    TheiaOverlay.GOREV -> GorevOverlayScreen(viewModel = viewModel)
                    TheiaOverlay.TEAM -> TeamOverlayScreen(viewModel = viewModel)
                    TheiaOverlay.TOPOLOGY -> TopologyOverlayScreen(viewModel = viewModel)
                    TheiaOverlay.SCREENSAVER -> MatrixScreensaver(onDismiss = { viewModel.setOverlay(TheiaOverlay.NONE) })
                    else -> {}
                }
            }
        }
    }
}

class MatrixColumn(
    var y: Float,
    val speed: Float,
    val chars: List<String>,
    val color: Color
)

val MatrixColors = listOf(
    Color(0xFF7C6EF5), // Purple
    Color(0xFF2DD4BF), // Teal
    Color(0xFFEF9F27), // Orange
    Color(0xFF4FA8D5), // Blue
    Color(0xFFC77DFF), // Pink
    Color(0xFF1D9E75), // Green
    Color(0xFFE8B86D)  // Gold
)

@Composable
fun MatrixScreensaver(
    onDismiss: () -> Unit,
    color: Color = GreenNeon
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020205))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        
        val fontSizePx = 36f
        val numColumns = (width / fontSizePx).toInt().coerceAtLeast(1)
        
        val columns = remember(numColumns) {
            List(numColumns) { colIndex ->
                val randomY = -(Math.random() * height * 1.5).toFloat()
                val speed = (8f + Math.random() * 16f).toFloat()
                val length = (8 + (Math.random() * 12).toInt())
                val randomColor = MatrixColors.random()
                
                val chars = List(length) {
                    val available = "0123456789ABCDEFｦｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ"
                    available[(Math.random() * available.length).toInt()].toString()
                }
                
                MatrixColumn(
                    y = randomY,
                    speed = speed,
                    chars = chars,
                    color = randomColor
                )
            }
        }
        
        var tick by remember { mutableLongStateOf(0L) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(30)
                tick++
            }
        }
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dummyVal = tick
            
            columns.forEachIndexed { index, col ->
                col.y += col.speed
                if (col.y > height + 200f) {
                    col.y = -200f
                }
                
                val x = index * fontSizePx
                
                col.chars.forEachIndexed { charIndex, char ->
                    val yVal = col.y - (charIndex * fontSizePx)
                    if (yVal in -50f..height) {
                        val alpha = (1.0f - (charIndex.toFloat() / col.chars.size)).coerceIn(0.1f, 1.0f)
                        val textColor = if (charIndex == 0) {
                            Color.White
                        } else if (charIndex < 3) {
                            col.color.copy(alpha = alpha)
                        } else {
                            col.color.copy(alpha = alpha * 0.7f)
                        }
                        
                        drawContext.canvas.nativeCanvas.drawText(
                            char,
                            x + 5f,
                            yVal,
                            android.graphics.Paint().apply {
                                setColor(textColor.toArgb())
                                setTextSize(fontSizePx * 0.8f)
                                setTypeface(android.graphics.Typeface.MONOSPACE)
                                setShadowLayer(10f, 0f, 0f, textColor.toArgb())
                            }
                        )
                    }
                }
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Column {
                Text(
                    text = "THEIA STATUS SECURITY // RE-ENCRYPT...",
                    color = color.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "COROSCALE CODES SHUNTING · TOUCH SCREEN TO RESTORE SYS",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// PORTAL ORBIT ENGINE WITH PERFECT TRIGONOMETRIC POSITIONS
@Composable
fun PortalOrbitDashboard(
    onCoreClick: () -> Unit,
    onNodeClick: (TheiaOverlay) -> Unit
) {
    val transition = rememberInfiniteTransition()
    val orbitRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .size(310.dp)
            .drawBehind {
                // Orbit path circles
                drawCircle(
                    color = Color.White.copy(alpha = 0.03f),
                    radius = size.minDimension * 0.46f,
                    style = Stroke(width = 1f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 15f)))
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Core center breath indicator circle
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(PurpleNeon.copy(alpha = 0.25f), PurpleNeon.copy(alpha = 0.03f), Color.Transparent)
                    )
                )
                .border(BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.45f)), CircleShape)
                .clickable { onCoreClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "💬", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "CHAT",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "theia portal",
                    color = TextMuted,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Orbit nodes matching web indices precisely (SES, GÖRÜNTÜ, PERSONA, VAULT, SAĞLIK, GATE, GÖREV, TEAM)
        val modulesList = listOf(
            ModuleNodeItem("🎙", "SES", RedNeon, TheiaOverlay.CHAT),
            ModuleNodeItem("👁", "GÖRÜNTÜ", BlueNeon, TheiaOverlay.CHAT), // fallback
            ModuleNodeItem("🧠", "PERSONA", PinkNeon, TheiaOverlay.PERSONA),
            ModuleNodeItem("🗄", "VAULT", OrangeNeon, TheiaOverlay.VAULT),
            ModuleNodeItem("⚡", "SAĞLIK", GreenNeon, TheiaOverlay.SAGLIK),
            ModuleNodeItem("🛡", "GATE", RedNeon, TheiaOverlay.CHAT), // loads chat with GK drawer trigger
            ModuleNodeItem("📋", "GÖREV", GoldNeon, TheiaOverlay.GOREV),
            ModuleNodeItem("🎯", "TEAM", TealNeon, TheiaOverlay.TEAM)
        )

        modulesList.forEachIndexed { index, item ->
            // Simulating precise math angles derived from node arrays index
            val angleBasis = (index.toFloat() / modulesList.size.toFloat()) * 2.0 * Math.PI - (Math.PI / 2.0)
            val currentRotationOffset = Math.toRadians(orbitRotation.toDouble())
            val finalAngle = angleBasis + currentRotationOffset
            val orbitRadius = 120.dp

            val offsetX = (orbitRadius * cos(finalAngle).toFloat())
            val offsetY = (orbitRadius * sin(finalAngle).toFloat())

            Box(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(62.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, item.col.copy(alpha = 0.35f)), RoundedCornerShape(12.dp))
                    .background(item.col.copy(alpha = 0.08f))
                    .clickable { onNodeClick(item.overlay) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = item.glyph, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        color = item.col,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

data class ModuleNodeItem(
    val glyph: String,
    val label: String,
    val col: Color,
    val overlay: TheiaOverlay
)

// MODULE 1: CHAT PORTAL COMPONENT
@Composable
fun ChatOverlayScreen(viewModel: TheiaViewModel) {
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    val ttsEnabled by viewModel.ttsEnabled.collectAsStateWithLifecycle()
    val ttsRate by viewModel.ttsRate.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.selectedSessionId.collectAsStateWithLifecycle()
    val currentModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val currentModelColor by viewModel.selectedModelColor.collectAsStateWithLifecycle()
    val chatSessions by viewModel.chatSessions.collectAsStateWithLifecycle()
    val riskLevel by viewModel.riskLevel.collectAsStateWithLifecycle()
    val gkLogs by viewModel.gkLogs.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    var showSessionDrawer by remember { mutableStateOf(false) }
    var showGkDrawer by remember { mutableStateOf(false) }
    var showModelDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBg)
            .navigationBarsPadding()
    ) {
        // CHAT HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Color(0x33000000))
                .border(BorderStroke(1.dp, BorderColor))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.setOverlay(TheiaOverlay.NONE) }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, tint = TextMuted, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "CHAT · PORTAL",
                        color = PurpleNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "SES: $currentSessionId",
                        color = TextMuted,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { showSessionDrawer = !showSessionDrawer },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text(text = "🗂 GEÇMİŞ", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.width(6.dp))
                // GK risk level badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(RedNeon.copy(alpha = 0.1f))
                        .border(BorderStroke(1.dp, if (riskLevel == "CRITICAL" || riskLevel == "HIGH") RedNeon else GreenNeon), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = "GK: $riskLevel", color = if (riskLevel == "CRITICAL" || riskLevel == "HIGH") RedNeon else GreenNeon, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.width(6.dp))
                // Model dropdown selector button
                Box {
                    Button(
                        onClick = { showModelDropdown = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(currentModelColor)))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = currentModel.uppercase(), color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = "▾", color = TextMuted, fontSize = 8.sp)
                    }

                    DropdownMenu(
                        expanded = showModelDropdown,
                        onDismissRequest = { showModelDropdown = false },
                        modifier = Modifier.background(Color(0xFF080516))
                    ) {
                        DropdownMenuItem(
                            text = { Text("CLAUDE", color = PurpleNeon, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            onClick = {
                                viewModel.selectModel("claude", "#7C6EF5")
                                showModelDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("DEEPSEEK", color = BlueNeon, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            onClick = {
                                viewModel.selectModel("deepseek", "#4FA8D5")
                                showModelDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("KIMI", color = PinkNeon, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            onClick = {
                                viewModel.selectModel("kimi", "#C77DFF")
                                showModelDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("OLLAMA", color = GreenNeon, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            onClick = {
                                viewModel.selectModel("ollama", "#1D9E75")
                                showModelDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            // MAIN MESSAGE CONVERSATION FEED
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "T",
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White.copy(alpha = 0.04f)
                                )
                                Text(
                                    text = "AWAITING INPUT · KAPTAN",
                                    color = TextDim,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                // Prompt seed chips
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(0.9f),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "bugün ne yaptım?",
                                        "🌍 son haberler",
                                        "hafıza durumunu göster",
                                        "sistem durumu"
                                    ).forEach { query ->
                                        Button(
                                            onClick = { textInput = query },
                                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor),
                                            border = BorderStroke(1.dp, BorderColor),
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Text(text = query, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                items(messages) { message ->
                    val isUser = message.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!isUser) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PurpleNeon.copy(alpha = 0.15f))
                                        .border(BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.35f)), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "T", color = PurpleNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 270.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isUser) OrangeNeon.copy(alpha = 0.08f) else PurpleNeon.copy(alpha = 0.06f))
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                if (isUser) OrangeNeon.copy(alpha = 0.2f) else PurpleNeon.copy(alpha = 0.16f)
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = message.content,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = message.time, color = TextDim, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            }

                            if (isUser) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(OrangeNeon.copy(alpha = 0.15f))
                                        .border(BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.35f)), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "K", color = OrangeNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (isThinking) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PurpleNeon.copy(alpha = 0.05f))
                                    .border(BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.14f)), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.dp, color = PurpleNeon)
                                    Text(text = "Theia düşünüyor...", color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            // PAST SESSION SIDEBAR GRAWER (Slide-in)
            androidx.compose.animation.AnimatedVisibility(
                visible = showSessionDrawer,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .width(260.dp)
                    .background(Color(0xFF060312))
                    .border(BorderStroke(1.dp, BorderColor))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "// OTURUM GEÇMİŞİ", color = TealNeon, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        IconButton(onClick = { showSessionDrawer = false }) {
                            Icon(imageVector = Icons.Default.Close, tint = Color.White, contentDescription = "Close")
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.createNewSession()
                            showSessionDrawer = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(text = "+ Yeni Oturum", color = PurpleNeon, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        items(chatSessions) { session ->
                            val isCurrent = session.id == currentSessionId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) TealNeon.copy(alpha = 0.08f) else Color.Transparent)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isCurrent) TealNeon.copy(alpha = 0.3f) else Color.Transparent
                                        ),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.selectSession(session.id)
                                        showSessionDrawer = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = session.id, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text(text = session.preview, color = TextMuted, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(session.modelColor)))
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            // GATEKEEPER LIVE REGISTRY AUDIT DRAWER (Bottom-slide)
            androidx.compose.animation.AnimatedVisibility(
                visible = showGkDrawer,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(Color(0xFF080414))
                    .border(BorderStroke(1.dp, RedNeon.copy(alpha = 0.2f)))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "// GATEKEEPER AUDIT LOGS", color = RedNeon, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        IconButton(onClick = { showGkDrawer = false }) {
                            Icon(imageVector = Icons.Default.Close, tint = Color.White, contentDescription = "Close")
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        items(gkLogs) { log ->
                            Text(text = log, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }

        // CHAT CONTROLLER BOTTOM INPUT WORKSPACE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xE6000000))
                .border(BorderStroke(1.dp, BorderColor))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = ">_", color = PurpleNeon, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                    TextField(
                        value = textInput,
                        onValueChange = { 
                            textInput = it 
                            viewModel.updateInteraction()
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Mesajını gir, Kaptan... (🌍 prefix = web, 🎯 = team)", color = TextDim, fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.startVoiceRecording() },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceColor)
                                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
                        ) {
                            Icon(imageVector = Icons.Default.Mic, tint = RedNeon, contentDescription = "Mic")
                        }

                        IconButton(
                            onClick = { if (!textInput.startsWith("🌍")) textInput = "🌍 $textInput" },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceColor)
                                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
                        ) {
                            Text(text = "🌍", fontSize = 15.sp)
                        }

                        IconButton(
                            onClick = { showGkDrawer = !showGkDrawer },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceColor)
                                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
                        ) {
                            Icon(imageVector = Icons.Default.Shield, tint = GoldNeon, contentDescription = "Shield")
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${textInput.length}",
                            color = TextDim,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Button(
                            onClick = {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleNeon),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(text = "GÖNDER", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    }
}

// MODULE 2: PERSONA ANNOTATOR OVERLAY
@Composable
fun PersonaOverlayScreen(viewModel: TheiaViewModel) {
    val logs by viewModel.memoryLogs.collectAsStateWithLifecycle()
    val textMessages by viewModel.currentMessages.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBg)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // PERSONA HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.setOverlay(TheiaOverlay.NONE) }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, tint = TextMuted, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "PERSONA SNAPSHOT",
                        color = TealNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(text = "v1.0 · real-time analytics", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GRID METRICS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(SurfaceColor, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(text = "PEAK SAAT", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text(text = "Per 02:00", color = TealNeon, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(SurfaceColor, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(text = "HAFIZA REG.", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text(text = "${logs.size} aktif", color = PurpleNeon, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(SurfaceColor, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(text = "MESAJ HACMİ", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text(text = "334", color = OrangeNeon, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // N-Gram Frekansı lists
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "N-GRAM FREKANSI · TOP LEITMOTIFS",
                            color = TealNeon,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        listOf(
                            Pair("yapay zekanın hafıza kazanması", 0.95f),
                            Pair("sistem prompt kurgusu", 0.8f),
                            Pair("gatekeeper engelleme mekanizmaları", 0.65f),
                            Pair("Ollama local kiralama motoru", 0.5f),
                            Pair("asenkron brief raporu", 0.35f)
                        ).forEach { (term, weight) ->
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = term, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text(text = "${(weight * 100).toInt()}%", color = GoldNeon, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                LinearProgressIndicator(
                                    progress = { weight },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    color = GoldNeon,
                                    trackColor = BorderColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            // Central Density Heatmap
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ORTALAMA HAFTALIK AKTİVİTE YOĞUNLUĞU",
                            color = TealNeon,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // Simulated grid heatmap
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz").forEach { day ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = day, color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    repeat(8) { hourIndex ->
                                        val randomBrightness = (hourIndex * 15 + day.hashCode().absoluteValue % 100) % 255
                                        val heatColor = TealNeon.copy(alpha = (randomBrightness / 255f).coerceIn(0.04f, 0.95f))
                                        Box(
                                            modifier = Modifier
                                                .padding(vertical = 1.6.dp)
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(heatColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Leitmotif card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceColor)
                        .border(BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.25f)), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(text = "TOP LEITMOTIF", color = PurpleNeon, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "İsmail Kaptan'ın theia veritabanı kiralama işlemlerini android framework seviyesine geçirerek, asenkron SQLite senkronizasyon başarısını kutladığı an.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

// MODULE 3: VAULT STORE EXPANDABLE LOGS
@Composable
fun VaultOverlayScreen(viewModel: TheiaViewModel) {
    val databaseLogs by viewModel.memoryLogs.collectAsStateWithLifecycle()
    var expandedKey by remember { mutableStateOf<String?>(null) }
    var selectedTabFilter by remember { mutableStateOf("all") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBg)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.setOverlay(TheiaOverlay.NONE) }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, tint = TextMuted, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "VAULT · HAFİZA",
                        color = OrangeNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(text = "${databaseLogs.size} records found", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // COUNT DECAY HEADER CARDS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("ACTIVE", databaseLogs.filter { it.status == "active" }.size, GreenNeon),
                Triple("PASSIVE", databaseLogs.filter { it.status == "passive" }.size, OrangeNeon),
                Triple("ARCHIVED", databaseLogs.filter { it.status == "archived" }.size, TextDim)
            ).forEach { (label, count, color) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceColor, RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, color.copy(alpha = 0.15f)), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = label, color = TextMuted, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text(text = "$count", color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // TAB FILTER LIST
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("all", "core", "daily_summary", "memory").forEach { type ->
                Button(
                    onClick = { selectedTabFilter = type },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTabFilter == type) OrangeNeon.copy(alpha = 0.15f) else SurfaceColor
                    ),
                    border = BorderStroke(1.dp, if (selectedTabFilter == type) OrangeNeon else BorderColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(text = type.uppercase(), color = if (selectedTabFilter == type) OrangeNeon else TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // KEY VALUE VALUE DATABASE
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filteredList = databaseLogs.filter {
                selectedTabFilter == "all" || it.entryType == selectedTabFilter
            }

            if (filteredList.isEmpty()) {
                item {
                    Text(
                        text = "Filtreye uygun bellek bulunamadı.",
                        color = TextDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            items(filteredList) { entry ->
                val isExpanded = entry.key == expandedKey
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    border = BorderStroke(1.dp, if (isExpanded) OrangeNeon.copy(alpha = 0.45f) else BorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedKey = if (isExpanded) null else entry.key }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (entry.entryType == "core") TealNeon.copy(alpha = 0.1f) else OrangeNeon.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = entry.entryType.uppercase(),
                                        color = if (entry.entryType == "core") TealNeon else OrangeNeon,
                                        fontSize = 7.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = entry.key,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(text = entry.updatedAt, color = TextDim, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = BorderColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = entry.value,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

// MODULE 4: SAĞLIK CONTROLLER DIAGNOSTIC SYSTEM
@Composable
fun SaglikOverlayScreen(viewModel: TheiaViewModel) {
    val healthProbes by viewModel.healthProbes.collectAsStateWithLifecycle()
    val statusText by viewModel.healthStatus.collectAsStateWithLifecycle()
    val latencyHistory by viewModel.latencyHistory.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBg)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.setOverlay(TheiaOverlay.NONE) }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, tint = TextMuted, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "SAĞLIK · DURUM TEST",
                        color = GreenNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(text = statusText, color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Button(
                onClick = { viewModel.loadDiagnostics() },
                colors = ButtonDefaults.buttonColors(containerColor = GreenNeon.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, GreenNeon)
            ) {
                Text(text = "↻ YENİLE", color = GreenNeon, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // STATUS BLOCKS CARDS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("TOTAL PROBES", healthProbes.size, GreenNeon),
                Triple("ONLINE", healthProbes.filter { it.status == "ok" }.size, GreenNeon),
                Triple("DEGRADED", healthProbes.filter { it.status == "warn" }.size, OrangeNeon),
                Triple("OFFLINE", healthProbes.filter { it.status == "err" }.size, RedNeon)
            ).forEach { (label, count, color) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceColor, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(text = label, color = TextMuted, fontSize = 6.5.sp, fontFamily = FontFamily.Monospace)
                        Text(text = "$count", color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // SPARKLINE CHARTS DRAWING BACKGROUND LATENCY HISTORY
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "LATENCY SPARKLINE GEÇMİŞİ · SON 10 ÖLÇÜM",
                            color = GreenNeon,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        healthProbes.forEach { probe ->
                            val history = latencyHistory[probe.id] ?: emptyList()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = probe.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(80.dp)
                                )

                                // Custom canvas sparkline graph implementation
                                Canvas(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(24.dp)
                                        .padding(horizontal = 12.dp)
                                ) {
                                    if (history.isNotEmpty()) {
                                        val path = Path()
                                        val stepX = size.width / (history.size - 1).coerceAtLeast(1)
                                        val minVal = 0f
                                        val maxVal = history.maxOrNull()?.toFloat() ?: 100f
                                        val diff = (maxVal - minVal).coerceAtLeast(1f)

                                        history.forEachIndexed { index, value ->
                                            val x = index * stepX
                                            val y = size.height - ((value.toFloat() - minVal) / diff * size.height)
                                            if (index == 0) {
                                                path.moveTo(x, y)
                                            } else {
                                                path.lineTo(x, y)
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = GreenNeon,
                                            style = Stroke(width = 2f)
                                        )
                                    }
                                }

                                Text(
                                    text = "${probe.latency}ms",
                                    color = if (probe.latency < 50) GreenNeon else OrangeNeon,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(50.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }

            // Real status rows
            item {
                Text(
                    text = "AKTİF ENDPOINT DURUMLARI",
                    color = TextMuted,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }

            items(healthProbes) { service ->
                val badgeColor = when (service.status) {
                    "ok" -> GreenNeon
                    "warn" -> OrangeNeon
                    else -> RedNeon
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceColor)
                        .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = service.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = service.desc, color = TextMuted, fontSize = 9.sp)
                        }
                    }

                    Text(
                        text = if (service.status == "ok") "ONLINE" else "DEGRADED",
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// MODULE 5: GÖREVLER (CALENDAR TASK SCHEDULER BOARD)
@Composable
fun GorevOverlayScreen(viewModel: TheiaViewModel) {
    val tasks by viewModel.gorevlerList.collectAsStateWithLifecycle()
    var selectedGorevTab by remember { mutableStateOf("liste") }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Add Task Field variables
    var taskTitle by remember { mutableStateOf("") }
    var taskDate by remember { mutableStateOf("") }
    var taskTime by remember { mutableStateOf("") }
    var taskRepeat by remember { mutableStateOf("yok") }
    var taskCategory by remember { mutableStateOf("kisisel") }
    var taskType by remember { mutableStateOf("gorev") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBg)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.setOverlay(TheiaOverlay.NONE) }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, tint = TextMuted, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "GÖREVLER PANELİ",
                        color = GoldNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(text = "${tasks.filter { !it.done }.size} aktif görev", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // TAB COMPONENT (LIST / CALENDAR / ARCHIVE)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x33000000))
                .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(4.dp))
        ) {
            listOf("liste", "takvim", "arsiv").forEach { tab ->
                Button(
                    onClick = { selectedGorevTab = tab },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedGorevTab == tab) SurfaceColorLight else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        text = tab.uppercase(),
                        color = if (selectedGorevTab == tab) GoldNeon else TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedGorevTab) {
                "liste" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val activeList = tasks.filter { !it.done }
                            if (activeList.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "Aktif göreviniz bulunmuyor.", color = TextDim, fontSize = 12.sp)
                                    }
                                }
                            }
                            items(activeList) { gorev ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceColor)
                                        .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = gorev.done,
                                        onCheckedChange = { viewModel.toggleGorev(gorev.id, it) },
                                        colors = CheckboxDefaults.colors(checkedColor = GreenNeon)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = gorev.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(text = gorev.date, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                            if (gorev.time.isNotEmpty()) {
                                                Text(text = "· ${gorev.time}", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (gorev.type == "rutin") PurpleNeon.copy(alpha = 0.1f) else GreenNeon.copy(alpha = 0.1f)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = gorev.type.uppercase(),
                                            color = if (gorev.type == "rutin") PurpleNeon else GreenNeon,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // FAB FLOATING BUTTON
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            ExtendedFloatingActionButton(
                                onClick = { showAddTaskDialog = true },
                                containerColor = PurpleNeon,
                                icon = { Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White) },
                                text = { Text(text = "YENİ GÖREV", color = Color.White, fontSize = 9.sp, letterSpacing = 1.sp) }
                            )
                        }
                    }
                }

                "takvim" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        CalendarViewWidget(tasks = tasks)
                    }
                }

                "arsiv" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val archives = tasks.filter { it.done }
                        if (archives.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "Arşivlenmiş tamamlanmış görev yok.", color = TextDim, fontSize = 12.sp)
                                }
                            }
                        }
                        items(archives) { archive ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceColor)
                                    .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = true,
                                    onCheckedChange = { viewModel.toggleGorev(archive.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = GreenNeon)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = archive.title,
                                        color = TextMuted,
                                        fontSize = 14.sp,
                                        style = TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                                    )
                                    Text(text = "Tamamlandı: ${archive.date}", color = TextDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                                IconButton(onClick = { viewModel.deleteGorev(archive.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RedNeon)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CUSTOM ADD GOREV MODAL SHEET
    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            containerColor = Color(0xFF0F0C22),
            title = {
                Text(
                    text = "YENİ GÖREV EKLE",
                    color = PurpleNeon,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Başlık", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleNeon,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = taskDate,
                            onValueChange = { taskDate = it },
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val cal = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _: DatePicker, y: Int, m: Int, d: Int ->
                                            taskDate = "$y-${String.format("%02d", m + 1)}-${String.format("%02d", d)}"
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                            enabled = false,
                            label = { Text("Tarih", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = BorderColor,
                                disabledTextColor = Color.White,
                                disabledLabelColor = TextMuted
                            )
                        )

                        OutlinedTextField(
                            value = taskTime,
                            onValueChange = { taskTime = it },
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val cal = Calendar.getInstance()
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            taskTime = "${String.format("%02d", hour)}:${String.format("%02d", minute)}"
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                            enabled = false,
                            label = { Text("Saat", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = BorderColor,
                                disabledTextColor = Color.White,
                                disabledLabelColor = TextMuted
                            )
                        )
                    }

                    // Repeated settings chips
                    Text(text = "Rutin Tekrar Ayarı", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("yok", "gunluk", "haftalik", "ozel").forEach { rep ->
                            FilterChip(
                                selected = taskRepeat == rep,
                                onClick = { taskRepeat = rep },
                                label = { Text(text = rep, fontSize = 9.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurpleNeon.copy(alpha = 0.2f))
                            )
                        }
                    }

                    // Categories chip selectors
                    Text(text = "Kategori", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("saglik", "kisisel", "is").forEach { cat ->
                            FilterChip(
                                selected = taskCategory == cat,
                                onClick = { taskCategory = cat },
                                label = { Text(text = cat, fontSize = 9.sp) }
                            )
                        }
                    }

                    // Task types chip selectors
                    Text(text = "Görev Türü", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("gorev", "rutin").forEach { type ->
                            FilterChip(
                                selected = taskType == type,
                                onClick = { taskType = type },
                                label = { Text(text = type, fontSize = 9.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotEmpty()) {
                            viewModel.addNewGorev(
                                taskTitle,
                                if (taskDate.isEmpty()) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) else taskDate,
                                taskTime,
                                taskRepeat,
                                taskCategory,
                                taskType
                            )
                        }
                        showAddTaskDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleNeon)
                ) {
                    Text(text = "Kaydet", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text(text = "İptal", color = TextMuted)
                }
            }
        )
    }
}

@Composable
fun CalendarViewWidget(tasks: List<Gorev>) {
    var selectedDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pa").forEach { name ->
                        Text(
                            text = name,
                            color = TextDim,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Simulated Calendar days grid
                val cal = Calendar.getInstance()
                val currentMonthDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val formatter = SimpleDateFormat("yyyy-MM-", Locale.getDefault())
                val prefix = formatter.format(Date())

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val rowsCount = (currentMonthDays + 6) / 7
                    for (row in 0 until rowsCount) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0 until 7) {
                                val dayNum = row * 7 + col + 1
                                if (dayNum <= currentMonthDays) {
                                    val formattedDate = prefix + String.format("%02d", dayNum)
                                    val hasTask = tasks.any { it.date == formattedDate && !it.done }
                                    val isSelected = formattedDate == selectedDate

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) PurpleNeon.copy(alpha = 0.25f) else Color.Transparent
                                            )
                                            .clickable { selectedDate = formattedDate }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$dayNum",
                                                color = if (isSelected) PurpleNeon else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            if (hasTask) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(3.dp)
                                                        .clip(CircleShape)
                                                        .background(GreenNeon)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(text = "SEÇİLİ TARİHTEKİ GÖREVLER", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val dailyTasks = tasks.filter { it.date == selectedDate && !it.done }
            if (dailyTasks.isEmpty()) {
                item {
                    Text(text = "Bu tarihte planlanmış aktif görev yok.", color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }
            } else {
                items(dailyTasks) { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceColor, RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TealNeon)
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = task.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(text = task.time, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// MODULE 6: TEAM OVERLAY (BİLİŞSEL MERCEK MULTI-AGENT STATE MONITOR)
@Composable
fun TeamOverlayScreen(viewModel: TheiaViewModel) {
    val teamAnalyses by viewModel.teamAnalyses.collectAsStateWithLifecycle()
    val activeAnalysis = teamAnalyses.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBg)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.setOverlay(TheiaOverlay.NONE) }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, tint = TextMuted, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "TEAM · BİLİŞSEL MERCEK",
                        color = GoldNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "v1.0 · multi-agent support model",
                        color = TextMuted,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CENTRAL SECRETARY SUMMARY STATEMENT CONSOLE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    border = BorderStroke(1.dp, GoldNeon.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SEKRETER ÖZETİ",
                            color = GoldNeon,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (activeAnalysis == null) {
                            Text(
                                text = "Henüz 🎯 prefix analizi yapılmamış, Kaptan. Chat alanında mesajınızın başına 🎯 koyarak başlatabilirsiniz.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        } else {
                            val s = activeAnalysis.sekreter
                            listOf(
                                Pair("konsensus", s.konsensus),
                                Pair("ana ayrışma", s.anaAyrisma),
                                Pair("görünmeyen risk", s.gorunmeyenRisk),
                                Pair("ucuz test", s.ucuzTest),
                                Pair("karar tipi", s.kararTipi),
                                Pair("ertelenen konu", s.ertelenenKonu),
                                Pair("açık soru", s.acikSoru),
                                Pair("sonraki adım", s.sonrakiAdim)
                            ).forEach { (label, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = label.uppercase(),
                                        color = GoldNeon,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(110.dp)
                                    )
                                    Text(
                                        text = value,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5 ADHERING COGNITIVE AGENTS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val mimarText = activeAnalysis?.mimarText ?: "Analiz bekleniyor..."
                    val tarihciText = activeAnalysis?.tarihciText ?: "Analiz bekleniyor..."
                    val antitezText = activeAnalysis?.antitezText ?: "Analiz bekleniyor..."
                    val toplumcuText = activeAnalysis?.toplumcuText ?: "Analiz bekleniyor..."
                    val stratejistText = activeAnalysis?.stratejistText ?: "Analiz bekleniyor..."

                    TeamAgentItemRow("🏗 MİMAR", "yapısal iç görü", BlueNeon, mimarText)
                    TeamAgentItemRow("📜 TARİHÇİ", "zamansal iç görü", PinkNeon, tarihciText)
                    TeamAgentItemRow("⚡ ANTİTEZ", "pragmatik iç görü", RedNeon, antitezText)
                    TeamAgentItemRow("👥 TOPLUMCU", "toplumsal dış görü", GreenNeon, toplumcuText)
                    TeamAgentItemRow("♟ STRATEJİST", "stratejik dış görü", TealNeon, stratejistText)
                }
            }

            // HISTORICAL PROMPTS INDEX LIST
            if (teamAnalyses.isNotEmpty()) {
                item {
                    Text(text = "GENEL TETİKLEYİCİ GEÇMİŞİ", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }

                items(teamAnalyses) { history ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceColor, RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GoldNeon))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = history.triggerPrompt, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        val timeF = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(history.timestamp))
                        Text(text = timeF, color = TextDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun TeamAgentItemRow(name: String, role: String, color: Color, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = name, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Default)
            Text(text = role, color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = text, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

// MODULE 7: SYSTEM TOPOLOGY (MİMARİ HARİTA SHIELDS GRAPHICS)
@Composable
fun TopologyOverlayScreen(viewModel: TheiaViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBg)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.setOverlay(TheiaOverlay.NONE) }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, tint = TextMuted, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "THEIA · MİMARİ MİRAS",
                        color = PurpleNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "topology live diagram mapping",
                        color = TextMuted,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GRAPHICS LAYER INTERACTIVE CANVAS MAP
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
            border = BorderStroke(1.dp, PurpleNeon.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw central connection grid lines
                    val centerW = size.width / 2
                    val centerH = size.height / 2

                    // Node coordinate points represent Kaptan, Telegram Bot, Soul SQLite DB, Ollama Local Node, Claude API
                    val nodes = listOf(
                        Pair(centerW, size.height * 0.15f), // Kaptan
                        Pair(centerW, size.height * 0.4f),  // Telegram Bot Handler
                        Pair(size.width * 0.2f, size.height * 0.65f), // Local SQLite Room (Theia DB)
                        Pair(size.width * 0.8f, size.height * 0.65f), // Local AI (Gemini REST service)
                        Pair(centerW, size.height * 0.85f)  // External Obsidian System Docs
                    )

                    // Draw connection path lines with dotted style
                    nodes.forEach { (x1, y1) ->
                        nodes.forEach { (x2, y2) ->
                            if (x1 != x2 || y1 != y2) {
                                drawLine(
                                    color = PurpleNeon.copy(alpha = 0.08f),
                                    start = Offset(x1, y1),
                                    end = Offset(x2, y2),
                                    strokeWidth = 2f,
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                                )
                            }
                        }
                    }

                    // Render physical core nodes boundaries
                    drawCircle(color = OrangeNeon, radius = 28f, center = Offset(nodes[0].first, nodes[0].second))
                    drawCircle(color = BlueNeon, radius = 28f, center = Offset(nodes[1].first, nodes[1].second))
                    drawCircle(color = GreenNeon, radius = 28f, center = Offset(nodes[2].first, nodes[2].second))
                    drawCircle(color = PurpleNeon, radius = 28f, center = Offset(nodes[3].first, nodes[3].second))
                    drawCircle(color = GoldNeon, radius = 28f, center = Offset(nodes[4].first, nodes[4].second))
                }

                // Node overlay text tags positioning
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "👤 KAPTAN",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    )
                    Text(
                        text = "🤖 PTB UPDATE HANDLER",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 54.dp)
                    )
                    Text(
                        text = "🗄 THEIA DB (SQLITE)",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 60.dp, start = 14.dp)
                    )
                    Text(
                        text = "🧠 GEMINI API SERVER",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 60.dp, end = 14.dp)
                    )
                    Text(
                        text = "📂 BRIDGE SYSTEM FILES",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )
                }
            }
        }
    }
}
