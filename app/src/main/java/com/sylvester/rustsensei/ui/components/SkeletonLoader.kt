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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SkeletonBox(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh

    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            surfaceContainer,
            surfaceContainerHigh,
            surfaceContainer
        ),
        start = Offset(shimmerOffset * 300f, 0f),
        end = Offset(shimmerOffset * 300f + 300f, 0f)
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(shimmerBrush)
    )
}
