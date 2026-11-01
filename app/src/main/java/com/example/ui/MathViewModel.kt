package com.example.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PlayerProfile
import com.example.data.ScoreRecord
import com.example.data.StatsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AppScreen {
    HOME, GAME_MODE, HISTORY, BADGES, ACTIVE_GAME, GAME_OVER
}

enum class AppLanguage {
    ENGLISH, HINGLISH, ODIA
}

enum class GameMode {
    COUNT, ADD, SUB, MUL, DIV, QUIZ
}

data class GameState(
    val gameMode: GameMode = GameMode.COUNT,
    val questionNumber: Int = 1,
    val maxQuestions: Int = 10,
    val numA: Int = 0,
    val numB: Int = 0,
    val operator: String = "",
    val correctAnswer: Int = 0,
    val options: List<Int> = emptyList(),
    val selectedAnswer: Int? = null,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val starsEarned: Int = 0,
    val emoji: String = "🍎",
    val lives: Int = 3,
    val timerSeconds: Int = 15,
    val shubhankarSpeech: String = "Namaste! Chalo shuru karte hain! 🚀",
    val solvedQuestions: List<String> = emptyList(), // history of formulas solved in current game
    val difficultyLevel: Int = 2, // 1: Very Easy, 2: Easy, 3: Medium, 4: Hard, 5: Genius
    val questionTextOverrideEN: String = "",
    val questionTextOverrideHI: String = "",
    val questionTextOverrideOR: String = ""
)

class MathViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StatsRepository
    private var toneGen: ToneGenerator? = null

    // UI Navigation State
    private val _activeScreen = MutableStateFlow(AppScreen.HOME)
    val activeScreen: StateFlow<AppScreen> = _activeScreen.asStateFlow()

    // Language state
    private val _appLanguage = MutableStateFlow(AppLanguage.HINGLISH)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    // Profile & Score stats from local DB
    private val _profile = MutableStateFlow(PlayerProfile())
    val profile: StateFlow<PlayerProfile> = _profile.asStateFlow()

    private val _history = MutableStateFlow<List<ScoreRecord>>(emptyList())
    val history: StateFlow<List<ScoreRecord>> = _history.asStateFlow()

    // Game Running State
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Dialog trigger for newly unlocked badge
    private val _unlockedBadgeName = MutableStateFlow<String?>(null)
    val unlockedBadgeName: StateFlow<String?> = _unlockedBadgeName.asStateFlow()

    private var timerJob: Job? = null

    private val emojis = listOf("🍎", "🎈", "🍦", "🧸", "🐱", "🚗", "🍭", "🍓", "🐶", "🍌", "🍩", "🦋", "🦁", "🐼")

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StatsRepository(database.statsDao())

        // Initialize Tone Generator
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Collect Player Profile from Database
        viewModelScope.launch {
            repository.profile.collectLatest { dbProfile ->
                if (dbProfile == null) {
                    val defaultProfile = PlayerProfile()
                    repository.saveProfile(defaultProfile)
                    _profile.value = defaultProfile
                } else {
                    _profile.value = dbProfile
                }
            }
        }

        // Collect Score History
        viewModelScope.launch {
            repository.scoreRecords.collectLatest { records ->
                _history.value = records
            }
        }
    }

    private fun playTone(correct: Boolean) {
        viewModelScope.launch {
            try {
                if (correct) {
                    toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    delay(120)
                    toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
                } else {
                    toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _activeScreen.value = screen
        if (screen == AppScreen.HOME) {
            stopTimer()
        }
    }

    fun toggleLanguage() {
        _appLanguage.value = when (_appLanguage.value) {
            AppLanguage.ENGLISH -> AppLanguage.HINGLISH
            AppLanguage.HINGLISH -> AppLanguage.ODIA
            AppLanguage.ODIA -> AppLanguage.ENGLISH
        }
        updateShubhankarSpeechOnLanguageChange()
    }

    fun dismissBadgeDialog() {
        _unlockedBadgeName.value = null
    }

    fun updatePlayerName(newName: String) {
        viewModelScope.launch {
            val updated = _profile.value.copy(name = newName.ifBlank { "Shubhankar" })
            repository.updateProfile(updated)
        }
    }

    fun updatePlayerClass(newClass: Int) {
        viewModelScope.launch {
            val updated = _profile.value.copy(classLevel = newClass)
            repository.updateProfile(updated)
        }
    }

    fun updatePlayerBoard(newBoard: String) {
        viewModelScope.launch {
            val updated = _profile.value.copy(board = newBoard)
            repository.updateProfile(updated)
        }
    }

    fun updatePlayerState(newState: String) {
        viewModelScope.launch {
            val updated = _profile.value.copy(stateName = newState)
            repository.updateProfile(updated)
        }
    }

    fun clearGameHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // --- GAME ENGINE ---

    fun startGame(mode: GameMode) {
        _gameState.value = GameState(
            gameMode = mode,
            questionNumber = 1,
            score = 0,
            starsEarned = 0,
            lives = if (mode == GameMode.QUIZ) 3 else 0,
            emoji = emojis.random(),
            shubhankarSpeech = getIntroSpeech(mode)
        )
        generateQuestion()
        _activeScreen.value = AppScreen.ACTIVE_GAME
    }

    private fun getIntroSpeech(mode: GameMode): String {
        return when (_appLanguage.value) {
            AppLanguage.ENGLISH -> {
                when (mode) {
                    GameMode.COUNT -> "Hello friend! Let's count the fun objects together! 🎒"
                    GameMode.ADD -> "Superb! Let's add and find the sum together! ➕"
                    GameMode.SUB -> "Awesome! Let's take away numbers and subtract! ➖"
                    GameMode.MUL -> "Fantastic! Let's multiply using colorful shapes! ✖️"
                    GameMode.DIV -> "Great! Let's share candies and divide! ➗"
                    GameMode.QUIZ -> "Welcome to the Ultimate Math Speed Challenge! ⚡"
                }
            }
            AppLanguage.HINGLISH -> {
                when (mode) {
                    GameMode.COUNT -> "Namaste dost! Chalo pyari cheezon ko sath ginte hain! 🎒"
                    GameMode.ADD -> "Bahut badhiya! Chalo jodna seekhein! ➕"
                    GameMode.SUB -> "Chalo ghatana seekhein aur puzzle solve karein! ➖"
                    GameMode.MUL -> "Chalo table yaad karein aur guna seekhein! ✖️"
                    GameMode.DIV -> "Chalo candy barabar baantein aur bhag karna seekhein! ➗"
                    GameMode.QUIZ -> "Shubhankar ki Math Speed Quiz me aapka swagat hai! ⚡"
                }
            }
            AppLanguage.ODIA -> {
                when (mode) {
                    GameMode.COUNT -> "ନମସ୍କାର ସାଙ୍ଗ! ଚାଲ ଏହି ସୁନ୍ଦର ଜିନିଷଗୁଡ଼ିକୁ ଏକାଠି ଗଣିବା! 🎒"
                    GameMode.ADD -> "ବହୁତ ବଢ଼ିଆ! ଚାଲ ମିଶାଣ ଶିଖିବା! ➕"
                    GameMode.SUB -> "ଚାଲ ଫେଡ଼ାଣ ଶିଖିବା ଏବଂ ପ୍ରଶ୍ନ ସମାଧାନ କରିବା! ➖"
                    GameMode.MUL -> "ଚାଲ ଗୁଣନ ଶିଖିବା ଏବଂ ମଜା କରିବା! ✖️"
                    GameMode.DIV -> "ଚାଲ କ୍ୟାଣ୍ଡି ସମାନ ଭାବରେ ବାଣ୍ଟିବା ଏବଂ ହରଣ ଶିଖିବା! ➗"
                    GameMode.QUIZ -> "ଶୁଭଙ୍କରଙ୍କ ଗଣିତ ସ୍ପିଡ୍ କୁଇଜ୍‌କୁ ଆପଣଙ୍କୁ ସ୍ୱାଗତ! ⚡"
                }
            }
        }
    }

    private fun updateShubhankarSpeechOnLanguageChange() {
        val current = _gameState.value
        if (current.selectedAnswer != null) {
            val correct = current.isCorrect == true
            _gameState.value = current.copy(
                shubhankarSpeech = getAnswerSpeech(correct)
            )
        } else {
            _gameState.value = current.copy(
                shubhankarSpeech = getIntroSpeech(current.gameMode)
            )
        }
    }

    private fun generateQuestion() {
        stopTimer()
        val current = _gameState.value
        val mode = current.gameMode
        val difficulty = current.difficultyLevel
        val classLvl = _profile.value.classLevel // 1 to 8

        var numA = 0
        var numB = 0
        var operator = ""
        var correctAnswer = 0
        val options = mutableSetOf<Int>()
        var qOverrideEN = ""
        var qOverrideHI = ""
        var qOverrideOR = ""

        val randomMode = if (mode == GameMode.QUIZ) {
            val quizModes = listOf(GameMode.COUNT, GameMode.ADD, GameMode.SUB, GameMode.MUL, GameMode.DIV)
            quizModes.random()
        } else {
            mode
        }

        // Generate curriculum-aligned math based on Class (1-8)
        if (classLvl <= 2) {
            // Early Grades: Focus on intuitive, visual math: Counting, 1-2 digit addition/subtraction, small tables.
            when (randomMode) {
                GameMode.COUNT -> {
                    correctAnswer = if (classLvl == 1) Random.nextInt(1, 8) else Random.nextInt(3, 15)
                    numA = correctAnswer
                    operator = "COUNT"
                    options.add(correctAnswer)
                    while (options.size < 4) {
                        options.add(Random.nextInt(Math.max(1, correctAnswer - 4), correctAnswer + 5))
                    }
                }
                GameMode.ADD -> {
                    numA = if (classLvl == 1) Random.nextInt(1, 8) else Random.nextInt(5, 18)
                    numB = if (classLvl == 1) Random.nextInt(1, 8) else Random.nextInt(3, 12)
                    operator = "+"
                    correctAnswer = numA + numB
                    options.add(correctAnswer)
                    while (options.size < 4) {
                        options.add(Random.nextInt(Math.max(1, correctAnswer - 5), correctAnswer + 6))
                    }
                }
                GameMode.SUB -> {
                    numA = if (classLvl == 1) Random.nextInt(4, 9) else Random.nextInt(10, 25)
                    numB = if (classLvl == 1) Random.nextInt(1, numA) else Random.nextInt(1, numA - 2)
                    operator = "-"
                    correctAnswer = numA - numB
                    options.add(correctAnswer)
                    while (options.size < 4) {
                        options.add(Random.nextInt(Math.max(0, correctAnswer - 4), correctAnswer + 5))
                    }
                }
                GameMode.MUL -> {
                    // Small visual multiplication (tables of 2, 3 or 5)
                    numA = Random.nextInt(2, 5)
                    numB = Random.nextInt(1, 6)
                    operator = "×"
                    correctAnswer = numA * numB
                    options.add(correctAnswer)
                    while (options.size < 4) {
                        options.add(Random.nextInt(Math.max(1, correctAnswer - 5), correctAnswer + 6))
                    }
                }
                GameMode.DIV -> {
                    // Small simple divisions
                    numB = Random.nextInt(2, 4)
                    correctAnswer = Random.nextInt(1, 6)
                    numA = numB * correctAnswer
                    operator = "÷"
                    options.add(correctAnswer)
                    while (options.size < 4) {
                        options.add(Random.nextInt(1, correctAnswer + 5))
                    }
                }
                else -> {}
            }
        } else if (classLvl <= 4) {
            // Intermediate Grades: Fractions, 2-3 digit addition, tables up to 12, division with remainder, pattern tracking.
            val topicChoice = Random.nextInt(1, 4)
            if (topicChoice == 1 && randomMode == GameMode.COUNT) {
                // Fractions visual override!
                val denom = listOf(4, 6, 8, 10).random()
                val num = Random.nextInt(1, denom)
                correctAnswer = num
                qOverrideEN = "Express the fraction of shaded items: $num out of $denom parts. Find numerator!"
                qOverrideHI = "Bhag gino: $denom me se $num bhag shaded hai. Numerator (ansh) kya hoga?"
                qOverrideOR = "ଭଗ୍ନାଂଶ ଚିହ୍ନଟ କର: $denom ଭାଗରୁ $num ଭାଗ ଚିତ୍ରିତ ଅଛି। ଲବ (numerator) କଣ ହେବ?"
                options.add(correctAnswer)
                while (options.size < 4) {
                    options.add(Random.nextInt(1, denom + 1))
                }
            } else {
                when (randomMode) {
                    GameMode.COUNT -> {
                        // Skip Counting pattern
                        val step = Random.nextInt(2, 6)
                        val start = Random.nextInt(1, 20)
                        val v1 = start
                        val v2 = start + step
                        val v3 = start + step * 2
                        correctAnswer = start + step * 3
                        qOverrideEN = "Complete the skip-counting pattern: $v1, $v2, $v3, ?"
                        qOverrideHI = "Pattern poora karo dost: $v1, $v2, $v3, ?"
                        qOverrideOR = "ଢାଞ୍ଚାଟି ପୂରଣ କରନ୍ତୁ: $v1, $v2, $v3, ?"
                        options.add(correctAnswer)
                        while (options.size < 4) {
                            options.add(correctAnswer + listOf(-step, step, step * 2, -step * 2).random())
                        }
                    }
                    GameMode.ADD -> {
                        numA = Random.nextInt(25, 250)
                        numB = Random.nextInt(15, 150)
                        operator = "+"
                        correctAnswer = numA + numB
                        options.add(correctAnswer)
                        while (options.size < 4) {
                            options.add(Random.nextInt(Math.max(50, correctAnswer - 20), correctAnswer + 25))
                        }
                    }
                    GameMode.SUB -> {
                        numA = Random.nextInt(100, 300)
                        numB = Random.nextInt(10, 99)
                        operator = "-"
                        correctAnswer = numA - numB
                        options.add(correctAnswer)
                        while (options.size < 4) {
                            options.add(Random.nextInt(Math.max(10, correctAnswer - 15), correctAnswer + 20))
                        }
                    }
                    GameMode.MUL -> {
                        numA = Random.nextInt(6, 13)
                        numB = Random.nextInt(4, 12)
                        operator = "×"
                        correctAnswer = numA * numB
                        options.add(correctAnswer)
                        while (options.size < 4) {
                            options.add(Random.nextInt(Math.max(10, correctAnswer - 12), correctAnswer + 15))
                        }
                    }
                    GameMode.DIV -> {
                        numB = Random.nextInt(3, 10)
                        correctAnswer = Random.nextInt(4, 15)
                        numA = numB * correctAnswer
                        operator = "÷"
                        options.add(correctAnswer)
                        while (options.size < 4) {
                            options.add(Random.nextInt(1, correctAnswer + 6))
                        }
                    }
                    else -> {}
                }
            }
        } else if (classLvl <= 6) {
            // Upper Primary: Decimals, Percentages, ratios, geometry perimeter/area, basic algebra variables
            val subTopic = Random.nextInt(1, 5)
            if (subTopic == 1) {
                // Basic Algebra Finding X
                val coeff = Random.nextInt(1, 4)
                val const = Random.nextInt(2, 15)
                val x = Random.nextInt(2, 10)
                val rhs = coeff * x + const
                correctAnswer = x
                if (coeff == 1) {
                    qOverrideEN = "Find x if: x + $const = $rhs"
                    qOverrideHI = "x ka maan nikalo agar: x + $const = $rhs"
                    qOverrideOR = "x ର ମୂଲ୍ୟ ବାହାର କର ଯଦି: x + $const = $rhs"
                } else {
                    qOverrideEN = "Find x if: ${coeff}x + $const = $rhs"
                    qOverrideHI = "x ka maan nikalo agar: ${coeff}x + $const = $rhs"
                    qOverrideOR = "x ର ମୂଲ୍ୟ ବାହାର କର ଯଦି: ${coeff}x + $const = $rhs"
                }
                options.add(correctAnswer)
                while (options.size < 4) {
                    options.add(Random.nextInt(1, 12))
                }
            } else if (subTopic == 2) {
                // Geometry Perimeter and Area
                val length = Random.nextInt(4, 12)
                val width = Random.nextInt(2, length)
                val areaOrPeri = Random.nextBoolean()
                if (areaOrPeri) {
                    correctAnswer = length * width
                    qOverrideEN = "Area of a rectangle with length $length cm and width $width cm in sq cm?"
                    qOverrideHI = "Ek aayat (rectangle) ka kshetrafal (area) kya hoga jiske lambai $length aur chaudai $width hai?"
                    qOverrideOR = "ଏକ ଆୟତକ୍ଷେତ୍ରର କ୍ଷେତ୍ରଫଳ କେତେ ହେବ ଯାହାର ଦୈର୍ଘ୍ୟ $length ସେମି ଓ ପ୍ରସ୍ଥ $width ସେମି?"
                } else {
                    correctAnswer = 2 * (length + width)
                    qOverrideEN = "Perimeter of a rectangle with length $length cm and width $width cm in cm?"
                    qOverrideHI = "Ek aayat ka parimap (perimeter) kya hoga agar lambai $length aur chaudai $width hai?"
                    qOverrideOR = "ଏକ ଆୟତକ୍ଷେତ୍ରର ପରିସୀମା କେତେ ହେବ ଯାହାର ଦୈର୍ଘ୍ୟ $length ସେମି ଓ ପ୍ରସ୍ଥ $width ସେମି?"
                }
                options.add(correctAnswer)
                while (options.size < 4) {
                    options.add(Random.nextInt(Math.max(4, correctAnswer - 10), correctAnswer + 12))
                }
            } else if (subTopic == 3) {
                // Percentages
                val base = listOf(50, 100, 200, 500).random()
                val percent = listOf(10, 20, 25, 50).random()
                correctAnswer = (base * percent) / 100
                qOverrideEN = "What is $percent% of $base?"
                qOverrideHI = "$base ka $percent% kitna hoga?"
                qOverrideOR = "$base ର $percent% କେତେ ହେବ?"
                options.add(correctAnswer)
                while (options.size < 4) {
                    options.add(Random.nextInt(Math.max(1, correctAnswer - 15), correctAnswer + 20))
                }
            } else {
                // Negative Integers
                val a = Random.nextInt(-15, 15)
                val b = Random.nextInt(1, 15)
                val op = listOf("+", "-").random()
                if (op == "+") {
                    correctAnswer = a + b
                } else {
                    correctAnswer = a - b
                }
                qOverrideEN = "Solve the integer expression: $a $op $b"
                qOverrideHI = "Integer ko solve karo dost: $a $op $b"
                qOverrideOR = "ପୂର୍ଣ୍ଣସଂଖ୍ୟାକୁ ସରଳ କର: $a $op $b"
                options.add(correctAnswer)
                while (options.size < 4) {
                    options.add(Random.nextInt(correctAnswer - 5, correctAnswer + 6))
                }
            }
        } else {
            // Class 7-8: Advanced Linear Equations, Exponents, Square roots, Mensuration
            val subTopic = Random.nextInt(1, 5)
            if (subTopic == 1) {
                // Exponents & Powers
                val base = listOf(2, 3, 5, 10).random()
                val exp = if (base == 10) Random.nextInt(1, 4) else if (base == 5) Random.nextInt(1, 4) else Random.nextInt(2, 5)
                correctAnswer = Math.pow(base.toDouble(), exp.toDouble()).toInt()
                qOverrideEN = "Evaluate: $base raised to the power of $exp ($base^$exp)"
                qOverrideHI = "$base ki power $exp ka maan kya hoga?"
                qOverrideOR = "ମୂଲ୍ୟ ନିରୂପଣ କର: $base ର ଘାତ $exp ($base^$exp)"
                options.add(correctAnswer)
                while (options.size < 4) {
                    options.add(Random.nextInt(Math.max(1, correctAnswer - 20), correctAnswer + 30))
                }
            } else if (subTopic == 2) {
                // Square Roots
                val roots = listOf(4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 20, 25)
                val ans = roots.random()
                val num = ans * ans
                correctAnswer = ans
                qOverrideEN = "Find the square root (√) of $num"
                qOverrideHI = "$num ka vargmool (√) nikalein"
                qOverrideOR = "$num ର ବର୍ଗମୂଳ (√) ବାହାର କରନ୍ତୁ"
                options.add(correctAnswer)
                while (options.size < 4) {
                    options.add(Random.nextInt(Math.max(2, correctAnswer - 5), correctAnswer + 6))
                }
            } else if (subTopic == 3) {
                // Linear Equations in One Variable
                val coeff = Random.nextInt(2, 6)
                val const = Random.nextInt(1, 20)
                val x = Random.nextInt(2, 12)
                val rhs = coeff * x - const
                correctAnswer = x
                qOverrideEN = "Solve for x: ${coeff}x - $const = $rhs"
                qOverrideHI = "x ka maan batayein: ${coeff}x - $const = $rhs"
                qOverrideOR = "x ର ମୂଲ୍ୟ ସମାଧାନ କର: ${coeff}x - $const = $rhs"
                options.add(correctAnswer)
                while (options.size < 4) {
                    options.add(Random.nextInt(1, 15))
                }
            } else {
                // Advanced Mensuration / Pythagorean
                val sides = listOf(Pair(3, 4), Pair(5, 12), Pair(6, 8), Pair(8, 15))
                val chosen = sides.random()
                correctAnswer = Math.sqrt((chosen.first * chosen.first + chosen.second * chosen.second).toDouble()).toInt()
                qOverrideEN = "In a right-angled triangle, if base is ${chosen.first} cm and height is ${chosen.second} cm, find hypotenuse!"
                qOverrideHI = "Ek right triangle me, agar base ${chosen.first} aur height ${chosen.second} hai toh hypotenuse (karn) kya hoga?"
                qOverrideOR = "ଏକ ସମକୋଣୀ ତ୍ରିଭୁଜର ଭୂମି ${chosen.first} ସେମି ଓ ଉଚ୍ଚତା ${chosen.second} ସେମି ହେଲେ, ଏହାର କର୍ଣ୍ଣ କେତେ ସେମି?"
                options.add(correctAnswer)
                while (options.size < 4) {
                    options.add(Random.nextInt(Math.max(3, correctAnswer - 6), correctAnswer + 8))
                }
            }
        }

        _gameState.value = current.copy(
            numA = numA,
            numB = numB,
            operator = operator,
            correctAnswer = correctAnswer,
            options = options.toList().shuffled(),
            selectedAnswer = null,
            isCorrect = null,
            emoji = emojis.random(),
            timerSeconds = if (mode == GameMode.QUIZ) 15 else 0,
            questionTextOverrideEN = qOverrideEN,
            questionTextOverrideHI = qOverrideHI,
            questionTextOverrideOR = qOverrideOR
        )

        if (mode == GameMode.QUIZ) {
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (_gameState.value.timerSeconds > 0) {
                delay(1000)
                val current = _gameState.value
                if (current.selectedAnswer != null) break // User answered, stop timer

                _gameState.value = current.copy(timerSeconds = current.timerSeconds - 1)
            }
            if (_gameState.value.selectedAnswer == null) {
                // Time up! Count as incorrect
                submitAnswer(-1)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun submitAnswer(answer: Int) {
        stopTimer()
        val current = _gameState.value
        if (current.selectedAnswer != null) return // Already answered

        val correct = answer == current.correctAnswer
        val newScore = if (correct) current.score + 1 else current.score
        val newStars = if (correct) current.starsEarned + 1 else current.starsEarned
        val newLives = if (!correct && current.gameMode == GameMode.QUIZ) current.lives - 1 else current.lives

        // Adaptive Difficulty Adjustment
        val currentDifficulty = current.difficultyLevel
        val newDifficulty = if (correct) {
            Math.min(5, currentDifficulty + 1)
        } else {
            Math.max(1, currentDifficulty - 1)
        }

        playTone(correct)

        val formulaStr = when (current.operator) {
            "COUNT" -> "Count ${current.numA} ${current.emoji} = ${current.correctAnswer} (${if (correct) "✓" else "✗"})"
            else -> "${current.numA} ${current.operator} ${current.numB} = ${current.correctAnswer} (${if (correct) "✓" else "✗"})"
        }
        val updatedSolved = current.solvedQuestions + formulaStr

        // Create feedback for difficulty scaling
        val diffSpeech = when (_appLanguage.value) {
            AppLanguage.ENGLISH -> {
                if (correct && newDifficulty > currentDifficulty) " Next question is going to be harder! 🚀"
                else if (!correct && newDifficulty < currentDifficulty) " Let's make the next question easier! 😊"
                else ""
            }
            AppLanguage.HINGLISH -> {
                if (correct && newDifficulty > currentDifficulty) " Agla sawal thoda mushkil hone wala hai! 🚀"
                else if (!correct && newDifficulty < currentDifficulty) " Agla sawal thoda aasan karte hain! 😊"
                else ""
            }
            AppLanguage.ODIA -> {
                if (correct && newDifficulty > currentDifficulty) " ପରବର୍ତ୍ତୀ ପ୍ରଶ୍ନ ଟିକେ କଷ୍ଟ ହେବ! 🚀"
                else if (!correct && newDifficulty < currentDifficulty) " ଚାଲ ପରବର୍ତ୍ତୀ ପ୍ରଶ୍ନ ସହଜ କରିବା! 😊"
                else ""
            }
        }

        _gameState.value = current.copy(
            selectedAnswer = answer,
            isCorrect = correct,
            score = newScore,
            starsEarned = newStars,
            lives = newLives,
            shubhankarSpeech = getAnswerSpeech(correct) + diffSpeech,
            solvedQuestions = updatedSolved,
            difficultyLevel = newDifficulty
        )

        // After small delay, proceed to next question or end game
        viewModelScope.launch {
            delay(2500)
            val currentUpdated = _gameState.value
            if (currentUpdated.gameMode == GameMode.QUIZ && currentUpdated.lives <= 0) {
                endGame()
            } else if (currentUpdated.questionNumber >= currentUpdated.maxQuestions) {
                endGame()
            } else {
                _gameState.value = currentUpdated.copy(
                    questionNumber = currentUpdated.questionNumber + 1
                )
                generateQuestion()
            }
        }
    }

    private fun getAnswerSpeech(correct: Boolean): String {
        return when (_appLanguage.value) {
            AppLanguage.ENGLISH -> {
                if (correct) {
                    listOf(
                        "Wow! Correct Answer! You are a superstar! ⭐",
                        "Fantastic! That is the right answer! 🎉",
                        "Brilliant! Keep going, my smart friend! 🧠",
                        "Excellent! Shubhankar is super proud of you! 🧸"
                    ).random()
                } else {
                    listOf(
                        "Oops! Don't worry, try the next one! 💪",
                        "Close one! Let's double check and try again! 👍",
                        "No problem! We learn by making mistakes! 😊",
                        "Keep trying! Shubhankar believes in you! 🌟"
                    ).random()
                }
            }
            AppLanguage.HINGLISH -> {
                if (correct) {
                    listOf(
                        "Wah! Sahi Jawab! Aap toh superstar hain! ⭐",
                        "Bahut badhiya! Bilkul sahi jawab! 🎉",
                        "Shabaash mere dost! Aise hi aage badhte raho! 🧠",
                        "Maza aa gaya! Shubhankar ko aap par garv hai! 🧸"
                    ).random()
                } else {
                    listOf(
                        "Oops! Koi baat nahi, agli baar bilkul sahi hoga! 💪",
                        "Thoda sa reh gaya! Chalo agli ginti dhyan se karein! 👍",
                        "Fikar mat karo dost! Galtiyon se hi toh seekhte hain! 😊",
                        "Koshish karte raho! Mujhe pata hai aap kar sakte ho! 🌟"
                    ).random()
                }
            }
            AppLanguage.ODIA -> {
                if (correct) {
                    listOf(
                        "ବାଃ! ସଠିକ୍ ଉତ୍ତର! ତୁମେ ଜଣେ ସୁପରଷ୍ଟାର୍! ⭐",
                        "ଚମତ୍କାର! ଏହା ବିଲକୁଲ ସଠିକ୍ ଉତ୍ତର! 🎉",
                        "ବୁଦ୍ଧିମାନ ପିଲା! ଏମିତି ଆଗକୁ ବଢ଼ୁଥାଅ ମୋ ସାଙ୍ଗ! 🧠",
                        "ଅତି ସୁନ୍ଦର! ଶୁଭଙ୍କର ତୁମ ପାଇଁ ବହୁତ ଗର୍ବିତ! 🧸"
                    ).random()
                } else {
                    listOf(
                        "ଓହୋ! ବ୍ୟସ୍ତ ହୁଅନି, ପରବର୍ତ୍ତୀ ପ୍ରଶ୍ନ ଚେଷ୍ଟା କର! 💪",
                        "ଅଳ୍ପକେ ରହିଗଲା! ଚାଲ ଆଉ ଥରେ ଚେଷ୍ଟା କରିବା! 👍",
                        "କିଛି ଅସୁବିଧା ନାହିଁ! ଭୁଲ୍ କରି ହିଁ ଆମେ ଶିଖିବା! 😊",
                        "ଚେଷ୍ଟା ଜାରି ରଖ! ଶୁଭଙ୍କରର ତୁମ ଉପରେ ବିଶ୍ୱାସ ଅଛି! 🌟"
                    ).random()
                }
            }
        }
    }

    private fun endGame() {
        stopTimer()
        val current = _gameState.value

        viewModelScope.launch {
            // Save Score Record
            val record = ScoreRecord(
                gameMode = current.gameMode.name,
                score = current.score,
                totalQuestions = current.questionNumber,
                starsEarned = current.starsEarned
            )
            repository.insertScoreRecord(record)

            // Update Player Stats & check for Badges
            val currentProfile = _profile.value
            val updatedStars = currentProfile.stars + current.starsEarned
            val updatedCorrect = currentProfile.correctAnswers + current.score
            
            // Coins: +12 per correct answer, +5 per star
            val earnedCoins = current.score * 12 + current.starsEarned * 5
            val updatedCoins = currentProfile.coins + earnedCoins
            
            // XP: +20 per correct answer
            val earnedXp = current.score * 20
            val updatedXp = currentProfile.xp + earnedXp
            
            // Level is calculated: 1 + (XP / 100)
            val newLevel = 1 + (updatedXp / 100)

            // Streak update: If last active was yesterday, increment streak. 
            // If active today, keep. Else reset to 1
            val diffMs = System.currentTimeMillis() - currentProfile.lastActiveDate
            val currentStreak = currentProfile.streak
            val updatedStreak = if (diffMs > 18 * 3600 * 1000 && diffMs < 36 * 3600 * 1000) {
                currentStreak + 1
            } else if (diffMs >= 36 * 3600 * 1000) {
                1
            } else {
                currentStreak
            }

            var badgeAddition = currentProfile.unlockedAdditionBadge
            var badgeSubtraction = currentProfile.unlockedSubtractionBadge
            var badgeMultiplication = currentProfile.unlockedMultiplicationBadge
            var badgeDivision = currentProfile.unlockedDivisionBadge
            var badgePerfect = currentProfile.unlockedPerfectScoreBadge
            var badgeSuperStar = currentProfile.unlockedSuperStarBadge

            var newlyUnlockedBadge: String? = null

            // Badge Conditions
            if (current.gameMode == GameMode.ADD && current.score >= 8 && !badgeAddition) {
                badgeAddition = true
                newlyUnlockedBadge = when (_appLanguage.value) {
                    AppLanguage.ENGLISH -> "Addition Master"
                    AppLanguage.HINGLISH -> "Jod Master"
                    AppLanguage.ODIA -> "ମିଶାଣ ମାଷ୍ଟର 🏆"
                }
            }
            if (current.gameMode == GameMode.SUB && current.score >= 8 && !badgeSubtraction) {
                badgeSubtraction = true
                newlyUnlockedBadge = when (_appLanguage.value) {
                    AppLanguage.ENGLISH -> "Subtraction Master"
                    AppLanguage.HINGLISH -> "Ghatav Master"
                    AppLanguage.ODIA -> "ଫେଡ଼ାଣ ମାଷ୍ଟର 🏆"
                }
            }
            if (current.gameMode == GameMode.MUL && current.score >= 8 && !badgeMultiplication) {
                badgeMultiplication = true
                newlyUnlockedBadge = when (_appLanguage.value) {
                    AppLanguage.ENGLISH -> "Multiplication Hero"
                    AppLanguage.HINGLISH -> "Guna Hero"
                    AppLanguage.ODIA -> "ଗୁଣନ ହିରୋ 🏆"
                }
            }
            if (current.gameMode == GameMode.DIV && current.score >= 8 && !badgeDivision) {
                badgeDivision = true
                newlyUnlockedBadge = when (_appLanguage.value) {
                    AppLanguage.ENGLISH -> "Division Star"
                    AppLanguage.HINGLISH -> "Bhag Star"
                    AppLanguage.ODIA -> "ହରଣ ଷ୍ଟାର୍ 🏆"
                }
            }
            if (current.score == current.questionNumber && current.score >= 5 && !badgePerfect) {
                badgePerfect = true
                newlyUnlockedBadge = when (_appLanguage.value) {
                    AppLanguage.ENGLISH -> "Perfect 10"
                    AppLanguage.HINGLISH -> "Shabaash Score"
                    AppLanguage.ODIA -> "ସମ୍ପୂର୍ଣ୍ଣ ସ୍କୋର 🏆"
                }
            }
            if (updatedStars >= 100 && !badgeSuperStar) {
                badgeSuperStar = true
                newlyUnlockedBadge = when (_appLanguage.value) {
                    AppLanguage.ENGLISH -> "Superstar Player"
                    AppLanguage.HINGLISH -> "Shubhankar Superstar"
                    AppLanguage.ODIA -> "ଶୁଭଙ୍କର ସୁପରଷ୍ଟାର୍ 🏆"
                }
            }

            val updatedProfile = currentProfile.copy(
                stars = updatedStars,
                correctAnswers = updatedCorrect,
                coins = updatedCoins,
                xp = updatedXp,
                level = newLevel,
                streak = updatedStreak,
                lastActiveDate = System.currentTimeMillis(),
                unlockedAdditionBadge = badgeAddition,
                unlockedSubtractionBadge = badgeSubtraction,
                unlockedMultiplicationBadge = badgeMultiplication,
                unlockedDivisionBadge = badgeDivision,
                unlockedPerfectScoreBadge = badgePerfect,
                unlockedSuperStarBadge = badgeSuperStar
            )
            repository.saveProfile(updatedProfile)

            // Trigger Badge dialog celebration if newly unlocked
            if (newlyUnlockedBadge != null) {
                _unlockedBadgeName.value = newlyUnlockedBadge
            }

            // Set final game over state
            val finalSpeech = when (_appLanguage.value) {
                AppLanguage.ENGLISH -> "Awesome job! You finished the game and earned ${current.starsEarned} Stars, $earnedCoins Coins, and $earnedXp XP! 🌟"
                AppLanguage.HINGLISH -> "Wah dost! Aapne game poora kiya aur ${current.starsEarned} Sitare, $earnedCoins Coins, aur $earnedXp XP jeete! 🌟"
                AppLanguage.ODIA -> "ଚମତ୍କାର ସାଙ୍ଗ! ତୁମେ ଖେଳ ସମାପ୍ତ କରିଛ ଏବଂ ${current.starsEarned} ଟି ଷ୍ଟାର୍, $earnedCoins କଏନ୍ ଏବଂ $earnedXp XP ଜିତିଛ! 🌟"
            }

            _gameState.value = current.copy(
                shubhankarSpeech = finalSpeech
            )

            _activeScreen.value = AppScreen.GAME_OVER
        }
    }

    fun continueGame() {
        val current = _gameState.value
        _gameState.value = current.copy(
            questionNumber = 1, // Reset question sequence
            // Keeps current score and starsEarned to build on top
            lives = if (current.gameMode == GameMode.QUIZ) 3 else 0,
            shubhankarSpeech = when (_appLanguage.value) {
                AppLanguage.ENGLISH -> "Awesome! Let's continue our adventure and solve more math questions! 🚀"
                AppLanguage.HINGLISH -> "Swagat hai phir se! Chalo math ki masti jaari rakhein! 🚀"
                AppLanguage.ODIA -> "ଅତି ସୁନ୍ଦର! ଚାଲ ଆମର ଏହି ଗଣିତ ଯାତ୍ରା ଚାଲୁ ରଖିବା ଏବଂ ଅଧିକ ପ୍ରଶ୍ନ ସମାଧାନ କରିବା! 🚀"
            }
        )
        generateQuestion()
        _activeScreen.value = AppScreen.ACTIVE_GAME
    }
}
