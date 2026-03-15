package com.sylvester.rustsensei.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MessageBubble(
    content: String,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxBubbleWidth = screenWidth * 0.85f

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Assistant avatar
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (isUser) {
            // User bubble with gradient
            val primaryColor = MaterialTheme.colorScheme.primary
            val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
            Box(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 6.dp
                        )
                    )
                    .background(
                        Brush.linearGradient(
                            colors = listOf(primaryColor, primaryContainerColor),
                            start = Offset.Zero,
                            end = Offset.Infinite
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = content,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp
                )
            }
        } else {
            // Assistant bubble with left accent border
            val accentColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .clip(
                        RoundedCornerShape(
                            topStart = 6.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 20.dp
                        )
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .drawBehind {
                        drawRect(
                            color = accentColor,
                            topLeft = Offset.Zero,
                            size = androidx.compose.ui.geometry.Size(
                                width = 3.dp.toPx(),
                                height = size.height
                            )
                        )
                    }
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp)
            ) {
                MarkdownContent(content)
            }
        }

        // User avatar
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun MarkdownContent(text: String) {
    val blocks = parseMarkdownBlocks(text)

    Column {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                    CodeBlock(code = block.code, language = block.language)
                    if (index < blocks.lastIndex) Spacer(modifier = Modifier.height(8.dp))
                }
                is MarkdownBlock.TextBlock -> {
                    Text(
                        text = parseInlineMarkdown(block.text),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}

sealed class MarkdownBlock {
    data class TextBlock(val text: String) : MarkdownBlock()
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock()
}

fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val codeBlockRegex = Regex("```(\\w*)\\n([\\s\\S]*?)```")

    var lastIndex = 0
    codeBlockRegex.findAll(text).forEach { match ->
        // Text before code block
        if (match.range.first > lastIndex) {
            val textBefore = text.substring(lastIndex, match.range.first).trim()
            if (textBefore.isNotEmpty()) {
                blocks.add(MarkdownBlock.TextBlock(textBefore))
            }
        }

        val language = match.groupValues[1].ifEmpty { "text" }
        val code = match.groupValues[2].trimEnd()
        blocks.add(MarkdownBlock.CodeBlock(code, language))

        lastIndex = match.range.last + 1
    }

    // Remaining text after last code block
    if (lastIndex < text.length) {
        val remaining = text.substring(lastIndex).trim()
        if (remaining.isNotEmpty()) {
            blocks.add(MarkdownBlock.TextBlock(remaining))
        }
    }

    if (blocks.isEmpty() && text.isNotBlank()) {
        blocks.add(MarkdownBlock.TextBlock(text))
    }

    return blocks
}

fun parseInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val str = text

        while (i < str.length) {
            when {
                // Bold **text**
                i + 1 < str.length && str[i] == '*' && str[i + 1] == '*' -> {
                    val end = str.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(str.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(str[i])
                        i++
                    }
                }
                // Italic *text*
                str[i] == '*' && (i == 0 || str[i - 1] != '*') -> {
                    val end = str.indexOf('*', i + 1)
                    if (end != -1 && (end + 1 >= str.length || str[end + 1] != '*')) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(str.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(str[i])
                        i++
                    }
                }
                // Inline code `text`
                str[i] == '`' -> {
                    val end = str.indexOf('`', i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0xFF21262D),
                            color = Color(0xFFF0883E),
                            fontSize = 14.sp
                        )) {
                            append(" ${str.substring(i + 1, end)} ")
                        }
                        i = end + 1
                    } else {
                        append(str[i])
                        i++
                    }
                }
                // Bullet points
                (str[i] == '-' || str[i] == '*') && i > 0 && str[i - 1] == '\n'
                        && i + 1 < str.length && str[i + 1] == ' ' -> {
                    append("  \u2022 ")
                    i += 2
                }
                else -> {
                    append(str[i])
                    i++
                }
            }
        }
    }
}
