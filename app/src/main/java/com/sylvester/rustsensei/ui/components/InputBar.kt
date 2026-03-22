package com.sylvester.rustsensei.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.ui.theme.DarkSurfaceContainer
import com.sylvester.rustsensei.ui.theme.PrimaryGlow
import com.sylvester.rustsensei.ui.theme.AppColors

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
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val pillShape = RoundedCornerShape(28.dp)
    val borderWidth = if (isFocused) 2.dp else 1.dp
    val borderColor = if (isFocused) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Column(modifier = modifier.fillMaxWidth()) {
        // Neon top divider: 1dp primary at 15% alpha
        HorizontalDivider(
            thickness = 1.dp,
            color = primary.copy(alpha = 0.15f)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp)
                        .border(
                            width = borderWidth,
                            color = borderColor,
                            shape = pillShape
                        ),
                    placeholder = {
                        // Terminal-style prompt: ">" in primary monospace, then hint text
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = primary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 15.sp
                                    )
                                ) {
                                    append("> ")
                                }
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                            .copy(alpha = 0.5f),
                                        fontSize = 15.sp
                                    )
                                ) {
                                    append("Ask about Rust...")
                                }
                            }
                        )
                    },
                    shape = pillShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceContainer,
                        unfocusedContainerColor = DarkSurfaceContainer,
                        disabledContainerColor = DarkSurfaceContainer,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 6,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Send / stop button: 48dp filled circle (M3 min touch target)
                FilledIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isGenerating) onStop() else onSend()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = if (isGenerating)
                                AppColors.current.error.copy(alpha = 0.4f)
                            else
                                PrimaryGlow,
                            ambientColor = if (isGenerating)
                                AppColors.current.error.copy(alpha = 0.2f)
                            else
                                PrimaryGlow.copy(alpha = 0.4f)
                        ),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isGenerating)
                            AppColors.current.error
                        else
                            primary
                    )
                ) {
                    if (isGenerating) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop generation",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
