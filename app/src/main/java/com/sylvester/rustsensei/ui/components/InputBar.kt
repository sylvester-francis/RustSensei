package com.sylvester.rustsensei.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        // Neon top border — 1dp primary at 15% alpha
        HorizontalDivider(
            thickness = 1.dp,
            color = primary.copy(alpha = 0.15f)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .padding(bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        // Terminal-style prompt: ">" in primary, then hint text
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(
                                    color = primary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 15.sp
                                )) {
                                    append("> ")
                                }
                                withStyle(SpanStyle(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 15.sp
                                )) {
                                    append("Enter command...")
                                }
                            }
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send / stop button with neon glow
                FilledIconButton(
                    onClick = if (isGenerating) onStop else onSend,
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = if (isGenerating)
                                MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                            else
                                primary.copy(alpha = 0.4f),
                            ambientColor = if (isGenerating)
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            else
                                primary.copy(alpha = 0.2f)
                        ),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isGenerating)
                            MaterialTheme.colorScheme.error
                        else
                            primary
                    )
                ) {
                    if (isGenerating) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop generation",
                            tint = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
