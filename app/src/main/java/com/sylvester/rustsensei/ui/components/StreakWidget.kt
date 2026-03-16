package com.sylvester.rustsensei.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

data class DayActivity(
    val label: String,
    val level: Int
)

@Composable
fun StreakWidget(
    streakDays: Int,
    weekActivity: List<DayActivity>,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "streak_flame")
    val flameScale by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_scale"
    )

    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier.semantics {
            contentDescription = "$streakDays day streak"
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Flame icon with animated scale
        Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = null,
            tint = primary,
            modifier = Modifier
                .size(40.dp)
                .scale(flameScale)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Day count
        Text(
            text = "$streakDays",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = if (streakDays == 1) "day streak" else "day streak",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Weekly activity dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            weekActivity.forEachIndexed { index, day ->
                WeekDot(
                    day = day,
                    isToday = index == weekActivity.lastIndex,
                    primary = primary,
                    surfaceVariant = surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeekDot(
    day: DayActivity,
    isToday: Boolean,
    primary: androidx.compose.ui.graphics.Color,
    surfaceVariant: androidx.compose.ui.graphics.Color
) {
    val dotSize = 10.dp

    when {
        // Today: pulsing outline
        isToday && day.level == 0 -> {
            val transition = rememberInfiniteTransition(label = "today_pulse")
            val pulseAlpha by transition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_alpha"
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = primary.copy(alpha = pulseAlpha),
                        shape = CircleShape
                    )
            )
        }
        // Today completed
        isToday && day.level > 0 -> {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(primary)
            )
        }
        // Completed past day: filled primary
        day.level > 0 -> {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(primary)
            )
        }
        // Missed past day: surfaceVariant
        else -> {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(surfaceVariant)
            )
        }
    }
}
