package com.sylvester.rustsensei.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.sylvester.rustsensei.ui.theme.DarkSurfaceContainerHigh

// User bubble background: primaryContainer dark variant
private val UserBubbleBackground = Color(0xFF2A1510)

// Bubble corner radii — M3 large shape (16dp) with tail corner at 4dp
private val UserBubbleShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 18.dp,
    bottomEnd = 4.dp,
    bottomStart = 18.dp
)

private val AiBubbleShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 18.dp,
    bottomEnd = 18.dp,
    bottomStart = 4.dp
)

@Composable
fun MessageBubble(
    content: String,
    isUser: Boolean,
    isFirstInGroup: Boolean = true,
    modifier: Modifier = Modifier
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    // Entrance animation: slide in from the sender's side with a spring
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val slideDirection = if (isUser) 1 else -1
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { slideDirection * (it / 4) },
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
        ) + fadeIn(animationSpec = spring(stiffness = 400f))
    ) {
    if (isUser) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .semantics { contentDescription = "You said: $content" },
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = screenWidth * 0.82f)
                    .clip(UserBubbleShape)
                    .background(UserBubbleBackground)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = content,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    } else {
        // Left-aligned AI message
        Row(
            modifier = modifier
                .fillMaxWidth()
                .semantics { contentDescription = "RustSensei said: $content" },
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // Crab emoji indicator for first message in a group
            if (isFirstInGroup) {
                Text(
                    text = "\uD83E\uDD80",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(end = 6.dp, top = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .widthIn(max = screenWidth * 0.88f)
                    .clip(AiBubbleShape)
                    .background(DarkSurfaceContainerHigh)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    RichContent(content)
                }
            }
        }
    }
    } // end AnimatedVisibility
}

/**
 * Renders LLM output as seamless rich text.
 * Code blocks get the CodeBlock component.
 * Everything else is rendered as a single flowing AnnotatedString
 * with bold, italic, inline code, and bullets handled natively.
 * No raw markdown characters are visible.
 */
@Composable
fun RichContent(text: String) {
    val blocks = splitCodeBlocks(text)

    blocks.forEachIndexed { index, block ->
        when (block) {
            is ContentBlock.Code -> {
                if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                CodeBlock(code = block.code, language = block.language)
                if (index < blocks.lastIndex) Spacer(modifier = Modifier.height(10.dp))
            }
            is ContentBlock.Prose -> {
                Text(
                    text = renderInline(block.text),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    lineHeight = 23.sp
                )
            }
        }
    }
}

// Keep these for backward compat (BookContentRenderer uses them)
@Composable
fun MarkdownContent(text: String) = RichContent(text)

private sealed class ContentBlock {
    data class Prose(val text: String) : ContentBlock()
    data class Code(val code: String, val language: String) : ContentBlock()
}

/**
 * Splits text into prose and fenced code blocks.
 * Only ``` fences are treated as code -- everything else stays as prose.
 */
private fun splitCodeBlocks(text: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    val regex = Regex("```(\\w*)\\n([\\s\\S]*?)```")

    var cursor = 0
    regex.findAll(text).forEach { match ->
        if (match.range.first > cursor) {
            val prose = text.substring(cursor, match.range.first).trim()
            if (prose.isNotEmpty()) blocks.add(ContentBlock.Prose(prose))
        }
        val lang = match.groupValues[1].ifEmpty { "text" }
        blocks.add(ContentBlock.Code(match.groupValues[2].trimEnd(), lang))
        cursor = match.range.last + 1
    }
    if (cursor < text.length) {
        val remaining = text.substring(cursor).trim()
        if (remaining.isNotEmpty()) blocks.add(ContentBlock.Prose(remaining))
    }
    if (blocks.isEmpty() && text.isNotBlank()) blocks.add(ContentBlock.Prose(text))
    return blocks
}

/**
 * Renders inline markdown as an AnnotatedString.
 * Handles: **bold**, *italic*, `inline code`, ### headings, - bullets.
 * All markdown syntax characters are consumed -- none leak to the user.
 */
private fun renderInline(text: String): AnnotatedString {
    // Pre-process: convert markdown headings to bold lines
    val processed = text
        .replace(Regex("^###\\s+(.+)", RegexOption.MULTILINE), "$1")
        .replace(Regex("^##\\s+(.+)", RegexOption.MULTILINE), "$1")
        .replace(Regex("^#\\s+(.+)", RegexOption.MULTILINE), "$1")
        .replace(Regex("^[-*]\\s+", RegexOption.MULTILINE), "  \u2022 ")

    return buildAnnotatedString {
        var i = 0
        val s = processed

        while (i < s.length) {
            when {
                // Bold **text**
                i + 1 < s.length && s[i] == '*' && s[i + 1] == '*' -> {
                    val end = s.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(s.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(s[i]); i++
                    }
                }
                // Italic *text*
                s[i] == '*' && (i == 0 || s[i - 1] != '*') -> {
                    val end = s.indexOf('*', i + 1)
                    if (end != -1 && (end + 1 >= s.length || s[end + 1] != '*')) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(s.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(s[i]); i++
                    }
                }
                // Inline code `text`
                s[i] == '`' -> {
                    val end = s.indexOf('`', i + 1)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0xFF1A1F2A),
                                color = Color(0xFFE8975A),
                                fontSize = 14.sp
                            )
                        ) {
                            append(" ${s.substring(i + 1, end)} ")
                        }
                        i = end + 1
                    } else {
                        append(s[i]); i++
                    }
                }
                else -> {
                    append(s[i]); i++
                }
            }
        }
    }
}

// Legacy aliases used by other files
sealed class MarkdownBlock {
    data class TextBlock(val text: String) : MarkdownBlock()
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock()
}

fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    return splitCodeBlocks(text).map {
        when (it) {
            is ContentBlock.Prose -> MarkdownBlock.TextBlock(it.text)
            is ContentBlock.Code -> MarkdownBlock.CodeBlock(it.code, it.language)
        }
    }
}

fun parseInlineMarkdown(text: String): AnnotatedString = renderInline(text)
