package com.sylvester.rustsensei.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val quickPrompts = listOf(
    "Explain ownership vs borrowing",
    "How do I handle errors in Rust?",
    "&str vs String \u2014 when to use which?",
    "Help me understand lifetimes"
)

@Composable
fun QuickPromptChips(
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        quickPrompts.forEach { prompt ->
            SuggestionChip(
                onClick = { onPromptSelected(prompt) },
                label = {
                    Text(
                        text = buildAnnotatedString {
                            // Terminal ">" prefix in primary color, monospace
                            withStyle(SpanStyle(
                                color = primary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )) {
                                append("> ")
                            }
                            // Prompt text in default font for readability
                            withStyle(SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )) {
                                append(prompt)
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = primary.copy(alpha = 0.15f)
                )
            )
        }
    }
}
