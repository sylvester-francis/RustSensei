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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp)
        ) {
            if (isUser) {
                Text(
                    text = content,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 15.sp
                )
            } else {
                MarkdownContent(content)
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
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
                            background = Color(0xFF2D2D2D),
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
