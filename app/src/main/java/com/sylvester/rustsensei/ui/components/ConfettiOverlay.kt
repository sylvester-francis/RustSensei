package com.sylvester.rustsensei.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import com.sylvester.rustsensei.ui.theme.AppColors

private data class Particle(
    val angle: Float,
    val speed: Float,
    val color: Color,
    val size: Float
)

@Composable
fun ConfettiOverlay(
    isVisible: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val colors = listOf(AppColors.current.accent, AppColors.current.cyan, AppColors.current.amber, AppColors.current.success)

    val particles = remember {
        List(25) {
            Particle(
                angle = Random.nextFloat() * 360f,
                speed = 200f + Random.nextFloat() * 400f,
                color = colors[Random.nextInt(colors.size)],
                size = 4f + Random.nextFloat() * 6f
            )
        }
    }

    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(isVisible) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 2000,
                easing = LinearEasing
            )
        )
        onComplete()
    }

    val progress = animationProgress.value

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        particles.forEach { particle ->
            val angleRad = Math.toRadians(particle.angle.toDouble())
            // Particles decelerate over time
            val distance = particle.speed * progress * (1f - progress * 0.3f)
            val x = centerX + (cos(angleRad) * distance).toFloat()
            val y = centerY + (sin(angleRad) * distance).toFloat()

            // Fade out in the second half of the animation
            val alpha = if (progress < 0.5f) 1f else (1f - (progress - 0.5f) * 2f).coerceIn(0f, 1f)

            // Shrink particles as they fade
            val currentSize = particle.size * (1f - progress * 0.5f)

            drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = currentSize,
                center = Offset(x, y)
            )
        }
    }
}
