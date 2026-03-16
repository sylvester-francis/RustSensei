package com.sylvester.rustsensei.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * VisualTransformation that applies Rust syntax highlighting to the editor text.
 * The transformation does not change the text content (only styling), so the
 * offset mapping is identity — cursor positions map 1:1.
 */
private class RustSyntaxTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = highlightRustSyntax(text.text)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

/**
 * Bracket pair definitions for auto-close behavior.
 */
private val bracketPairs = mapOf(
    '{' to '}',
    '(' to ')',
    '[' to ']',
    '"' to '"'
)

/**
 * A reusable code editor component with line numbers, Rust syntax highlighting,
 * auto-indent, and auto-close brackets. Styled to match the Neon Terminal theme.
 *
 * @param value The current TextFieldValue (text + cursor/selection state)
 * @param onValueChange Callback invoked when the text or selection changes
 * @param modifier Modifier for the root container
 * @param minHeight Minimum height of the editor area
 * @param readOnly Whether the editor is read-only
 */
@Composable
fun CodeEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 200.dp,
    readOnly: Boolean = false
) {
    val syntaxTransformation = remember { RustSyntaxTransformation() }
    val lines = value.text.lines()
    val lineCount = lines.size.coerceAtLeast(1)
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CodeBgColor)
    ) {
        // Editor tab label — terminal tab style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CodeBgColor.copy(alpha = 0.8f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(primary.copy(alpha = 0.10f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Editor",
                    color = primary.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Thin neon accent line under header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(primary.copy(alpha = 0.15f))
        )

        // Code content with line numbers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .height(IntrinsicSize.Min)
        ) {
            // Line numbers column
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(CodeBgColor)
                    .padding(start = 10.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    Text(
                        text = "$i",
                        color = primary.copy(alpha = 0.30f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
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

            // Code editor area with horizontal scroll
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 10.dp, top = 14.dp, end = 14.dp, bottom = 14.dp)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = { newValue ->
                        val processed = processEditorInput(
                            oldValue = value,
                            newValue = newValue
                        )
                        onValueChange(processed)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = minHeight - 40.dp),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = DefaultCodeColor
                    ),
                    cursorBrush = SolidColor(primary),
                    visualTransformation = syntaxTransformation,
                    readOnly = readOnly
                )
            }
        }
    }
}

/**
 * Process editor input to handle auto-indent and auto-close brackets.
 *
 * Auto-indent: When the user presses Enter after a line ending with `{`,
 * the new line gets 4 extra spaces of indentation.
 *
 * Auto-close: When the user types an opening bracket, the matching closing
 * bracket is inserted and the cursor is placed between them.
 */
internal fun processEditorInput(
    oldValue: TextFieldValue,
    newValue: TextFieldValue
): TextFieldValue {
    val oldText = oldValue.text
    val newText = newValue.text
    val cursor = newValue.selection.start

    // Only process single-character insertions (not paste, delete, etc.)
    if (newText.length != oldText.length + 1 || cursor < 1) {
        return newValue
    }

    val insertedChar = newText[cursor - 1]

    // Auto-indent: user pressed Enter
    if (insertedChar == '\n') {
        // Find the line before the newline to determine indentation
        val beforeCursor = newText.substring(0, cursor - 1)
        val lastLineStart = beforeCursor.lastIndexOf('\n') + 1
        val lastLine = beforeCursor.substring(lastLineStart)

        // Calculate existing indentation of the previous line
        val existingIndent = lastLine.takeWhile { it == ' ' }
        val endsWithOpenBrace = lastLine.trimEnd().endsWith('{')

        val indent = if (endsWithOpenBrace) {
            existingIndent + "    "
        } else {
            existingIndent
        }

        if (indent.isNotEmpty()) {
            val before = newText.substring(0, cursor)
            val after = newText.substring(cursor)
            val result = before + indent + after
            val newCursorPos = cursor + indent.length
            return TextFieldValue(
                text = result,
                selection = androidx.compose.ui.text.TextRange(newCursorPos)
            )
        }
        return newValue
    }

    // Auto-close brackets
    val closingChar = bracketPairs[insertedChar]
    if (closingChar != null) {
        // For quotes, only auto-close if there isn't already the same char right after cursor
        if (insertedChar == '"') {
            if (cursor < newText.length && newText[cursor] == '"') {
                return newValue
            }
        }

        val before = newText.substring(0, cursor)
        val after = newText.substring(cursor)
        val result = before + closingChar + after
        return TextFieldValue(
            text = result,
            selection = androidx.compose.ui.text.TextRange(cursor)
        )
    }

    return newValue
}
