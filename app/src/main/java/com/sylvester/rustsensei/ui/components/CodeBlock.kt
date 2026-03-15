package com.sylvester.rustsensei.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Rust syntax highlighting colors
private val KeywordColor = Color(0xFFCC7832)      // orange - keywords
private val StringColor = Color(0xFF6A8759)        // green - strings
private val CommentColor = Color(0xFF808080)       // gray - comments
private val TypeColor = Color(0xFF4EC9B0)          // teal - types
private val NumberColor = Color(0xFFB5CEA8)        // light green - numbers
private val MacroColor = Color(0xFFD4D4AA)         // yellow - macros
private val FunctionColor = Color(0xFFDCDCAA)      // light yellow - functions
private val DefaultCodeColor = Color(0xFFD4D4D4)   // light gray - default

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
    ) {
        // Header with language label and copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2D2D2D))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language,
                color = Color(0xFF808080),
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(code))
                    showCopied = true
                    scope.launch {
                        delay(2000)
                        showCopied = false
                    }
                }
            ) {
                if (showCopied) {
                    Text("Copied!", color = Color(0xFF4EC9B0), fontSize = 12.sp)
                } else {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Color(0xFF808080)
                    )
                }
            }
        }

        // Code content with syntax highlighting
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = if (language == "rust") highlightRustSyntax(code) else AnnotatedString(code),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = DefaultCodeColor
            )
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
                // Macros (word followed by !)
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
