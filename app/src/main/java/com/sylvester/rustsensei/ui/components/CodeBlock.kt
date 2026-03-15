package com.sylvester.rustsensei.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Neon Terminal code block colors ──────────────────────────────────

// Rust syntax highlighting — tuned for dark neon background
private val KeywordColor = Color(0xFFCC7832)      // orange - keywords
private val StringColor = Color(0xFF6A8759)        // green - strings
private val CommentColor = Color(0xFF5C6370)       // muted gray - comments
private val TypeColor = Color(0xFF4DEEEA)          // neon cyan - types
private val NumberColor = Color(0xFFB5CEA8)        // light green - numbers
private val MacroColor = Color(0xFFD4D4AA)         // yellow - macros
private val FunctionColor = Color(0xFFDCDCAA)      // light yellow - functions
private val DefaultCodeColor = Color(0xFFD4D4D4)   // light gray - default

// Code block structure colors — embedded terminal feel
private val CodeBgColor = Color(0xFF06080C)        // same as app background
private val CodeHeaderColor = Color(0xFF0C1018)    // dark navy header
private val NeonAccent = Color(0xFFCE412B)         // primary orange for accents

private val rustKeywords = setOf(
    "fn", "let", "mut", "pub", "struct", "enum", "impl", "trait", "use", "mod",
    "match", "if", "else", "for", "while", "loop", "return", "break", "continue",
    "where", "as", "in", "ref", "self", "Self", "super", "crate", "const", "static",
    "type", "unsafe", "async", "await", "move", "dyn", "extern", "true", "false",
    "Some", "None", "Ok", "Err", "Box", "Vec", "String", "Option", "Result"
)

private val rustTypes = setOf(
    "i8", "i16", "i32", "i64", "i128", "isize",
    "u8", "u16", "u32", "u64", "u128", "usize",
    "f32", "f64", "bool", "char", "str", "&str"
)

@Composable
fun CodeBlock(
    code: String,
    language: String = "rust",
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var showCopied by remember { mutableStateOf(false) }
    val lines = code.lines()
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CodeBgColor)
            .semantics { contentDescription = "$language code block: $code" }
    ) {
        // Header with neon bottom border
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CodeHeaderColor)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language pill — primary at 15% alpha bg, primary text, monospace
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = language,
                        color = primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(modifier = Modifier.weight(1f))

                // Copy button with neon glow on "Copied!" state
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        showCopied = true
                        scope.launch {
                            delay(2000)
                            showCopied = false
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    if (showCopied) {
                        Text(
                            "Copied!",
                            color = primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Thin 1dp neon accent line under header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(primary.copy(alpha = 0.20f))
            )
        }

        // Code content with line numbers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Line numbers column — orange-tinted
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(CodeBgColor)
                    .padding(start = 12.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                lines.forEachIndexed { index, _ ->
                    Text(
                        text = "${index + 1}",
                        color = primary.copy(alpha = 0.30f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.End
                    )
                }
            }

            // Neon divider between line numbers and code
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(primary.copy(alpha = 0.10f))
            )

            // Code content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text(
                    text = if (language == "rust") highlightRustSyntax(code) else AnnotatedString(code),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    color = DefaultCodeColor
                )
            }
        }
    }
}

private fun highlightRustSyntax(code: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val text = code

        while (i < text.length) {
            when {
                // Line comments
                i + 1 < text.length && text[i] == '/' && text[i + 1] == '/' -> {
                    val end = text.indexOf('\n', i).let { if (it == -1) text.length else it }
                    withStyle(SpanStyle(color = CommentColor)) {
                        append(text.substring(i, end))
                    }
                    i = end
                }
                // String literals
                text[i] == '"' -> {
                    val end = findStringEnd(text, i)
                    withStyle(SpanStyle(color = StringColor)) {
                        append(text.substring(i, end))
                    }
                    i = end
                }
                // Char literals
                text[i] == '\'' && i + 2 < text.length && text[i + 2] == '\'' -> {
                    withStyle(SpanStyle(color = StringColor)) {
                        append(text.substring(i, i + 3))
                    }
                    i += 3
                }
                // Numbers
                text[i].isDigit() -> {
                    val start = i
                    while (i < text.length && (text[i].isDigit() || text[i] == '.' || text[i] == '_' ||
                                text[i] == 'x' || text[i] == 'b' || text[i] == 'o' ||
                                (text[i] in 'a'..'f') || (text[i] in 'A'..'F'))) {
                        i++
                    }
                    withStyle(SpanStyle(color = NumberColor)) {
                        append(text.substring(start, i))
                    }
                }
                // Identifiers, keywords, macros
                text[i].isLetter() || text[i] == '_' -> {
                    val start = i
                    while (i < text.length && (text[i].isLetterOrDigit() || text[i] == '_')) {
                        i++
                    }
                    val word = text.substring(start, i)

                    // Check for macro (word!)
                    if (i < text.length && text[i] == '!' && word != "return") {
                        withStyle(SpanStyle(color = MacroColor)) {
                            append(word)
                            append("!")
                        }
                        i++ // skip the !
                    } else if (word in rustKeywords) {
                        withStyle(SpanStyle(color = KeywordColor, fontWeight = FontWeight.Bold)) {
                            append(word)
                        }
                    } else if (word in rustTypes) {
                        withStyle(SpanStyle(color = TypeColor)) {
                            append(word)
                        }
                    } else if (i < text.length && text[i] == '(') {
                        withStyle(SpanStyle(color = FunctionColor)) {
                            append(word)
                        }
                    } else {
                        withStyle(SpanStyle(color = DefaultCodeColor)) {
                            append(word)
                        }
                    }
                }
                else -> {
                    withStyle(SpanStyle(color = DefaultCodeColor)) {
                        append(text[i])
                    }
                    i++
                }
            }
        }
    }
}

private fun findStringEnd(text: String, start: Int): Int {
    var i = start + 1
    while (i < text.length) {
        if (text[i] == '\\') {
            i += 2
            continue
        }
        if (text[i] == '"') return i + 1
        i++
    }
    return text.length
}
