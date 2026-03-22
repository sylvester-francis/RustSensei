package com.sylvester.rustsensei

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.sylvester.rustsensei.data.PreferencesManager
import com.sylvester.rustsensei.data.ThemePreference
import com.sylvester.rustsensei.ui.theme.RustSenseiTheme
import com.sylvester.rustsensei.work.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var reminderScheduler: ReminderScheduler

    val themePreference = MutableStateFlow(ThemePreference.SYSTEM)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themePreference.value = ThemePreference.fromString(preferencesManager.getThemePreference())
        enableEdgeToEdge()
        setContent {
            val themePref by themePreference.collectAsState()
            val darkTheme = when (themePref) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
            }

            RustSenseiTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RustSenseiApp(
                        preferencesManager = preferencesManager,
                        reminderScheduler = reminderScheduler
                    )
                }
            }
        }
    }

    fun updateThemePreference(preference: ThemePreference) {
        themePreference.value = preference
        preferencesManager.setThemePreference(preference.name)
    }
}
