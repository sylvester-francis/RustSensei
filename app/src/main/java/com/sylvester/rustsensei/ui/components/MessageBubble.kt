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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

@Composable
fun MessageBubble(
    content: String,
    isUser: Boolean,
    isFirstInGroup: Boolean = true,
    modifier: Modifier = Modifier
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val primary = MaterialTheme.colorScheme.primary

    if (isUser) {
        // User message: right-aligned pill with neon glow shadow
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = screenWidth * 0.88f)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = primary.copy(alpha = 0.3f),
                        ambientColor = primary.copy(alpha = 0.15f)
                    )
                    .background(
                        color = primary,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Text(
                    text = content,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    } else {
        // Assistant message: full-width, no background, neon accent line on left
        val accentColor = primary.copy(alpha = 0.25f)
        val showBorder = isFirstInGroup

        Column(
            modifier = modifier
                .fillMaxWidth()
                .then(
                    if (showBorder) {
                        Modifier.drawBehind {
                            drawRect(
                                color = accentColor,
                                topLeft = Offset.Zero,
                                size = androidx.compose.ui.geometry.Size(
                                    width = 3.dp.toPx(),
                                    height = size.height
                                )
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(
                    start = if (showBorder) 12.dp else 4.dp,
                    end = 4.dp,
                    top = 2.dp,
                    bottom = 2.dp
                )
        ) {
            MarkdownContent(content)
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
                    if (index > 0) Spacer(modifier = Modifier.height(12.dp))
                    CodeBlock(code = block.code, language = block.language)
                    if (index < blocks.lastIndex) Spacer(modifier = Modifier.height(12.dp))
                }
                is MarkdownBlock.TextBlock -> {
                    Text(
                        text = parseInlineMarkdown(block.text),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        lineHeight = 23.sp
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
                // Inline code `text` — neon terminal style
                str[i] == '`' -> {
                    val end = str.indexOf('`', i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0xFF141820),
                            color = Color(0xFFCE412B),
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
