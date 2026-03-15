package com.sylvester.rustsensei.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.data.LearningStats

@Composable
fun ActivityChart(
    stats: List<LearningStats>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier.fillMaxSize()) {
        if (stats.isEmpty()) {
            // Draw empty state
            drawRect(
                color = surfaceVariantColor.copy(alpha = 0.3f),
                topLeft = Offset.Zero,
                size = size
            )
            return@Canvas
        }

        val sortedStats = stats.sortedBy { it.date }.takeLast(7)
        val barCount = sortedStats.size
        if (barCount == 0) return@Canvas

        val padding = 40f
        val bottomPadding = 60f
        val chartWidth = size.width - padding * 2
        val chartHeight = size.height - padding - bottomPadding
        val barWidth = (chartWidth / barCount) * 0.6f
        val barSpacing = chartWidth / barCount

        // Find max value for scaling
        val maxValue = sortedStats.maxOf {
            (it.sectionsRead + it.exercisesCompleted).coerceAtLeast(1)
        }.toFloat()

        sortedStats.forEachIndexed { index, stat ->
            val x = padding + index * barSpacing + (barSpacing - barWidth) / 2

            // Sections bar
            val sectionsHeight = (stat.sectionsRead / maxValue) * chartHeight
            drawRect(
                color = primaryColor,
                topLeft = Offset(x, padding + chartHeight - sectionsHeight),
                size = Size(barWidth / 2, sectionsHeight)
            )

            // Exercises bar
            val exercisesHeight = (stat.exercisesCompleted / maxValue) * chartHeight
            drawRect(
                color = tertiaryColor,
                topLeft = Offset(x + barWidth / 2, padding + chartHeight - exercisesHeight),
                size = Size(barWidth / 2, exercisesHeight)
            )

            // Date label
            val dateLabel = stat.date.takeLast(5) // MM-DD
            drawContext.canvas.nativeCanvas.drawText(
                dateLabel,
                x + barWidth / 2 - 15f,
                size.height - 10f,
                android.graphics.Paint().apply {
                    color = onSurfaceColor.hashCode()
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}
