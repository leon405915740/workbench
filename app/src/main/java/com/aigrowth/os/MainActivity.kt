package com.aigrowth.os

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aigrowth.os.ui.onboarding.isOnboardingComplete
import com.aigrowth.os.ui.onboarding.OnboardingScreen
import com.aigrowth.os.ui.splash.SplashScreen
import com.aigrowth.os.ui.theme.AIGrowthOSTheme
import com.accounting.app.log.AppLogger
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIGrowthOSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    val showOnboarding = remember {
                        mutableStateOf(!isOnboardingComplete(this))
                    }

                    when {
                        showSplash -> {
                            SplashScreen(
                                onTimeout = {
                                    val requestId = AppLogger.generateRequestId()
                                    AppLogger.i(requestId, "MainActivity", "splashTimeout 完成")
                                    showSplash = false
                                },
                                onSkip = {
                                    val requestId = AppLogger.generateRequestId()
                                    AppLogger.i(requestId, "MainActivity", "splashSkipped 完成")
                                    showSplash = false
                                }
                            )
                        }
                        showOnboarding.value -> {
                            OnboardingScreen(
                                onComplete = {
                                    showOnboarding.value = false
                                }
                            )
                        }
                        else -> {
                            AIGrowthOSApp()
                        }
                    }
                }
            }
        }
    }
}
