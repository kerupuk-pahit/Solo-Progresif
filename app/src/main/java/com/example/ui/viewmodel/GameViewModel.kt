package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.*
import com.example.util.SoundSynthesizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(application)

    // Reactive State Flows
    val userStats: StateFlow<UserStats?> = repository.userStatsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val quests: StateFlow<List<QuestEntity>> = repository.allQuestsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val habitLogs: StateFlow<List<HabitLogEntity>> = repository.habitLogsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeLocks: StateFlow<List<ActiveLockEntity>> = repository.activeLocksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI & Loading States
    var isAILoading = MutableStateFlow(false)
        private set

    var systemNotification = MutableStateFlow<String?>(null)
        private set

    // Active Quest Timer States
    var activeTimerQuest = MutableStateFlow<QuestEntity?>(null)
        private set

    var isCountingDown = MutableStateFlow(false) // 3-second starting count
        private set

    var countdownSeconds = MutableStateFlow(3)
        private set

    var timerRemainingSeconds = MutableStateFlow(0)
        private set

    var isTimerPaused = MutableStateFlow(false)
        private set

    private var timerJob: Job? = null

    // App Lock Simulator State
    var simulatedCurrentOpenedApp = MutableStateFlow<String?>(null)
        private set

    init {
        viewModelScope.launch {
            // Initialize defaults and stats
            repository.initializeGameIfNecessary()
            
            // Check day rollover and reward daily rewards
            val rolloverMessage = repository.checkDailyRolloverAndLogin()
            if (rolloverMessage != null) {
                showSystemNotification(rolloverMessage)
            }
        }

        // Lock monitoring loop: decrease remaining duration for locked apps in real-time if simulated
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val currentApp = simulatedCurrentOpenedApp.value
                if (currentApp != null) {
                    val locks = repository.activeLocksFlow.firstOrNull() ?: emptyList()
                    val matchingLock = locks.find { it.appName == currentApp }
                    val now = System.currentTimeMillis()
                    
                    if (matchingLock == null || matchingLock.expiresAt <= now) {
                        // Time's up! Open the SYSTEM Lock Screen
                        // We keep simulatedCurrentOpenedApp as non-null but flag lock state to trigger the lock overlay
                    }
                }
            }
        }
    }

    // Floating notification helper
    fun showSystemNotification(message: String) {
        systemNotification.value = message
        viewModelScope.launch {
            delay(5000)
            if (systemNotification.value == message) {
                systemNotification.value = null
            }
        }
    }

    fun dismissNotification() {
        systemNotification.value = null
    }

    // Sounds
    fun playClickSound() {
        viewModelScope.launch { SoundSynthesizer.playClick() }
    }

    // Quest Creation
    fun createQuest(name: String, category: String, difficulty: String, duration: Int) {
        viewModelScope.launch {
            repository.createCustomQuest(name, category, difficulty, duration)
            showSystemNotification("SYSTEM: Quest Baru Ditambahkan: '$name'")
        }
    }

    // Quest Deletion
    fun deleteQuest(id: Int) {
        viewModelScope.launch {
            repository.deleteQuest(id)
            showSystemNotification("SYSTEM: Quest telah dihapus.")
        }
    }

    // Direct Complete (e.g. without timer, like Minum Air)
    fun quickCompleteQuest(id: Int) {
        viewModelScope.launch {
            val result = repository.completeQuest(id)
            handleQuestCompletionResult(result)
        }
    }

    // Quest Countdown Timer management
    fun startQuestTimer(quest: QuestEntity) {
        timerJob?.cancel()
        activeTimerQuest.value = quest
        isCountingDown.value = true
        countdownSeconds.value = 3
        isTimerPaused.value = false

        viewModelScope.launch {
            SoundSynthesizer.playWarning() // Countdown sound
            while (countdownSeconds.value > 1) {
                delay(1000)
                countdownSeconds.value -= 1
                SoundSynthesizer.playClick()
            }
            delay(1000)
            
            // Start the actual timer
            isCountingDown.value = false
            timerRemainingSeconds.value = quest.durationMinutes * 60
            runQuestTimerLoop()
        }
    }

    private fun runQuestTimerLoop() {
        timerJob = viewModelScope.launch {
            showSystemNotification("SYSTEM: Quest Dimulai! Tetap fokus pada tujuan Anda.")
            while (timerRemainingSeconds.value > 0) {
                delay(1000)
                if (!isTimerPaused.value) {
                    timerRemainingSeconds.value -= 1
                }
            }
            // Timer completed!
            val quest = activeTimerQuest.value
            if (quest != null) {
                val result = repository.completeQuest(quest.id)
                handleQuestCompletionResult(result)
            }
            resetTimerStates()
        }
    }

    fun pauseQuestTimer() {
        isTimerPaused.value = true
        viewModelScope.launch { SoundSynthesizer.playWarning() }
        showSystemNotification("SYSTEM: Timer Ditangguhkan.")
    }

    fun resumeQuestTimer() {
        isTimerPaused.value = false
        viewModelScope.launch { SoundSynthesizer.playClick() }
        showSystemNotification("SYSTEM: Timer Dilanjutkan.")
    }

    fun cancelQuestTimer() {
        timerJob?.cancel()
        viewModelScope.launch { SoundSynthesizer.playWarning() }
        showSystemNotification("SYSTEM: Quest Dibatalkan.")
        resetTimerStates()
    }

    private fun resetTimerStates() {
        activeTimerQuest.value = null
        isCountingDown.value = false
        timerRemainingSeconds.value = 0
        isTimerPaused.value = false
        timerJob = null
    }

    private fun handleQuestCompletionResult(result: CompleteQuestResult) {
        when (result) {
            is CompleteQuestResult.Success -> {
                val congrats = buildString {
                    append("SYSTEM: Quest Selesai!\n")
                    append("+${result.baseExp} EXP (+${result.bonusExp} Bonus Lucky Reward)\n")
                    append("+${result.baseGold} Emas (+${result.bonusGold} Bonus Lucky Reward)\n")
                    append("Lucky Reward bonus: +${result.luckyPercent}%\n")
                    if (result.leveledUp) {
                        append("LEVEL UP! Anda telah mencapai Level ${result.newLevel}!")
                    }
                }
                showSystemNotification(congrats)
            }
            is CompleteQuestResult.Error -> {
                showSystemNotification("SYSTEM: Gagal menyelesaikan quest: ${result.message}")
            }
        }
    }

    // Shop Purchasing
    fun purchaseEntertainment(appName: String, minutes: Int, baseCostGold: Int) {
        viewModelScope.launch {
            val result = repository.purchaseShopItem(appName, minutes, baseCostGold)
            when (result) {
                is ShopPurchaseResult.Success -> {
                    showSystemNotification("SYSTEM: Pembelian berhasil! Akses ke '$appName' dibuka selama $minutes menit.")
                }
                is ShopPurchaseResult.Error -> {
                    showSystemNotification("SYSTEM: ${result.message}")
                }
            }
        }
    }

    // Use Emergency Pass
    fun useEmergencyPass(appName: String) {
        viewModelScope.launch {
            val result = repository.useEmergencyPass(appName)
            when (result) {
                is ShopPurchaseResult.Success -> {
                    showSystemNotification("SYSTEM: Emergency Pass digunakan! Akses ke '$appName' dibuka selama 5 menit.")
                    // Immediately focus on that app in simulator to let them see it
                    simulatedCurrentOpenedApp.value = appName
                }
                is ShopPurchaseResult.Error -> {
                    showSystemNotification("SYSTEM: ${result.message}")
                }
            }
        }
    }

    // App Lock Simulation controls
    fun simulateOpeningApp(appName: String?) {
        simulatedCurrentOpenedApp.value = appName
        if (appName != null) {
            viewModelScope.launch { SoundSynthesizer.playClick() }
            showSystemNotification("SYSTEM: Memantau aktivitas: '$appName'")
        } else {
            showSystemNotification("SYSTEM: Kembali ke dasbor utama.")
        }
    }

    // Trigger Adaptive AI system
    fun runAIEvaluation() {
        if (isAILoading.value) return
        isAILoading.value = true
        viewModelScope.launch {
            val result = repository.runAdaptiveAIEvaluation()
            isAILoading.value = false
            when (result) {
                is AIEvaluationResult.Success -> {
                    val questTitles = result.quests.joinToString(", ") { it.name }
                    showSystemNotification("SYSTEM: ${result.message}\nMandatory Quests baru: $questTitles")
                }
                is AIEvaluationResult.Error -> {
                    showSystemNotification("SYSTEM: Gagal memproses data AI: ${result.message}")
                }
            }
        }
    }

    fun getDefaultLockedApps(): List<String> {
        return repository.getDefaultLockedApps()
    }
}
