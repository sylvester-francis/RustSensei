package com.sylvester.rustsensei.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 96.dp,
    thickness: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    centerText: String? = null,
    label: String? = null
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(
        modifier = modifier.semantics {
            contentDescription = buildString {
                append("Progress: ${(clampedProgress * 100).toInt()}%")
                if (label != null) append(", $label")
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(diameter)
        ) {
            Canvas(modifier = Modifier.size(diameter)) {
                val strokeWidth = thickness.toPx()
                val arcSize = size.minDimension - strokeWidth

                // Track (background circle)
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(
                        (size.width - arcSize) / 2f,
                        (size.height - arcSize) / 2f
                    ),
                    size = androidx.compose.ui.geometry.Size(arcSize, arcSize)
                )

                // Fill arc (progress)
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * clampedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(
                        (size.width - arcSize) / 2f,
                        (size.height - arcSize) / 2f
                    ),
                    size = androidx.compose.ui.geometry.Size(arcSize, arcSize)
                )
            }

            if (centerText != null) {
                Text(
                    text = centerText,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
