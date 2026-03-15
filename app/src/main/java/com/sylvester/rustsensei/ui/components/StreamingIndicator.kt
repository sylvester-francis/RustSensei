package com.sylvester.rustsensei.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun StreamingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scanner")
    val primary = MaterialTheme.colorScheme.primary

    // Animated width fraction: oscillates between 0 and 1
    val widthFraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scannerWidth"
    )

    // Alpha pulse for extra neon feel
    val alpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scannerAlpha"
    )

    // Map fraction to width: 20dp to 60dp
    val barWidth = (20 + (40 * widthFraction)).dp

    Box(
        modifier = modifier
            .padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(primary.copy(alpha = alpha))
        )
    }
}
