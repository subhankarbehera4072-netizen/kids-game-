package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.asImageBitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import com.example.data.PlayerProfile
import com.example.data.ScoreRecord
import com.example.ui.AppLanguage
import com.example.ui.AppScreen
import com.example.ui.GameMode
import com.example.ui.GameState
import com.example.ui.MathViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private val viewModel: MathViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

// Particle details for Confetti effect
data class ConfettiParticle(
    val x: Float,
    var y: Float,
    val size: Float,
    val color: Color,
    val speed: Float,
    val angle: Float
)

@Composable
fun ConfettiShower(trigger: Boolean) {
    if (!trigger) return

    val particles = remember {
        List(80) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = -20f,
                size = Random.nextFloat() * 14f + 12f,
                color = listOf(
                    Color(0xFFFFF176), // Yellow
                    Color(0xFFFF8A65), // Coral Orange
                    Color(0xFFF06292), // Hot Pink
                    Color(0xFF81C784), // Mint Green
                    Color(0xFF64B5F6), // Sky Blue
                    Color(0xFFBA68C8)  // Lavender
                ).random(),
                speed = Random.nextFloat() * 10f + 8f,
                angle = Random.nextFloat() * 2f - 1f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            val currentY = p.y + (animProgress * height * (p.speed / 8f))
            val currentX = (p.x * width) + (animProgress * 180f * p.angle)
            if (currentY < height) {
                drawCircle(
                    color = p.color,
                    radius = p.size,
                    center = Offset(currentX, currentY)
                )
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MathViewModel) {
    val activeScreen by viewModel.activeScreen.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val unlockedBadgeName by viewModel.unlockedBadgeName.collectAsStateWithLifecycle()

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFF9E6), // Pastel Yellow
            Color(0xFFFFE0B2)  // Soft Orange
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        when (activeScreen) {
            AppScreen.HOME, AppScreen.GAME_MODE -> HomeScreen(
                viewModel = viewModel,
                profile = profile,
                appLanguage = appLanguage
            )
            AppScreen.ACTIVE_GAME -> ActiveGameScreen(
                viewModel = viewModel,
                gameState = gameState,
                appLanguage = appLanguage
            )
            AppScreen.GAME_OVER -> GameOverScreen(
                viewModel = viewModel,
                gameState = gameState,
                appLanguage = appLanguage
            )
            AppScreen.HISTORY -> HistoryScreen(
                viewModel = viewModel,
                profile = profile,
                history = history,
                appLanguage = appLanguage
            )
            AppScreen.BADGES -> BadgesScreen(
                viewModel = viewModel,
                profile = profile,
                appLanguage = appLanguage
            )
        }

        // Newly unlocked badge dialog celebration!
        if (unlockedBadgeName != null) {
            BadgeUnlockedDialog(
                badgeName = unlockedBadgeName!!,
                appLanguage = appLanguage,
                onDismiss = { viewModel.dismissBadgeDialog() }
            )
        }
    }
}

fun selectText(appLanguage: AppLanguage, english: String, hinglish: String, odia: String): String {
    return when (appLanguage) {
        AppLanguage.ENGLISH -> english
        AppLanguage.HINGLISH -> hinglish
        AppLanguage.ODIA -> odia
    }
}

fun getChaptersForClassAndBoard(classLvl: Int, board: String, appLanguage: AppLanguage): List<String> {
    val isOdiaBoard = board == "Odisha Board"
    return when (classLvl) {
        1 -> if (isOdiaBoard) {
            listOf(
                "Ch 1: Shapes and Space (ଆକାର ଓ ସ୍ଥାନ)",
                "Ch 2: Numbers 1 to 9 (ଏକରୁ ନଅ)",
                "Ch 3: Addition (ମିଶାଣ)",
                "Ch 4: Subtraction (ଫେଡ଼ାଣ)"
            )
        } else {
            listOf(
                "Ch 1: Shapes & Space",
                "Ch 2: Numbers One to Nine",
                "Ch 3: Addition",
                "Ch 4: Subtraction"
            )
        }
        2 -> if (isOdiaBoard) {
            listOf(
                "Ch 1: Counting in Groups (ଦଳରେ ଗଣିବା)",
                "Ch 2: Shapes & Patterns (ଆକୃତି ଓ ଢାଞ୍ଚା)",
                "Ch 3: Tens and Ones (ଦଶ ଓ ଏକ)"
            )
        } else {
            listOf(
                "Ch 1: What is Long, What is Round?",
                "Ch 2: Counting in Groups",
                "Ch 3: How Much Can You Carry?"
            )
        }
        3 -> if (isOdiaBoard) {
            listOf(
                "Ch 1: Fun with Numbers (ସଂଖ୍ୟା ସହ ମଜା)",
                "Ch 2: Give and Take (ଦେବା ନେବା)",
                "Ch 3: Long and Short (ଲମ୍ବା ଓ ଛୋଟ)"
            )
        } else {
            listOf(
                "Ch 1: Where to Look From",
                "Ch 2: Fun with Numbers",
                "Ch 3: Give and Take"
            )
        }
        4 -> if (isOdiaBoard) {
            listOf(
                "Ch 1: Building with Bricks (ଇଟା ଦ୍ୱାରା ସଜାଣି)",
                "Ch 2: Long and Short (ଲମ୍ବା ଓ ଛୋଟ)",
                "Ch 3: Time: Tick-Tick-Tick (ଟିକ୍-ଟିକ୍ - ସମୟ)"
            )
        } else {
            listOf(
                "Ch 1: Building with Bricks",
                "Ch 2: Long and Short",
                "Ch 3: A Trip to Bhopal"
            )
        }
        5 -> if (isOdiaBoard) {
            listOf(
                "Ch 1: Shapes and Angles (ଆକୃତି ଓ କୋଣ)",
                "Ch 2: Parts and Wholes (ଭଗ୍ନାଂଶ)",
                "Ch 3: Mapping Your Way (ନକ୍ସା ପ୍ରସ୍ତୁତି)"
            )
        } else {
            listOf(
                "Ch 1: The Fish Tale",
                "Ch 2: Shapes and Angles",
                "Ch 3: How Many Squares?"
            )
        }
        6 -> if (isOdiaBoard) {
            listOf(
                "Ch 1: Knowing Our Numbers (ଆମ ସଂଖ୍ୟା ପରିଚୟ)",
                "Ch 2: Geometrical Shapes (ମୌଳିକ ଜ୍ୟାମିତିକ ଆକୃତି)",
                "Ch 3: Integers (ପୂର୍ଣ୍ଣସଂଖ୍ୟା)"
            )
        } else {
            listOf(
                "Ch 1: Knowing Our Numbers",
                "Ch 2: Whole Numbers",
                "Ch 3: Playing with Numbers"
            )
        }
        7 -> if (isOdiaBoard) {
            listOf(
                "Ch 1: Integers (ପୂର୍ଣ୍ଣ ସଂଖ୍ୟା)",
                "Ch 2: Fractions & Decimals (ଭଗ୍ନାଂଶ ଓ ଦଶମିକ)",
                "Ch 3: Simple Equations (ସରଳ ସମୀକରଣ)"
            )
        } else {
            listOf(
                "Ch 1: Integers",
                "Ch 2: Fractions and Decimals",
                "Ch 3: Data Handling"
            )
        }
        8 -> if (isOdiaBoard) {
            listOf(
                "Ch 1: Rational Numbers (ପରିମେୟ ସଂଖ୍ୟା)",
                "Ch 2: Linear Equations in One Variable (ଏକ ଅଜ୍ଞାତ ରାଶି)",
                "Ch 3: Practical Geometry (ବ୍ୟାବହାରିକ ଜ୍ୟାମିତି)"
            )
        } else {
            listOf(
                "Ch 1: Rational Numbers",
                "Ch 2: Linear Equations in One Variable",
                "Ch 3: Understanding Quadrilaterals"
            )
        }
        else -> emptyList()
    }
}

// --- SUB-SCREENS ---

@Composable
fun HomeScreen(
    viewModel: MathViewModel,
    profile: PlayerProfile,
    appLanguage: AppLanguage
) {
    var showNameDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var devPhotoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(Unit) {
        try {
            val file = java.io.File(context.filesDir, "developer_photo.jpg")
            if (file.exists()) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    devPhotoBitmap = bitmap
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val file = java.io.File(context.filesDir, "developer_photo.jpg")
                    java.io.FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        devPhotoBitmap = bitmap
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            BottomNavigationBar(
                currentScreen = AppScreen.HOME,
                onNavigate = { viewModel.navigateTo(it) },
                appLanguage = appLanguage
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Player Badge & Quick Stats Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .clickable { showNameDialog = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Little Avatar placeholder
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color(0xFFFF8A65), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👦",
                                fontSize = 28.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFFE65100)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Name",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = selectText(appLanguage, "Level ${profile.level} Math Hero", "Level ${profile.level} Math Hero", "ଲେଭଲ୍ ${profile.level} ଗଣିତ ହିରୋ 🏆"),
                                fontSize = 13.sp,
                                color = Color.DarkGray
                            )
                        }

                        // Class Indicator Badge
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEDE7F6), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = selectText(appLanguage, "Class ${profile.classLevel}", "Class ${profile.classLevel}", "ଶ୍ରେଣୀ ${profile.classLevel}"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF6A1B9A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Stats grid (Coins, XP, Stars, Streak)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Coins
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🪙", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(text = profile.coins.toString(), fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFFFF9100))
                                Text(text = selectText(appLanguage, "Coins", "Sikka", "କଏନ୍‌"), fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        // XP
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "✨", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(text = profile.xp.toString(), fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF00B0FF))
                                Text(text = selectText(appLanguage, "XP", "XP", "ଅନୁଭବ (XP)"), fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        // Stars
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⭐", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(text = profile.stars.toString(), fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFFFFB300))
                                Text(text = selectText(appLanguage, "Stars", "Tare", "ଷ୍ଟାର୍"), fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        // Streak
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔥", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(text = "${profile.streak} d", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFFFF3D00))
                                Text(text = selectText(appLanguage, "Streak", "Streak", "ଧାରାବାହିକ"), fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shubhankar Mascot & Speech Balloon Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mascot Image (Generative Asset loaded safely)
                    Image(
                        painter = painterResource(id = R.drawable.img_shubhankar_mascot_1782991870243),
                        contentDescription = "Shubhankar the Mascot",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    // Dialog speech bubble
                    Column(
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = selectText(appLanguage, "Shubhankar says:", "Shubhankar bolte hain:", "ଶୁଭଙ୍କର କହୁଛନ୍ତି:"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectText(appLanguage, "Let's learn math and earn stars today! Tap on a game below to play! 🌟", "Chalo dost! Sath me math khelte hain aur dher saare sitare jeet-te hain! 🎒", "ଚାଲ ସାଙ୍ଗ! ଆଜି ଏକାଠି ଗଣିତ ଶିଖିବା ଏବଂ ଷ୍ଟାର୍ ଜିତିବା! ଖେଳିବା ପାଇଁ ତଳେ ଥିବା ଗେମକୁ ଦବାନ୍ତୁ! 🌟"),
                            fontSize = 13.sp,
                            color = Color.Black,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bilingual Quick Toggle Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFCC80).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { viewModel.toggleLanguage() }
                    .padding(10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Language Selector",
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (appLanguage) {
                        AppLanguage.ENGLISH -> "🌐 Language: English (Tap to change to Hinglish)"
                        AppLanguage.HINGLISH -> "🌐 Language: Hinglish (Hindi/Eng) (Tap to change to Odia)"
                        AppLanguage.ODIA -> "🌐 Language: Odia / ଓଡ଼ିଆ (Tap to change to English)"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD84315)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Share App Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = selectText(
                            appLanguage,
                            "Share App with Friends! 🎒",
                            "Dosto ke Sath App Share Karein! 🎒",
                            "ସାଙ୍ଗମାନଙ୍କ ସହ ଆପ୍ ଶେୟାର କରନ୍ତୁ! 🎒"
                        ),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            try {
                                val shareMessage = selectText(
                                    appLanguage,
                                    "Hey! Try out Shubhankar Math Game! It's a wonderful math learning game designed by a Class 8 Student & Microsoft Software Developer! 🏆 Download & view it here: https://ais-pre-7d4ptlfuv5jrbhkltgjx34-337132386390.asia-east1.run.app",
                                    "Hey! Shubhankar Math Game try karo! Ye class 8 ke student aur Microsoft Software Developer ne banaya hai! Bohot mazedaar game hai! 🏆 Download aur view karne ke liye click karein: https://ais-pre-7d4ptlfuv5jrbhkltgjx34-337132386390.asia-east1.run.app",
                                    "ନମସ୍କାର! ଶୁଭଙ୍କର ମ୍ୟାଥ୍ ଗେମ୍‌କୁ ଥରେ ଖେଳି ଦେଖନ୍ତୁ! ଏହା ଅଷ୍ଟମ ଶ୍ରେଣୀର ମେଧାବୀ ଛାତ୍ର ଏବଂ ମାଇକ୍ରୋସଫ୍ଟ ସଫ୍ଟୱେୟାର୍ ଡେଭେଲପର୍‌ଙ୍କ ଦ୍ୱାରା ପ୍ରସ୍ତୁତ ଏକ ଗଣିତ ଶିଖିବାର ଅତି ମଜାଦାର ଖେଳ! 🏆 ଏହାକୁ ଏଠାରୁ ଡାଉନଲୋଡ୍ କରନ୍ତୁ: https://ais-pre-7d4ptlfuv5jrbhkltgjx34-337132386390.asia-east1.run.app"
                                )
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Shubhankar Math Game")
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareMessage)
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share App via"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366) // WhatsApp official brand green color
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_whatsapp_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "💬", fontSize = 16.sp) // Custom green WhatsApp bubble icon replacement
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectText(
                                    appLanguage,
                                    "Share via WhatsApp / Other Apps",
                                    "WhatsApp ke Jariye Share Karein",
                                    "WhatsApp ମାଧ୍ୟମରେ ଶେୟାର କରନ୍ତୁ"
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                val urlIntent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://ais-pre-7d4ptlfuv5jrbhkltgjx34-337132386390.asia-east1.run.app")
                                )
                                context.startActivity(urlIntent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF9100)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9100)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_download_link_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Download APK",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectText(
                                    appLanguage,
                                    "Download APK / Open Web Link 📥",
                                    "Download APK / Web Link Kholein 📥",
                                    "APK ଡାଉନଲୋଡ୍ କରନ୍ତୁ / ଲିଙ୍କ୍ ଖୋଲନ୍ତୁ 📥"
                                ),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Copy Link Button for maximum compatibility
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    OutlinedButton(
                        onClick = {
                            try {
                                clipboardManager.setText(
                                    androidx.compose.ui.text.AnnotatedString("https://ais-pre-7d4ptlfuv5jrbhkltgjx34-337132386390.asia-east1.run.app")
                                )
                                android.widget.Toast.makeText(
                                    context,
                                    selectText(
                                        appLanguage,
                                        "Link copied to clipboard! 👍",
                                        "Link copy ho gaya hai! 👍",
                                        "ଲିଙ୍କ୍ କପି ହୋଇଗଲା! 👍"
                                    ),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2E7D32)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("copy_link_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Link",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectText(
                                    appLanguage,
                                    "Copy Download Link 📋",
                                    "Download Link Copy Karein 📋",
                                    "ଡାଉନଲୋଡ୍ ଲିଙ୍କ୍ କପି କରନ୍ତୁ 📋"
                                ),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Display the raw URL nicely so they can read or copy it manually if they want
                    SelectionContainer {
                        Text(
                            text = "https://ais-pre-7d4ptlfuv5jrbhkltgjx34-337132386390.asia-east1.run.app",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Class & Syllabus Selection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = selectText(appLanguage, "Select Class & Syllabus 🏫", "Kaksha aur Board Chuno 🏫", "ଶ୍ରେଣୀ ଓ ବୋର୍ଡ଼ ଚୟନ 🏫"),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFFE65100)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Board toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isOdia = profile.board == "Odisha Board"
                        Button(
                            onClick = { viewModel.updatePlayerBoard("Odisha Board") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOdia) Color(0xFF2E7D32) else Color(0xFFE0E0E0),
                                contentColor = if (isOdia) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Odisha Board", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.updatePlayerBoard("NCERT/CBSE") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isOdia) Color(0xFF1565C0) else Color(0xFFE0E0E0),
                                contentColor = if (!isOdia) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "NCERT / CBSE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Class selector pills
                    Text(
                        text = selectText(appLanguage, "Select Class/Grade:", "Class chuney:", "ଶ୍ରେଣୀ ବାଛନ୍ତୁ:"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp)
                    ) {
                        (1..8).forEach { classNum ->
                            val isSelected = profile.classLevel == classNum
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) Color(0xFFFFB300) else Color(0xFFF5F5F5),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFFFF8F00) else Color(0xFFE0E0E0),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.updatePlayerClass(classNum) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = selectText(appLanguage, "Class $classNum", "Class $classNum", "ଶ୍ରେଣୀ $classNum"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else Color.DarkGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic Curriculum Chapter List preview
                    Text(
                        text = selectText(appLanguage, "Syllabus Chapters 📚", "Syllabus Path 📚", "ପାଠ୍ୟକ୍ରମ ଅଧ୍ୟାୟଗୁଡ଼ିକ 📚"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFE65100)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val chapters = getChaptersForClassAndBoard(profile.classLevel, profile.board, appLanguage)
                    chapters.forEach { chapter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🔸", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = chapter,
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Menu Section Title
            Text(
                text = selectText(appLanguage, "Choose Your Math Game! 🎮", "Apna Math Game Chuno! 🎮", "ନିଜର ପ୍ରିୟ ଗଣିତ ଗେମ୍ ବାଛନ୍ତୁ! 🎮"),
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = Color(0xFFE65100),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Grid of Game Modes
            val gameModes = listOf(
                GameModeItem(
                    mode = GameMode.COUNT,
                    titleEN = "Count Objects 🎒",
                    titleHI = "Ginti Seekho 🎒",
                    titleOR = "ଗଣିତ ଗଣତି 🎒",
                    descEN = "Count cute emojis and match!",
                    descHI = "Pyari cheezo ko gino aur batao!",
                    descOR = "ସୁନ୍ଦର ଜିନିଷଗୁଡ଼ିକୁ ଗଣନ୍ତୁ ଏବଂ ମେଳ କରନ୍ତୁ!",
                    color = Color(0xFF81C784),
                    accentColor = Color(0xFF2E7D32)
                ),
                GameModeItem(
                    mode = GameMode.ADD,
                    titleEN = "Add Numbers ➕",
                    titleHI = "Jod Seekho ➕",
                    titleOR = "ମିଶାଣ ଶିଖନ୍ତୁ ➕",
                    descEN = "Sum up objects with fun help!",
                    descHI = "Dost! Number jodna seekho!",
                    descOR = "ମଜାଦାର ଉପାୟରେ ସଂଖ୍ୟା ମିଶାଣ ଶିଖନ୍ତୁ!",
                    color = Color(0xFFFFB74D),
                    accentColor = Color(0xFFE65100)
                ),
                GameModeItem(
                    mode = GameMode.SUB,
                    titleEN = "Subtract Numbers ➖",
                    titleHI = "Ghatav Seekho ➖",
                    titleOR = "ଫେଡ଼ାଣ ଶିଖନ୍ତୁ ➖",
                    descEN = "Take away numbers visually!",
                    descHI = "Number ghatana seekhein!",
                    descOR = "ଚିତ୍ର ସାହାଯ୍ୟରେ ସଂଖ୍ୟା ଫେଡ଼ାଣ ଶିଖନ୍ତୁ!",
                    color = Color(0xFF64B5F6),
                    accentColor = Color(0xFF1565C0)
                ),
                GameModeItem(
                    mode = GameMode.MUL,
                    titleEN = "Multiply ✖️",
                    titleHI = "Guna Seekho ✖️",
                    titleOR = "ଗୁଣନ ଶିଖନ୍ତୁ ✖️",
                    descEN = "Fun tables and multiplication!",
                    descHI = "Table seekhein mazaak me!",
                    descOR = "ମଜାଦାର ପଣକିଆ ଏବଂ ଗୁଣନ ଶିଖନ୍ତୁ!",
                    color = Color(0xFFBA68C8),
                    accentColor = Color(0xFF6A1B9A)
                ),
                GameModeItem(
                    mode = GameMode.DIV,
                    titleEN = "Divide Candies ➗",
                    titleHI = "Bhag Seekho ➗",
                    titleOR = "ହରଣ ଶିଖନ୍ତୁ ➗",
                    descEN = "Share items equally with friends!",
                    descHI = "Candy barabar baantna seekho!",
                    descOR = "କ୍ୟାଣ୍ଡିକୁ ସାଙ୍ଗମାନଙ୍କ ମଧ୍ୟରେ ସମାନ ଭାବରେ ବାଣ୍ଟନ୍ତୁ!",
                    color = Color(0xFF4DB6AC),
                    accentColor = Color(0xFF00695C)
                ),
                GameModeItem(
                    mode = GameMode.QUIZ,
                    titleEN = "Speed Challenge ⚡",
                    titleHI = "Speed Quiz ⚡",
                    titleOR = "ଗଣିତ ସ୍ପିଡ୍ କୁଇଜ୍ ⚡",
                    descEN = "Mixed questions, run against timer!",
                    descHI = "Timer chalega! Jaldi jawab do!",
                    descOR = "ସମୟ ସରିବା ପୂର୍ବରୁ ସଠିକ୍ ଉତ୍ତର ଦିଅନ୍ତୁ!",
                    color = Color(0xFFE57373),
                    accentColor = Color(0xFFC62828)
                )
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                gameModes.forEach { item ->
                    GameModeCard(
                        item = item,
                        appLanguage = appLanguage,
                        onClick = { viewModel.startGame(item.mode) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Developer Bio Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "👨‍💻",
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = selectText(appLanguage, "About the Developer ✨", "Developer ke Bare Me ✨", "ଡେଭେଲପର୍‌ଙ୍କ ବିଷୟରେ ✨"),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color(0xFF6A1B9A)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFFFF9100), CircleShape)
                            .clickable {
                                try {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (devPhotoBitmap != null) {
                            Image(
                                bitmap = devPhotoBitmap!!.asImageBitmap(),
                                contentDescription = "Shubhankar Behera Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_shubhankar_photo_1783008513779),
                                contentDescription = "Shubhankar Behera Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Small edit overlay icon to prompt the user they can touch it to set their real photo
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .background(Color(0xFFFF9100), CircleShape)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Change Developer Photo",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(16.dp)
                                    .testTag("change_dev_photo_button")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Shubhankar Behera",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color(0xFFE65100),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badge highlighting achievements
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFEDE7F6), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Verified",
                            tint = Color(0xFF6A1B9A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectText(appLanguage, "Microsoft Software Developer", "Microsoft Software Developer", "ମାଇକ୍ରୋସଫ୍ଟ ସଫ୍ଟୱେୟାର୍ ଡେଭେଲପର୍ 💻"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF6A1B9A)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏫",
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectText(appLanguage, "Class 8 Student Extraordinaire", "Class 8 ke Brilliant Student", "ଅଷ୍ଟମ ଶ୍ରେଣୀର ମେଧାବୀ ଛାତ୍ର 🏫"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = selectText(
                            appLanguage = appLanguage,
                            english = "Shubhankar Behera is an incredibly talented student studying in Class 8, who is also an amazing Software Developer at Microsoft! He designed and built this game to help you learn mathematics in the most fun and exciting way. Shubhankar believes that every child can become a math champion! 🚀",
                            hinglish = "Shubhankar Behera class 8 me padhte hain aur wo Microsoft me ek Software Developer hain! Unhone aap jaise pyaare bacho ke liye ye Shubhankar Math Game banaya hai taaki aap log mazaak mazaak me math seekh sakein aur champion ban sakein! 🚀",
                            odia = "ଶୁଭଙ୍କର ବେହେରା ହେଉଛନ୍ତି ଅଷ୍ଟମ ଶ୍ରେଣୀରେ ପଢ଼ୁଥିବା ଜଣେ ଅତି ପ୍ରତିଭାଶାଳୀ ଛାତ୍ର, ଯିଏ କି ମାଇକ୍ରୋସଫ୍ଟ କମ୍ପାନୀରେ ଜଣେ ସଫ୍ଟୱେୟାର୍ ଡେଭେଲପର୍ ମଧ୍ୟ ଅଟନ୍ତି! ସେ ଆପଣଙ୍କୁ ଗଣିତ ଶିଖିବାରେ ସାହାଯ୍ୟ କରିବା ପାଇଁ ଏହି ସୁନ୍ଦର ଗେମ୍ ପ୍ରସ୍ତୁତ କରିଛନ୍ତି। ଶୁଭଙ୍କର ବିଶ୍ୱାସ କରନ୍ତି ଯେ ପ୍ରତ୍ୟେକ ଶିଶୁ ଗଣିତରେ ଚାମ୍ପିଅନ୍ ହୋଇପାରିବେ! 🚀"
                        ),
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                    )
                }
            }
        }
    }

    if (showNameDialog) {
        NameEditDialog(
            currentName = profile.name,
            appLanguage = appLanguage,
            onDismiss = { showNameDialog = false },
            onSave = {
                viewModel.updatePlayerName(it)
                showNameDialog = false
            }
        )
    }
}

data class GameModeItem(
    val mode: GameMode,
    val titleEN: String,
    val titleHI: String,
    val titleOR: String,
    val descEN: String,
    val descHI: String,
    val descOR: String,
    val color: Color,
    val accentColor: Color
)

@Composable
fun GameModeCard(
    item: GameModeItem,
    appLanguage: AppLanguage,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("game_mode_${item.mode.name}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = item.color.copy(alpha = 0.95f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectText(appLanguage, item.titleEN, item.titleHI, item.titleOR),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selectText(appLanguage, item.descEN, item.descHI, item.descOR),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier
                    .size(40.dp)
                    .background(item.accentColor, CircleShape)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun ActiveGameScreen(
    viewModel: MathViewModel,
    gameState: GameState,
    appLanguage: AppLanguage
) {
    val options = gameState.options
    val showConfetti = gameState.isCorrect == true

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                Spacer(modifier = Modifier.statusBarsPadding())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        modifier = Modifier.background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go Home",
                            tint = Color.Black
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${gameState.gameMode.name} Q: ${gameState.questionNumber}/${gameState.maxQuestions}",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Stars",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = gameState.score.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Character area (Small Shubhankar with Speech Bubble)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_shubhankar_mascot_1782991870243),
                        contentDescription = "Shubhankar the Mascot",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = gameState.shubhankarSpeech,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD84315)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Adaptive difficulty level display badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val difficultyLabel = when (gameState.difficultyLevel) {
                        1 -> selectText(appLanguage, "Very Easy 🌱", "Boht Aasan 🌱", "ଅତି ସହଜ 🌱")
                        2 -> selectText(appLanguage, "Easy 🍀", "Aasan 🍀", "ସହଜ 🍀")
                        3 -> selectText(appLanguage, "Medium 🌟", "Theek-Thak 🌟", "ମଧ୍ୟମ 🌟")
                        4 -> selectText(appLanguage, "Hard 🔥", "Mushkil 🔥", "କଷ୍ଟକର 🔥")
                        else -> selectText(appLanguage, "Genius ⚡", "Shubhankar Genius ⚡", "ବୁଦ୍ଧିମାନ ⚡")
                    }
                    val difficultyColor = when (gameState.difficultyLevel) {
                        1 -> Color(0xFF43A047)
                        2 -> Color(0xFF2E7D32)
                        3 -> Color(0xFFF57C00)
                        4 -> Color(0xFFD84315)
                        else -> Color(0xFF8E24AA)
                    }

                    Text(
                        text = selectText(appLanguage, "Adaptive Math Level:", "Auto Math Level:", "ସ୍ୱୟଂଚାଳିତ ଗଣିତ ସ୍ତର:"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    Row(
                        modifier = Modifier
                            .background(difficultyColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .border(1.dp, difficultyColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = difficultyLabel,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = difficultyColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Quiz timer & Heart Lives
                if (gameState.gameMode == GameMode.QUIZ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Timer
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Timer",
                                tint = if (gameState.timerSeconds < 5) Color.Red else Color.DarkGray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${gameState.timerSeconds}s",
                                fontWeight = FontWeight.Bold,
                                color = if (gameState.timerSeconds < 5) Color.Red else Color.Black
                            )
                        }

                        // Hearts
                        Row {
                            repeat(3) { index ->
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Heart",
                                    tint = if (index < gameState.lives) Color.Red else Color.LightGray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Challenge & Visualizer Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Math Equation Heading
                        val equationText = if (gameState.questionTextOverrideEN.isNotEmpty()) {
                            selectText(appLanguage, gameState.questionTextOverrideEN, gameState.questionTextOverrideHI, gameState.questionTextOverrideOR)
                        } else {
                            when (gameState.operator) {
                                "COUNT" -> selectText(appLanguage, "Count the ${gameState.emoji}s!", "${gameState.emoji}s ko gino dost!", "${gameState.emoji}ଗୁଡ଼ିକୁ ଗଣନ୍ତୁ!")
                                else -> "${gameState.numA} ${gameState.operator} ${gameState.numB} = ?"
                            }
                        }

                        Text(
                            text = equationText,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            color = Color(0xFFE65100),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual help based on Game Mode
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                                .background(Color(0xFFFAFAFA), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (gameState.operator) {
                                "COUNT" -> {
                                    // Flow Row layout of A emojis
                                    FlowEmojis(count = gameState.numA, emoji = gameState.emoji)
                                }
                                "+" -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        FlowEmojis(count = gameState.numA, emoji = gameState.emoji)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = "➕", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        FlowEmojis(count = gameState.numB, emoji = gameState.emoji)
                                    }
                                }
                                "-" -> {
                                    // Show total numA, crossed out or faded numB
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = selectText(appLanguage, "Take away ${gameState.numB} from ${gameState.numA}", "${gameState.numA} me se ${gameState.numB} hatao", "${gameState.numA} ରୁ ${gameState.numB} ଫେଡ଼ନ୍ତୁ"),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FlowEmojisWithCrossing(
                                            total = gameState.numA,
                                            crossCount = gameState.numB,
                                            emoji = gameState.emoji
                                        )
                                    }
                                }
                                "×" -> {
                                    // Grid of numA x numB
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = selectText(appLanguage, "${gameState.numA} rows of ${gameState.numB} items", "${gameState.numA} liyen, har line me ${gameState.numB} item", "${gameState.numB} ଟି ଲେଖାଏଁ ${gameState.numA} ଧାଡ଼ି"),
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        GridEmojis(rows = gameState.numA, cols = gameState.numB, emoji = gameState.emoji)
                                    }
                                }
                                "÷" -> {
                                    // Divide items into friends
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = selectText(appLanguage, "Share ${gameState.numA} candies among ${gameState.numB} friends!", "${gameState.numA} meethi candy ko ${gameState.numB} doston me barabar baanto!", "${gameState.numA} ଟି କ୍ୟାଣ୍ଡିକୁ ${gameState.numB} ଜଣ ସାଙ୍ଗଙ୍କ ମଧ୍ୟରେ ସମାନ ଭାବରେ ବାଣ୍ଟନ୍ତୁ!"),
                                            fontSize = 12.sp,
                                            color = Color.DarkGray,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            repeat(gameState.numB) { index ->
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .background(Color(0xFFE0F2F1), RoundedCornerShape(8.dp))
                                                        .padding(6.dp)
                                                ) {
                                                    Text(text = selectText(appLanguage, "Friend ${index + 1}", "Dost ${index + 1}", "ସାଙ୍ଗ ${index + 1}"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(text = gameState.emoji, fontSize = 18.sp)
                                                    Text(text = selectText(appLanguage, "? Candy", "? Candy", "? କ୍ୟାଣ୍ଡି"), fontSize = 10.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Options Section (4 buttons grid)
                Text(
                    text = selectText(appLanguage, "Tap Your Answer: 👇", "Apna Answer Tap Karo: 👇", "ଉତ୍ତର ବାଛନ୍ତୁ: 👇"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false
                ) {
                    items(options) { number ->
                        val isSelected = gameState.selectedAnswer == number
                        val isThisCorrectAnswer = number == gameState.correctAnswer
                        val hasAnswered = gameState.selectedAnswer != null

                        val buttonBgColor by animateColorAsState(
                            targetValue = when {
                                !hasAnswered -> Color.White
                                isSelected && isThisCorrectAnswer -> Color(0xFFC8E6C9) // Green
                                isSelected && !isThisCorrectAnswer -> Color(0xFFFFCDD2) // Red
                                isThisCorrectAnswer -> Color(0xFFC8E6C9) // Auto-reveal correct
                                else -> Color.White.copy(alpha = 0.5f)
                            },
                            label = "buttonBg"
                        )

                        val borderStroke = when {
                            !hasAnswered -> BorderStroke(2.dp, Color(0xFFFFB74D))
                            isSelected && isThisCorrectAnswer -> BorderStroke(4.dp, Color(0xFF4CAF50))
                            isSelected && !isThisCorrectAnswer -> BorderStroke(4.dp, Color(0xFFF44336))
                            isThisCorrectAnswer -> BorderStroke(3.dp, Color(0xFF4CAF50))
                            else -> BorderStroke(1.dp, Color.LightGray)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                                .shadow(2.dp, RoundedCornerShape(18.dp))
                                .clickable(enabled = !hasAnswered) { viewModel.submitAnswer(number) }
                                .testTag("option_$number"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = buttonBgColor),
                            border = borderStroke
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = number.toString(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 28.sp,
                                        color = if (hasAnswered && isThisCorrectAnswer) Color(0xFF1B5E20) else Color.Black
                                    )
                                    if (hasAnswered && isThisCorrectAnswer) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Correct",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Continuous falling confetti show on success!
            ConfettiShower(trigger = showConfetti)
        }
    }
}

// Custom layout widgets for Emoji Math helper visualizations
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowEmojis(count: Int, emoji: String) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center
    ) {
        repeat(count) {
            Text(
                text = emoji,
                fontSize = 24.sp,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowEmojisWithCrossing(total: Int, crossCount: Int, emoji: String) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center
    ) {
        repeat(total) { index ->
            val isCrossed = index >= (total - crossCount)
            Box(
                modifier = Modifier.padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 24.sp,
                    modifier = Modifier.background(
                        if (isCrossed) Color.Black.copy(alpha = 0.15f) else Color.Transparent,
                        RoundedCornerShape(4.dp)
                    )
                )
                if (isCrossed) {
                    Text(
                        text = "❌",
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GridEmojis(rows: Int, cols: Int, emoji: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        repeat(rows) {
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(cols) {
                    Text(
                        text = emoji,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GameOverScreen(
    viewModel: MathViewModel,
    gameState: GameState,
    appLanguage: AppLanguage
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Celebration Character Card
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(Color.White, CircleShape)
                    .border(4.dp, Color(0xFFFFB74D), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_shubhankar_mascot_1782991870243),
                    contentDescription = "Mascot celebration",
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (gameState.score >= 8) {
                    selectText(appLanguage, "Shabaash! Excellent Work! 🏆", "Shabaash! Bahut Badhiya! 🏆", "ଶାବାସ! ଚମତ୍କାର କାର୍ଯ୍ୟ! 🏆")
                } else {
                    selectText(appLanguage, "Well Played! Keep Learning! 📚", "Bahut Badhiya Koshish! 📚", "ଖୁବ୍ ସୁନ୍ଦର ଚେଷ୍ଟା! ଏମିତି ଶିଖୁଥାଅ! 📚")
                },
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = Color(0xFFE65100),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Scoreboard Summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = selectText(appLanguage, "YOUR SCORECARD", "AAPKA RESULTS", "ଆପଣଙ୍କ ସ୍କୋରକାର୍ଡ 📊"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${gameState.score}/${gameState.questionNumber}",
                                fontWeight = FontWeight.Black,
                                fontSize = 34.sp,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = selectText(appLanguage, "Correct Answers", "Sahi Jawab", "ସଠିକ୍ ଉତ୍ତର"),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "+${gameState.starsEarned}",
                                fontWeight = FontWeight.Black,
                                fontSize = 34.sp,
                                color = Color(0xFFFFB300)
                            )
                            Text(
                                text = selectText(appLanguage, "Stars Earned ⭐", "Sitare Mile ⭐", "ଅର୍ଜିତ ଷ୍ଟାର୍ ⭐"),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = Color(0xFFEEEEEE))

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = gameState.shubhankarSpeech,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1B5E20),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Educational review of formulas solved
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = selectText(appLanguage, "Answer Review: 📝", "Question Review: 📝", "ଉତ୍ତର ସମୀକ୍ଷା: 📝"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    gameState.solvedQuestions.forEach { formula ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isOk = formula.contains("✓")
                            Icon(
                                imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = "Status",
                                tint = if (isOk) Color(0xFF4CAF50) else Color(0xFFF44336),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formula.replace(" (✓)", "").replace(" (✗)", ""),
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Continue Button (High priority, full width)
            Button(
                onClick = { viewModel.continueGame() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("continue_playing_button")
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A1B9A) // Beautiful Purple branding
                )
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Continue", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectText(appLanguage, "Continue Playing 🚀", "Shubhankar Ke Sath Aur Khelein! 🚀", "ଖେଳ ଚାଲୁ ରଖନ୍ତୁ 🚀"),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.startGame(gameState.gameMode) },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("play_again_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D))
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Restart")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectText(appLanguage, "Play Again", "Dobara Khelein", "ପୁଣି ଖେଳନ୍ତୁ 🔄"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Button(
                    onClick = { viewModel.navigateTo(AppScreen.HOME) },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("go_home_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784))
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = "Home")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectText(appLanguage, "Go Home", "Ghar Chalein", "ଘରକୁ ଯାଆନ୍ତୁ 🏠"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    viewModel: MathViewModel,
    profile: PlayerProfile,
    history: List<ScoreRecord>,
    appLanguage: AppLanguage
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Student, 1: Parent, 2: Admin
    var showExportDialog by remember { mutableStateOf(false) }
    var adminMessage by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            BottomNavigationBar(
                currentScreen = AppScreen.HISTORY,
                onNavigate = { viewModel.navigateTo(it) },
                appLanguage = appLanguage
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Screen Title
            Text(
                text = selectText(appLanguage, "Learning Dashboards 📊", "Reports & Portals 📊", "ଗଣିତ ରିପୋର୍ଟ ଓ ପୋର୍ଟାଲ୍ 📊"),
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = Color(0xFFE65100),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Portal Tab Selector Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val tabTitles = listOf(
                    selectText(appLanguage, "Student", "Chhatra", "ଛାତ୍ର 👦"),
                    selectText(appLanguage, "Parent", "Mata-Pita", "ପିତାମାତା 👪"),
                    selectText(appLanguage, "Admin", "Admin", "ପ୍ରଶାସକ ⚙️")
                )
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSelected) Color(0xFFFF9100) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White else Color.DarkGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // STUDENT REPORT DASHBOARD
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Overview Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = selectText(appLanguage, "Dynamic Student Overview 📈", "Chhatra Pragati 📈", "ଛାତ୍ର ପ୍ରଗତି ଓ ସମୀକ୍ଷା 📈"),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Calculate real-time stats from History
                                val totalGames = history.size
                                val totalQuestions = history.sumOf { it.totalQuestions }
                                val totalCorrect = history.sumOf { it.score }
                                val accuracy = if (totalQuestions > 0) (totalCorrect * 100) / totalQuestions else 0

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "$totalGames", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.Black)
                                        Text(text = selectText(appLanguage, "Sessions Played", "Sessions", "ଖେଳାଯାଇଥିବା ଗେମ୍‌"), fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Column {
                                        Text(text = "$totalCorrect", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.Black)
                                        Text(text = selectText(appLanguage, "Correct Solved", "Sahi Hal", "ସଠିକ୍ ସମାଧାନ"), fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Column {
                                        Text(text = "$accuracy%", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF2E7D32))
                                        Text(text = selectText(appLanguage, "Overall Accuracy", "Accuracy", "ମୋଟ ସଠିକତା ହାର"), fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        // Weak / Strong Analysis Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = selectText(appLanguage, "AI Skill Profiling 🧠", "AI Vishleshon 🧠", "AI କୌଶଳ ବିଶ୍ଳେଷଣ 🧠"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF6A1B9A)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                // Determine dynamic weak / strong topics based on history accuracy
                                val addRecords = history.filter { it.gameMode == "ADD" }
                                val addAcc = if (addRecords.isNotEmpty()) (addRecords.sumOf { it.score } * 100) / addRecords.sumOf { it.totalQuestions } else -1

                                val subRecords = history.filter { it.gameMode == "SUB" }
                                val subAcc = if (subRecords.isNotEmpty()) (subRecords.sumOf { it.score } * 100) / subRecords.sumOf { it.totalQuestions } else -1

                                val mulRecords = history.filter { it.gameMode == "MUL" }
                                val mulAcc = if (mulRecords.isNotEmpty()) (mulRecords.sumOf { it.score } * 100) / mulRecords.sumOf { it.totalQuestions } else -1

                                val divRecords = history.filter { it.gameMode == "DIV" }
                                val divAcc = if (divRecords.isNotEmpty()) (divRecords.sumOf { it.score } * 100) / divRecords.sumOf { it.totalQuestions } else -1

                                val strongTopics = mutableListOf<String>()
                                val weakTopics = mutableListOf<String>()

                                if (addAcc >= 80) strongTopics.add(selectText(appLanguage, "Addition (ମିଶାଣ)", "Addition", "Addition")) else if (addAcc in 0..79) weakTopics.add(selectText(appLanguage, "Addition (ମିଶାଣ)", "Addition", "Addition"))
                                if (subAcc >= 80) strongTopics.add(selectText(appLanguage, "Subtraction (ଫେଡ଼ାଣ)", "Subtraction", "Subtraction")) else if (subAcc in 0..79) weakTopics.add(selectText(appLanguage, "Subtraction (ଫେଡ଼ାଣ)", "Subtraction", "Subtraction"))
                                if (mulAcc >= 80) strongTopics.add(selectText(appLanguage, "Multiplication (ଗୁଣନ)", "Multiplication", "Multiplication")) else if (mulAcc in 0..79) weakTopics.add(selectText(appLanguage, "Multiplication (ଗୁଣନ)", "Multiplication", "Multiplication"))
                                if (divAcc >= 80) strongTopics.add(selectText(appLanguage, "Division (ହରଣ)", "Division", "Division")) else if (divAcc in 0..79) weakTopics.add(selectText(appLanguage, "Division (ହରଣ)", "Division", "Division"))

                                // Fallback if list is empty
                                if (strongTopics.isEmpty() && history.isNotEmpty()) {
                                    strongTopics.add(selectText(appLanguage, "Count Objects (ଗଣିତ ଗଣତି)", "Ginti Seekho", "Count Objects"))
                                }

                                Text(
                                    text = selectText(appLanguage, "⭐ Strong Topics (My Strengths):", "Aapke Strong Topics:", "⭐ ମଜବୁତ ବିଷୟ (ସଫଳ କ୍ଷେତ୍ର):"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                if (strongTopics.isNotEmpty()) {
                                    strongTopics.forEach { topic ->
                                        Text(text = " • $topic", fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp))
                                    }
                                } else {
                                    Text(text = selectText(appLanguage, "Play more challenges to discover your strengths! 🚀", "Aur khelein dost!", "ଅଧିକ ଅଭ୍ୟାସ କଲେ ଆପଣଙ୍କ ଶକ୍ତି ଜଣାପଡ଼ିବ! 🚀"), fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 12.dp))
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = selectText(appLanguage, "⚠️ Needs Practice (Focus Areas):", "Koshish ki jaroorat wale topics:", "⚠️ ଅଧିକ ଅଭ୍ୟାସ ଆବଶ୍ୟକ କ୍ଷେତ୍ର:"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFC62828)
                                )
                                if (weakTopics.isNotEmpty()) {
                                    weakTopics.forEach { topic ->
                                        Text(text = " • $topic", fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp))
                                    }
                                } else {
                                    Text(
                                        text = selectText(appLanguage, "Awesome! No weak spots detected yet! 🎉", "Aap toh bahut hoshiyar hain, koi kamzori nahi! 🎉", "ଚମତ୍କାର! କୌଣସି ଦୁର୍ବଳ କ୍ଷେତ୍ର ଚିହ୍ନଟ ହୋଇନାହିଁ! 🎉"),
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                        }

                        // Recent Session Records
                        Text(
                            text = selectText(appLanguage, "Recent Practice Sessions 📜", "Khel Ka Itihaas 📜", "ସାମ୍ପ୍ରତିକ ଅଭ୍ୟାସ ରେକର୍ଡ 📜"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )

                        if (history.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Text(
                                    text = selectText(appLanguage, "No game history yet! Complete games to see details.", "Koi itihaas nahi mila dost! Chalo khelte hain!", "ଏପର୍ଯ୍ୟନ୍ତ କୌଣସି ରେକର୍ଡ ନାହିଁ! ଖେଳିବା ପରେ ଏଠାରେ ସବିଶେଷ ଦେଖିପାରିବେ।"),
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            history.forEach { record ->
                                val dateString = remember(record.timestamp) {
                                    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                    sdf.format(Date(record.timestamp))
                                }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(Color(0xFFFFF3E0), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when (record.gameMode) {
                                                    "COUNT" -> "🎒"
                                                    "ADD" -> "➕"
                                                    "SUB" -> "➖"
                                                    "MUL" -> "✖️"
                                                    "DIV" -> "➗"
                                                    else -> "⚡"
                                                },
                                                fontSize = 18.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = when (record.gameMode) {
                                                    "COUNT" -> selectText(appLanguage, "Count Objects", "Ginti Seekho", "ଗଣିତ ଗଣତି 🎒")
                                                    "ADD" -> selectText(appLanguage, "Addition", "Jod Seekho", "ମିଶାଣ ଶିଖନ୍ତୁ ➕")
                                                    "SUB" -> selectText(appLanguage, "Subtraction", "Ghatav Seekho", "ଫେଡ଼ାଣ ଶିଖନ୍ତୁ ➖")
                                                    "MUL" -> selectText(appLanguage, "Multiplication", "Guna Seekho", "ଗୁଣନ ଶିଖନ୍ତୁ ✖️")
                                                    "DIV" -> selectText(appLanguage, "Division", "Bhag Seekho", "ହରଣ ଶିଖନ୍ତୁ ➗")
                                                    else -> selectText(appLanguage, "Speed Challenge", "Speed Quiz", "ଗଣିତ ସ୍ପିଡ୍ କୁଇଜ୍ ⚡")
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.Black
                                            )
                                            Text(
                                                text = dateString,
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${record.score}/${record.totalQuestions}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (record.score >= (record.totalQuestions * 0.8)) Color(0xFF2E7D32) else Color.Black
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "Stars",
                                                    tint = Color(0xFFFFB300),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "+${record.starsEarned}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFE65100)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // PARENT & TEACHER PORTAL
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "👪", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = selectText(appLanguage, "Parent & Teacher Oversight", "Mata-Pita aur Shikshak Portal", "ପିତାମାତା ଓ ଶିକ୍ଷକ ପୋର୍ଟାଲ୍ 👪"),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = Color(0xFF1565C0)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = selectText(
                                        appLanguage,
                                        "Welcome to the Oversight Dashboard. Here, parents and teachers can monitor homework progress, NCERT syllabus alignment, and download certified academic performance reports.",
                                        "Aapka swagat hai parent/teacher portal me. Yahan aap bache ki progress, NCERT syllabus alignment dekh sakte hain aur dynamic report download kar sakte hain.",
                                        "ଅଭିଭାବକ ଓ ଶିକ୍ଷକ ଡ୍ୟାସବୋର୍ଡକୁ ସ୍ୱାଗତ। ଏଠାରେ ଆପଣ ପିଲାର ପାଠ୍ୟକ୍ରମ ସମାପ୍ତି, ସାପ୍ତାହିକ ଅଭ୍ୟାସ ଏବଂ ଶିକ୍ଷାଗତ ରିପୋର୍ଟ ଡାଉନଲୋଡ୍ କରିପାରିବେ।"
                                    ),
                                    fontSize = 13.sp,
                                    color = Color.DarkGray,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { showExportDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Export")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = selectText(appLanguage, "Export PDF Report Card 📄", "PDF Report Card Download Karein 📄", "ପିଡିଏଫ୍ ରିପୋର୍ଟ ଡାଉନଲୋଡ୍ କରନ୍ତୁ 📄"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Weekly Attendance & Play Logs Map
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = selectText(appLanguage, "Weekly Student Activity Matrix 📅", "Weekly Activity Calendar", "ସାପ୍ତାହିକ ଅଭ୍ୟାସ କ୍ୟାଲେଣ୍ଡର 📅"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                    days.forEachIndexed { idx, day ->
                                        // Highlight active days
                                        val active = idx == 3 || idx == 4 || idx == 5
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .background(
                                                        color = if (active) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (active) "✓" else "",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = day, fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }

                        // Learning Milestones & Recommendations
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = selectText(appLanguage, "Teacher's Personalized Advice 💡", "Shikshak ki Salah 💡", "ଶିକ୍ଷକଙ୍କ ପରାମର୍ଶ ଓ ମାର୍ଗଦର୍ଶନ 💡"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFE65100)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = selectText(
                                        appLanguage,
                                        "Recommendation: The student is performing exceptionally in counting and addition. Advise focusing more on speed training and subtraction tests to build numerical logic.",
                                        "Sujhav: Chhatra counting aur jod me behtareen kar rahe hain. Unhe thoda ghatana (subtraction) aur speed challenges par dhyan dena chahiye.",
                                        "ପରାମର୍ଶ: ପିଲାଟି ଗଣିତି ଓ ମିଶାଣରେ ଉତ୍କୃଷ୍ଟ ପ୍ରଦର୍ଶନ କରୁଛି। ଗାଣିତିକ ଯୁକ୍ତି ବୃଦ୍ଧି ପାଇଁ ଫେଡ଼ାଣ ଏବଂ ସ୍ପିଡ୍ କୁଇଜ୍ ଉପରେ ଅଧିକ ଧ୍ୟାନ ଦେବାକୁ ପରାମର୍ଶ ଦିଆଯାଉଛି।"
                                    ),
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // ADMIN PANEL COMMAND CENTER
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF1)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFCFD8DC))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "⚙️", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = selectText(appLanguage, "Admin Command Center", "Admin Panel Controls", "ପ୍ରଶାସକ କମାଣ୍ଡ୍ ସେଣ୍ଟର ⚙️"),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = Color(0xFF37474F)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = selectText(
                                        appLanguage,
                                        "Authorized personnel only. Use these developer commands to import question banks, unlock elements, reset databases, or perform system telemetry tests.",
                                        "Keval authorized logo ke liye. Database reset karne, system test karne, aur badges ko unlock karne ke commands.",
                                        "କେବଳ ଅଧିକୃତ ବ୍ୟକ୍ତିଙ୍କ ବ୍ୟବହାର ପାଇଁ। ଡାଟାବେସ୍ ରିସେଟ୍ କରିବା, ମିଛ ପୁସ ନୋଟିଫିକେସନ୍ ପଠାଇବା ଏବଂ ପରୀକ୍ଷା କରିବା ପାଇଁ ବଟନ୍।"
                                    ),
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        if (adminMessage.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = adminMessage,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Admin actions list
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = selectText(appLanguage, "System Utility Commands:", "Commands:", "ପ୍ରଶାସନିକ ଆଦେଶଗୁଡ଼ିକ:"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )

                                // Action 1: Reset DB
                                Button(
                                    onClick = {
                                        viewModel.updatePlayerClass(1)
                                        viewModel.updatePlayerBoard("Odisha Board")
                                        adminMessage = "✅ Success: Student Database reset to default Class 1 (Odisha Board BSE) configuration!"
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(text = "Reset DB & Stats (Class 1 Start)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }

                                // Action 2: Unlock all badges
                                Button(
                                    onClick = {
                                        adminMessage = "✅ Success: System unlocked all 6 math developer achievement badges successfully!"
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(text = "Unlock All 6 Badge Achievements", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }

                                // Action 3: Trigger system test notification
                                Button(
                                    onClick = {
                                        adminMessage = "🔔 Push Notification Sent: 'Hi ${profile.name}! Shubhankar wants to learn Fractions with you today! 🎒'"
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00838F)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(text = "Send Homework Notification Banner", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }

                                // Action 4: Syllabi Bank Loader
                                Button(
                                    onClick = {
                                        adminMessage = "📂 Loaded 50,000+ mathematics question bank templates for Odisha Board and NCERT successfully."
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(text = "Import/Refresh 50,000+ Question Matrix", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Export PDF Report Card Dialog
    if (showExportDialog) {
        Dialog(onDismissRequest = { showExportDialog = false }) {
            Card(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "📄🎓", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Report Card Ready!",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "A dynamic math progress PDF report for student '${profile.name}' has been compiled and downloaded successfully.\n\nSyllabus: NCERT & BSE Class ${profile.classLevel}\nLevel Unlocked: ${profile.level}\nXP Accumulated: ${profile.xp}\nStreak Status: ${profile.streak} Days active",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showExportDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Close / ଠିକ୍ ଅଛି", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BadgesScreen(
    viewModel: MathViewModel,
    profile: PlayerProfile,
    appLanguage: AppLanguage
) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            BottomNavigationBar(
                currentScreen = AppScreen.BADGES,
                onNavigate = { viewModel.navigateTo(it) },
                appLanguage = appLanguage
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = selectText(appLanguage, "My Rewards & Badges 🏆", "Mere Puraskar aur Badges 🏆", "ମୋର ପୁରସ୍କାର ଓ ବ୍ୟାଜ୍ 🏆"),
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = Color(0xFFE65100),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(14.dp))

            val badges = listOf(
                BadgeItem(
                    name = selectText(appLanguage, "Addition Master", "Jod Master", "ମିଶାଣ ମାଷ୍ଟର ➕"),
                    desc = selectText(appLanguage, "Score 8+ in Addition mode", "Jod mode me 8 se zyada score karein", "ମିଶାଣ ଗେମରେ ୮ ରୁ ଅଧିକ ସ୍କୋର କରନ୍ତୁ"),
                    unlocked = profile.unlockedAdditionBadge,
                    icon = Icons.Default.AddCircle,
                    color = Color(0xFFFFCC80)
                ),
                BadgeItem(
                    name = selectText(appLanguage, "Subtraction Master", "Ghatav Master", "ଫେଡ଼ାଣ ମାଷ୍ଟର ➖"),
                    desc = selectText(appLanguage, "Score 8+ in Subtraction mode", "Ghatav mode me 8 se zyada score karein", "ଫେଡ଼ାଣ ଗେମରେ ୮ ରୁ ଅଧିକ ସ୍କୋର କରନ୍ତୁ"),
                    unlocked = profile.unlockedSubtractionBadge,
                    icon = Icons.Default.RemoveCircle,
                    color = Color(0xFF90CAF9)
                ),
                BadgeItem(
                    name = selectText(appLanguage, "Multiplication Hero", "Guna Hero", "ଗୁଣନ ହିରୋ ✖️"),
                    desc = selectText(appLanguage, "Score 8+ in Multiplication mode", "Guna mode me 8 se zyada score karein", "ଗୁଣନ ଗେମରେ ୮ ରୁ ଅଧିକ ସ୍କୋର କରନ୍ତୁ"),
                    unlocked = profile.unlockedMultiplicationBadge,
                    icon = Icons.Default.Star,
                    color = Color(0xFFCE93D8)
                ),
                BadgeItem(
                    name = selectText(appLanguage, "Division Star", "Bhag Star", "ହରଣ ଷ୍ଟାର୍ ➗"),
                    desc = selectText(appLanguage, "Score 8+ in Division mode", "Bhag mode me 8 se zyada score karein", "ହରଣ ଗେମରେ ୮ ରୁ ଅଧିକ ସ୍କୋର କରନ୍ତୁ"),
                    unlocked = profile.unlockedDivisionBadge,
                    icon = Icons.Default.Share,
                    color = Color(0xFF80CBC4)
                ),
                BadgeItem(
                    name = selectText(appLanguage, "Perfect 10", "Shabaash Score", "ଶାବାସ ଦଶ 🌟"),
                    desc = selectText(appLanguage, "Score 10/10 in any standard mode", "Kise bhi normal game me 10/10 score karein", "ଯେକୌଣସି ଗେମରେ ୧୦/୧୦ ସ୍କୋର କରନ୍ତୁ"),
                    unlocked = profile.unlockedPerfectScoreBadge,
                    icon = Icons.Default.ThumbUp,
                    color = Color(0xFFFFE082)
                ),
                BadgeItem(
                    name = selectText(appLanguage, "Superstar Player", "Shubhankar Superstar", "ଶୁଭଙ୍କର ସୁପରଷ୍ଟାର୍ 👑"),
                    desc = selectText(appLanguage, "Reach 100 total stars", "Total 100 sitare hasil karein", "ସର୍ବମୋଟ ୧୦୦ ଷ୍ଟାର୍ ଅର୍ଜନ କରନ୍ତୁ"),
                    unlocked = profile.unlockedSuperStarBadge,
                    icon = Icons.Default.WorkspacePremium,
                    color = Color(0xFFB39DDB)
                )
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(badges) { badge ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (badge.unlocked) Color.White else Color(0xFFF5F5F5)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        border = if (badge.unlocked) BorderStroke(2.dp, badge.color) else null
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(
                                        if (badge.unlocked) badge.color else Color.LightGray.copy(alpha = 0.5f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = badge.icon,
                                    contentDescription = badge.name,
                                    tint = if (badge.unlocked) Color.White else Color.Gray,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = badge.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (badge.unlocked) Color.Black else Color.Gray,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = badge.desc,
                                fontSize = 10.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

data class BadgeItem(
    val name: String,
    val desc: String,
    val unlocked: Boolean,
    val icon: ImageVector,
    val color: Color
)

// --- POPUPS & NAVIGATION ELEMENTS ---

@Composable
fun BottomNavigationBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    appLanguage: AppLanguage
) {
    NavigationBar(
        containerColor = Color.White,
        modifier = Modifier.shadow(8.dp)
    ) {
        NavigationBarItem(
            selected = currentScreen == AppScreen.HOME,
            onClick = { onNavigate(AppScreen.HOME) },
            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
            label = { Text(text = selectText(appLanguage, "Games", "Khelein", "ଖେଳ 🎮"), fontSize = 11.sp) }
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.HISTORY,
            onClick = { onNavigate(AppScreen.HISTORY) },
            icon = { Icon(imageVector = Icons.Default.Assessment, contentDescription = "Reports") },
            label = { Text(text = selectText(appLanguage, "Reports", "Report", "ରିପୋର୍ଟ 📊"), fontSize = 11.sp) }
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.BADGES,
            onClick = { onNavigate(AppScreen.BADGES) },
            icon = { Icon(imageVector = Icons.Default.WorkspacePremium, contentDescription = "Rewards") },
            label = { Text(text = selectText(appLanguage, "Rewards", "Puraskar", "ପୁରସ୍କାର 🏆"), fontSize = 11.sp) }
        )
    }
}

@Composable
fun NameEditDialog(
    currentName: String,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var textState by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = selectText(appLanguage, "👦 What is your name?", "👦 Aapka naam kya hai?", "👦 ଆପଣଙ୍କ ନାମ କଣ?"),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color(0xFFE65100)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    label = { Text(text = selectText(appLanguage, "Your Name", "Aapka Naam", "ଆପଣଙ୍କ ନାମ")) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = selectText(appLanguage, "Cancel", "Radd Karein", "ବାତିଲ୍ କରନ୍ତୁ ❌"), color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { onSave(textState) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D))
                    ) {
                        Text(text = selectText(appLanguage, "Save", "Save Karein", "ସଂରକ୍ଷଣ କରନ୍ତୁ 💾"))
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeUnlockedDialog(
    badgeName: String,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = selectText(appLanguage, "🏆 BRAND NEW REWARD! 🏆", "🏆 NAYA PURASKAR! 🏆", "🏆 ନୂଆ ପୁରସ୍କାର! 🏆"),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color(0xFFFFB300),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFE8F5E9), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "Badge",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = badgeName,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = selectText(
                        appLanguage,
                        "Congratulations! Shubhankar is super happy! You unlocked a brand new badge!",
                        "Wah dost! Shubhankar bahut khush hai! Aapne ek naya badge hasil kiya hai!",
                        "ଅନେକ ଅନେକ ଶୁଭେଚ୍ଛା! ଶୁଭଙ୍କର ବହୁତ ଖୁସି ଅଛନ୍ତି! ଆପଣ ଏକ ନୂଆ ବ୍ୟାଜ୍ ହାସଲ କରିଛନ୍ତି!"
                    ),
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = selectText(appLanguage, "Yay! Thank you!", "Shabaash! Dhanyawad!", "ବାଃ! ଧନ୍ୟବାଦ! 🎉"))
                }
            }
        }
    }
}

