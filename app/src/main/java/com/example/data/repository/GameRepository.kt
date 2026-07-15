package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.data.api.Content
import com.example.data.api.GeminiClient
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.local.*
import com.example.util.SoundSynthesizer
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class GameRepository(private val context: Context) {

    private val db: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "system_life_rpg_db"
        ).fallbackToDestructiveMigration().build()
    }

    private val userDao by lazy { db.userDao() }
    private val questDao by lazy { db.questDao() }
    private val habitLogDao by lazy { db.habitLogDao() }
    private val activeLockDao by lazy { db.activeLockDao() }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Expose flows for reactive Jetpack Compose updates
    val userStatsFlow: Flow<UserStats?> = userDao.getUserStatsFlow()
    val allQuestsFlow: Flow<List<QuestEntity>> = questDao.getAllQuestsFlow()
    val habitLogsFlow: Flow<List<HabitLogEntity>> = habitLogDao.getAllLogsFlow()
    val activeLocksFlow: Flow<List<ActiveLockEntity>> = activeLockDao.getAllLocksFlow()

    // Initialize database default values
    suspend fun initializeGameIfNecessary() = withContext(Dispatchers.IO) {
        var stats = userDao.getUserStats()
        if (stats == null) {
            stats = UserStats(
                id = 1,
                level = 1,
                exp = 0,
                gold = 100,
                emergencyPasses = 1,
                streak = 0,
                rank = "E-Rank Hunter",
                title = "The Awakened",
                aura = "Faint Glow",
                skin = "Default Cyber",
                lastLoginDate = getTodayDateString(),
                balanceScore = 50,
                shopPenaltyPercent = 0,
                consecutiveSuccessfulMandatoryDays = 0
            )
            userDao.insertUserStats(stats)
            insertDefaultQuests()
        }
    }

    private suspend fun insertDefaultQuests() {
        val defaults = listOf(
            QuestEntity(name = "Belajar Berpikir Kritis", category = "Belajar", difficulty = "Medium", durationMinutes = 30, expReward = 60, goldReward = 30),
            QuestEntity(name = "Olahraga Pagi (Jogging/Stretching)", category = "Olahraga", difficulty = "Easy", durationMinutes = 15, expReward = 40, goldReward = 20),
            QuestEntity(name = "Membaca Buku Self-Improvement", category = "Membaca", difficulty = "Medium", durationMinutes = 20, expReward = 50, goldReward = 25),
            QuestEntity(name = "Meditasi Pernapasan", category = "Meditasi", difficulty = "Easy", durationMinutes = 10, expReward = 30, goldReward = 15),
            QuestEntity(name = "Membersihkan Meja Belajar", category = "Kebersihan", difficulty = "Easy", durationMinutes = 10, expReward = 30, goldReward = 15),
            QuestEntity(name = "Minum Air Putih 2 Liter", category = "Kesehatan", difficulty = "Easy", durationMinutes = 0, expReward = 25, goldReward = 10)
        )
        for (q in defaults) {
            questDao.insertQuest(q)
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Daily Login Rollover Logic
    suspend fun checkDailyRolloverAndLogin(): String? = withContext(Dispatchers.IO) {
        val stats = userDao.getUserStats() ?: return@withContext null
        val today = getTodayDateString()

        if (stats.lastLoginDate != today) {
            // It's a new day! Daily reward: +10 Gold, +1 Emergency Pass, and evaluate yesterday's quests
            val allQuests = questDao.getAllQuestsFlow().firstOrNull() ?: emptyList()
            val mandatoryQuests = allQuests.filter { it.isMandatory }

            var hasFailedMandatory = false
            if (mandatoryQuests.isNotEmpty()) {
                // Check if any mandatory quest from yesterday (or before today) was incomplete
                val incompleteMandatory = mandatoryQuests.filter { !it.isCompleted }
                if (incompleteMandatory.isNotEmpty()) {
                    hasFailedMandatory = true
                }
            }

            val newPenaltyPercent: Int
            val newConsecutiveSuccessDays: Int
            if (hasFailedMandatory) {
                // Penalty rises: +30% for the first fail, or +5% extra on subsequent consecutive failed days
                val currentPenalty = stats.shopPenaltyPercent
                newPenaltyPercent = if (currentPenalty == 0) 30 else currentPenalty + 5
                newConsecutiveSuccessDays = 0
            } else {
                // If there were mandatory quests and they were completed
                val hadMandatory = mandatoryQuests.isNotEmpty()
                newConsecutiveSuccessDays = if (hadMandatory) stats.consecutiveSuccessfulMandatoryDays + 1 else stats.consecutiveSuccessfulMandatoryDays
                
                // If successfully completed 3 days of mandatory quests in a row, penalties are wiped!
                newPenaltyPercent = if (newConsecutiveSuccessDays >= 3) {
                    0
                } else {
                    stats.shopPenaltyPercent
                }
            }

            // Remove non-completed custom quests to keep dashboard fresh, keep completed ones or reset
            questDao.clearNonCompletedCustomQuests()

            // Compile new Stats
            val updatedStats = stats.copy(
                gold = stats.gold + 10,
                emergencyPasses = stats.emergencyPasses + 1,
                lastLoginDate = today,
                streak = stats.streak + 1,
                shopPenaltyPercent = newPenaltyPercent,
                consecutiveSuccessfulMandatoryDays = newConsecutiveSuccessDays
            )
            userDao.updateUserStats(updatedStats)

            SoundSynthesizer.playReward()

            return@withContext if (hasFailedMandatory) {
                "SYSTEM: Hari baru dimulai! Anda GAGAL menyelesaikan Mandatory Quest kemarin. Penalti harga toko naik menjadi +$newPenaltyPercent% dan upah Gold berkurang 15%!"
            } else {
                "SYSTEM: Selamat datang kembali! Hadiah login harian (+10 Gold, +1 Emergency Pass) telah diklaim. Streak harian Anda: ${updatedStats.streak} hari!"
            }
        }
        return@withContext null
    }

    // Complete a Quest
    suspend fun completeQuest(questId: Int): CompleteQuestResult = withContext(Dispatchers.IO) {
        val quest = questDao.getQuestById(questId) ?: return@withContext CompleteQuestResult.Error("Quest tidak ditemukan")
        if (quest.isCompleted) return@withContext CompleteQuestResult.Error("Quest sudah selesai sebelumnya")

        val stats = userDao.getUserStats() ?: return@withContext CompleteQuestResult.Error("Gagal memuat profil")

        // 1. Mark quest completed
        val updatedQuest = quest.copy(isCompleted = true)
        questDao.updateQuest(updatedQuest)

        // 2. Base Rewards
        var baseGold = quest.goldReward
        val baseExp = quest.expReward

        // Apply Gold penalty if active (Gold from all quests reduced by 15%)
        if (stats.shopPenaltyPercent > 0) {
            baseGold = (baseGold * 0.85).toInt()
        }

        // 3. Lucky Reward Calculation
        val luckyPercentage = calculateLuckyRewardPercent(stats.balanceScore)
        val bonusGold = (baseGold * (luckyPercentage / 100.0)).toInt()
        val bonusExp = (baseExp * (luckyPercentage / 100.0)).toInt()

        val finalGold = baseGold + bonusGold
        val finalExp = baseExp + bonusExp

        // 4. Update Level Up logic
        var newExp = stats.exp + finalExp
        var newLevel = stats.level
        var leveledUp = false

        while (newExp >= getRequiredExp(newLevel)) {
            newExp -= getRequiredExp(newLevel)
            newLevel += 1
            leveledUp = true
        }

        // 5. Update titles, ranks, cosmetic styles based on level
        val newRank = when {
            newLevel >= 30 -> "S-Rank Sovereign"
            newLevel >= 20 -> "A-Rank Legend"
            newLevel >= 15 -> "B-Rank Grandmaster"
            newLevel >= 10 -> "C-Rank Elite Scout"
            newLevel >= 5 -> "D-Rank Challenger"
            else -> "E-Rank Hunter"
        }

        val newTitle = when {
            newLevel >= 30 -> "Sovereign of the System"
            newLevel >= 20 -> "Monarch of Shadows"
            newLevel >= 15 -> "Mana Weaver"
            newLevel >= 10 -> "Shadow Monk"
            newLevel >= 5 -> "Grit Survivor"
            else -> "The Awakened"
        }

        val newSkin = when {
            newLevel >= 30 -> "System Admin Cloak"
            newLevel >= 20 -> "Void Monarch Armor"
            newLevel >= 10 -> "Neon Shadow Assassin"
            else -> "Default Cyber"
        }

        // 6. Log habit to database
        val habitLog = HabitLogEntity(
            category = quest.category,
            durationMinutes = quest.durationMinutes
        )
        habitLogDao.insertLog(habitLog)

        // Recalculate Balance Score based on habit logs
        val allLogs = habitLogDao.getAllLogs()
        val newBalanceScore = calculateBalanceScore(allLogs)

        val newAura = when {
            newBalanceScore >= 80 -> "Calamity Void Aura (Neon Pulse)"
            newBalanceScore >= 60 -> "Sovereign Might (Crimson Spark)"
            newBalanceScore >= 40 -> "Iron Will (Azure Glow)"
            else -> "Faint Spark"
        }

        val updatedStats = stats.copy(
            level = newLevel,
            exp = newExp,
            gold = stats.gold + finalGold,
            rank = newRank,
            title = newTitle,
            skin = newSkin,
            balanceScore = newBalanceScore,
            aura = newAura
        )
        userDao.updateUserStats(updatedStats)

        // Trigger Audio Feedback
        if (leveledUp) {
            SoundSynthesizer.playLevelUp()
        } else {
            SoundSynthesizer.playQuestComplete()
        }

        return@withContext CompleteQuestResult.Success(
            questName = quest.name,
            baseGold = baseGold,
            bonusGold = bonusGold,
            baseExp = baseExp,
            bonusExp = bonusExp,
            luckyPercent = luckyPercentage,
            leveledUp = leveledUp,
            newLevel = newLevel,
            balanceScore = newBalanceScore
        )
    }

    private fun getRequiredExp(level: Int): Int {
        return level * 100
    }

    // Lucky Reward Distribution Logic
    private fun calculateLuckyRewardPercent(balanceScore: Int): Int {
        val roll = Random.nextInt(100) + 1 // 1..100
        val baseTier = when {
            roll <= 35 -> Random.nextInt(1, 6)    // 1-5%
            roll <= 60 -> Random.nextInt(6, 11)   // 6-10%
            roll <= 78 -> Random.nextInt(11, 21)  // 11-20%
            roll <= 90 -> Random.nextInt(21, 31)  // 21-30%
            roll <= 96 -> Random.nextInt(31, 41)  // 31-40%
            roll <= 99 -> Random.nextInt(41, 51)  // 41-50%
            else -> Random.nextInt(51, 61)        // 51-60%
        }

        // Semakin tinggi Balance Score: Peluang Lucky Reward sedikit meningkat (+ bonus flat up to 10%)
        val balanceBonus = if (balanceScore > 50) {
            (balanceScore - 50) / 5 // +1% up to +10%
        } else {
            0
        }

        return (baseTier + balanceBonus).coerceIn(1, 70)
    }

    // Calculate Balance Score
    private fun calculateBalanceScore(logs: List<HabitLogEntity>): Int {
        if (logs.isEmpty()) return 50
        val categories = listOf("Belajar", "Olahraga", "Membaca", "Meditasi", "Kebersihan", "Kesehatan")
        val uniqueCompletedCount = categories.count { cat -> logs.any { it.category == cat } }

        // Base reward for covering different categories: 10 points each (up to 60)
        val baseScore = uniqueCompletedCount * 10
        // Total completions add 2 points each (capped at 40 points)
        val activityPoints = (logs.size * 2).coerceAtMost(40)

        return (baseScore + activityPoints).coerceIn(10, 100)
    }

    // Insert Custom Quest
    suspend fun createCustomQuest(
        name: String,
        category: String,
        difficulty: String,
        duration: Int
    ) = withContext(Dispatchers.IO) {
        val expReward = when (difficulty) {
            "Easy" -> 30
            "Medium" -> 60
            "Hard" -> 100
            else -> 150
        }
        val goldReward = when (difficulty) {
            "Easy" -> 15
            "Medium" -> 30
            "Hard" -> 50
            else -> 75
        }

        val quest = QuestEntity(
            name = name,
            category = category,
            difficulty = difficulty,
            durationMinutes = duration,
            expReward = expReward,
            goldReward = goldReward
        )
        questDao.insertQuest(quest)
        SoundSynthesizer.playClick()
    }

    // Delete Quest
    suspend fun deleteQuest(id: Int) = withContext(Dispatchers.IO) {
        questDao.deleteQuestById(id)
    }

    // Shop Purchase - Entertainment Unlock
    suspend fun purchaseShopItem(
        appName: String,
        minutes: Int,
        baseCostGold: Int
    ): ShopPurchaseResult = withContext(Dispatchers.IO) {
        val stats = userDao.getUserStats() ?: return@withContext ShopPurchaseResult.Error("Profil tidak ditemukan")

        // Penalty price multiplier: 1.0 + penaltyPercent / 100
        val multiplier = 1.0 + (stats.shopPenaltyPercent / 100.0)
        val finalCost = (baseCostGold * multiplier).toInt()

        if (stats.gold < finalCost) {
            SoundSynthesizer.playWarning()
            return@withContext ShopPurchaseResult.Error("Emas tidak cukup! Membutuhkan $finalCost Emas.")
        }

        // Subtract Gold
        val updatedStats = stats.copy(gold = stats.gold - finalCost)
        userDao.updateUserStats(updatedStats)

        // Apply app lock exemption time
        val currentLock = activeLockDao.getLockForApp(appName)
        val now = System.currentTimeMillis()
        val currentExpiration = if (currentLock != null && currentLock.expiresAt > now) {
            currentLock.expiresAt
        } else {
            now
        }

        val addedMs = minutes * 60 * 1000L
        val newExpiration = currentExpiration + addedMs

        activeLockDao.insertLock(ActiveLockEntity(appName = appName, expiresAt = newExpiration))
        SoundSynthesizer.playReward()

        return@withContext ShopPurchaseResult.Success(
            appName = appName,
            unlockedMinutes = minutes,
            costPaid = finalCost,
            expiresAt = newExpiration
        )
    }

    // Use Emergency Pass
    suspend fun useEmergencyPass(appName: String): ShopPurchaseResult = withContext(Dispatchers.IO) {
        val stats = userDao.getUserStats() ?: return@withContext ShopPurchaseResult.Error("Profil tidak ditemukan")
        if (stats.emergencyPasses <= 0) {
            SoundSynthesizer.playWarning()
            return@withContext ShopPurchaseResult.Error("Anda tidak memiliki Emergency Pass!")
        }

        // Subtract Pass
        val updatedStats = stats.copy(emergencyPasses = stats.emergencyPasses - 1)
        userDao.updateUserStats(updatedStats)

        // Apply 5 minutes lock exception
        val currentLock = activeLockDao.getLockForApp(appName)
        val now = System.currentTimeMillis()
        val currentExpiration = if (currentLock != null && currentLock.expiresAt > now) {
            currentLock.expiresAt
        } else {
            now
        }

        val addedMs = 5 * 60 * 1000L // 5 minutes
        val newExpiration = currentExpiration + addedMs

        activeLockDao.insertLock(ActiveLockEntity(appName = appName, expiresAt = newExpiration))
        SoundSynthesizer.playQuestComplete()

        return@withContext ShopPurchaseResult.Success(
            appName = appName,
            unlockedMinutes = 5,
            costPaid = 0,
            expiresAt = newExpiration
        )
    }

    // Get Locked Apps default list
    fun getDefaultLockedApps(): List<String> {
        return listOf("Mobile Legends", "Free Fire", "TikTok", "YouTube", "Chess.com", "ANIMEIN", "Wibuku")
    }

    // Adaptive AI System: Evaluate habits and generate Mandatory Quests using Gemini API
    suspend fun runAdaptiveAIEvaluation(): AIEvaluationResult = withContext(Dispatchers.IO) {
        val stats = userDao.getUserStats() ?: return@withContext AIEvaluationResult.Error("Profil tidak ditemukan")
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY

        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isEmpty()) {
            // No valid API key provided or placeholder, simulate the AI result locally!
            return@withContext runLocalAdaptiveAIEvaluation()
        }

        try {
            val logs = habitLogDao.getAllLogs()
            
            // Build historical breakdown
            val categories = listOf("Belajar", "Olahraga", "Membaca", "Meditasi", "Kebersihan", "Kesehatan")
            val counts = categories.associateWith { cat -> logs.count { it.category == cat } }
            
            val totalQuests = logs.size
            val description = counts.entries.joinToString(", ") { "${it.key}: ${it.value} kali" }

            val prompt = """
                Anda adalah Adaptive AI System pendukung kedisiplinan hidup dari aplikasi gamifikasi RPG harian.
                Misi Anda adalah menganalisis statistik produktivitas pengguna berikut dan membuat Mandatory Quests yang mengatasi kelemahan terdalam mereka.
                
                Statistik Pengguna saat ini:
                - Level Pengguna: ${stats.level}
                - Total Quests Selesai: $totalQuests
                - Rincian Aktivitas: $description
                
                Tentukan SATU atau DUA kategori yang paling jarang dilakukan (terlemah) di antara kategori ini: Belajar, Olahraga, Membaca, Meditasi, Kebersihan, Kesehatan.
                Lalu buatlah 1 sampai 3 Mandatory Quest harian (tidak boleh lebih dari 3) spesifik untuk kategori lemah tersebut.
                Setiap quest harus memiliki batas waktu (deadlineTime) di sore/malam hari dengan format "HH:mm" (misalnya "17:30" atau "20:00").
                Tingkat kesulitan harus disesuaikan: Easy, Medium, atau Hard.
                
                Keluaran harus murni berupa ARRAY JSON valid tanpa pembungkus markdown backticks ```json atau kalimat pembuka/penutup lainnya. Format JSON persis seperti berikut:
                [
                  {
                    "name": "Olahraga Kardio Ringan",
                    "category": "Olahraga",
                    "difficulty": "Easy",
                    "durationMinutes": 15,
                    "expReward": 40,
                    "goldReward": 20,
                    "deadlineTime": "17:30"
                  }
                ]
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )

            val response = GeminiClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext AIEvaluationResult.Error("Sistem AI mengembalikan respons kosong.")

            val cleanedJson = extractJsonArray(jsonText)
            
            val listType = Types.newParameterizedType(List::class.java, GeneratedQuest::class.java)
            val adapter = moshi.adapter<List<GeneratedQuest>>(listType)
            val generatedList = adapter.fromJson(cleanedJson)

            if (generatedList.isNullOrEmpty()) {
                return@withContext AIEvaluationResult.Error("Gagal menguraikan Mandatory Quest dari AI.")
            }

            // Insert generated quests as Mandatory
            for (q in generatedList) {
                val entity = QuestEntity(
                    name = q.name,
                    category = q.category,
                    difficulty = q.difficulty,
                    durationMinutes = q.durationMinutes,
                    expReward = q.expReward,
                    goldReward = q.goldReward,
                    isMandatory = true,
                    deadlineTime = q.deadlineTime
                )
                questDao.insertQuest(entity)
            }

            SoundSynthesizer.playReward()
            return@withContext AIEvaluationResult.Success(generatedList, "Sistem AI berhasil menganalisis statistik Anda dan merumuskan Mandatory Quest baru!")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("GameRepository", "AI Evaluation Failed: ${e.message}")
            return@withContext runLocalAdaptiveAIEvaluation() // Fallback to local evaluation gracefully
        }
    }

    // Local Rule-based Evaluation when API is not available or fails
    private suspend fun runLocalAdaptiveAIEvaluation(): AIEvaluationResult {
        val logs = habitLogDao.getAllLogs()
        val categories = listOf("Belajar", "Olahraga", "Membaca", "Meditasi", "Kebersihan", "Kesehatan")
        val counts = categories.associateWith { cat -> logs.count { it.category == cat } }
        
        // Find categories with the minimum counts
        val weakest = counts.entries.minByOrNull { it.value }?.key ?: "Olahraga"

        val mockQuests = when (weakest) {
            "Olahraga" -> listOf(
                GeneratedQuest("Olahraga Sore (Mandatory)", "Olahraga", "Medium", 30, 80, 40, "17:30"),
                GeneratedQuest("Minum Air Tambahan (Mandatory)", "Kesehatan", "Easy", 0, 30, 15, "19:00")
            )
            "Belajar" -> listOf(
                GeneratedQuest("Fokus Belajar / Koding (Mandatory)", "Belajar", "Hard", 45, 120, 60, "20:00")
            )
            "Membaca" -> listOf(
                GeneratedQuest("Membaca Artikel Bermanfaat (Mandatory)", "Membaca", "Easy", 15, 40, 20, "18:00")
            )
            "Meditasi" -> listOf(
                GeneratedQuest("Meditasi Ketenangan Malam (Mandatory)", "Meditasi", "Easy", 10, 30, 15, "21:30")
            )
            "Kebersihan" -> listOf(
                GeneratedQuest("Merapikan Kamar Tidur (Mandatory)", "Kebersihan", "Easy", 15, 35, 15, "16:00")
            )
            else -> listOf(
                GeneratedQuest("Istirahat Tepat Waktu (Mandatory)", "Kesehatan", "Medium", 0, 60, 30, "22:30")
            )
        }

        for (q in mockQuests) {
            val entity = QuestEntity(
                name = q.name,
                category = q.category,
                difficulty = q.difficulty,
                durationMinutes = q.durationMinutes,
                expReward = q.expReward,
                goldReward = q.goldReward,
                isMandatory = true,
                deadlineTime = q.deadlineTime
            )
            questDao.insertQuest(entity)
        }

        SoundSynthesizer.playQuestComplete()
        return AIEvaluationResult.Success(mockQuests, "Sistem AI Lokal mendeteksi kelemahan Anda di bidang '$weakest' dan merumuskan Mandatory Quest harian!")
    }

    private fun extractJsonArray(rawText: String): String {
        val startIndex = rawText.indexOf('[')
        val endIndex = rawText.lastIndexOf(']')
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return rawText.substring(startIndex, endIndex + 1)
        }
        return rawText
    }
}

// Result models for repository transactions
sealed class CompleteQuestResult {
    data class Success(
        val questName: String,
        val baseGold: Int,
        val bonusGold: Int,
        val baseExp: Int,
        val bonusExp: Int,
        val luckyPercent: Int,
        val leveledUp: Boolean,
        val newLevel: Int,
        val balanceScore: Int
    ) : CompleteQuestResult()
    data class Error(val message: String) : CompleteQuestResult()
}

sealed class ShopPurchaseResult {
    data class Success(
        val appName: String,
        val unlockedMinutes: Int,
        val costPaid: Int,
        val expiresAt: Long
    ) : ShopPurchaseResult()
    data class Error(val message: String) : ShopPurchaseResult()
}

sealed class AIEvaluationResult {
    data class Success(val quests: List<GeneratedQuest>, val message: String) : AIEvaluationResult()
    data class Error(val message: String) : AIEvaluationResult()
}

@JsonClass(generateAdapter = true)
data class GeneratedQuest(
    val name: String,
    val category: String,
    val difficulty: String,
    val durationMinutes: Int,
    val expReward: Int,
    val goldReward: Int,
    val deadlineTime: String? = null
)
