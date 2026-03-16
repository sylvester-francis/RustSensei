package com.sylvester.rustsensei.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StreamingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "streaming")
    val primary = MaterialTheme.colorScheme.primary

    // Sequential pulse animation: scale 1.0 -> 1.4 -> 1.0, 600ms duration,
    // 150ms stagger between dots
    val scales = List(3) { dotIndex ->
        val delayMs = dotIndex * 150

        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 600 + delayMs
                    1f at 0 + delayMs
                    1.4f at 150 + delayMs
                    1f at 300 + delayMs
                    1f at 600 + delayMs
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "dot_scale_$dotIndex"
        )
        scale
    }

    Row(
        modifier = modifier
            .padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            .semantics { contentDescription = "Generating response" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        scales.forEach { scale ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(primary)
            )
        }

        Text(
            text = "thinking",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
