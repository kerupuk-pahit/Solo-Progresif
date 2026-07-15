package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.data.repository.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AppLockAccessibilityService : AccessibilityService() {

    private val repository by lazy { GameRepository(applicationContext) }
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Map common target store/play store app packages to their app locks list
            val matchedApp = when {
                packageName.contains("mobile.legends", ignoreCase = true) -> "Mobile Legends"
                packageName.contains("freefire", ignoreCase = true) -> "Free Fire"
                packageName.contains("tiktok", ignoreCase = true) || packageName.contains("zhiliaoapp", ignoreCase = true) -> "TikTok"
                packageName.contains("youtube", ignoreCase = true) -> "YouTube"
                packageName.contains("chess", ignoreCase = true) -> "Chess.com"
                packageName.contains("animein", ignoreCase = true) -> "ANIMEIN"
                packageName.contains("wibuku", ignoreCase = true) -> "Wibuku"
                else -> null
            }

            if (matchedApp != null) {
                serviceScope.launch {
                    val activeLocks = repository.activeLocksFlow.firstOrNull() ?: emptyList()
                    val matchingLock = activeLocks.find { it.appName == matchedApp }
                    val now = System.currentTimeMillis()

                    if (matchingLock == null || matchingLock.expiresAt <= now) {
                        // Access limit exceeded! Intercept and launch SYSTEM LOCK SCREEN
                        val intent = Intent(applicationContext, com.example.MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra("TRIGGER_LOCK_APP", matchedApp)
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {}
}
