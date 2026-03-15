package com.sylvester.rustsensei.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BookContentRenderer(
    content: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val blocks = parseBookContent(content)
        blocks.forEach { block ->
            when (block) {
                is BookBlock.Heading -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = block.text,
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineSmall
                            2 -> MaterialTheme.typography.titleLarge
                            else -> MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                is BookBlock.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(block.text),
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                is BookBlock.Code -> {
                    CodeBlock(code = block.code, language = block.language)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                is BookBlock.BulletList -> {
                    block.items.forEach { item ->
                        Text(
                            text = parseInlineMarkdown("  \u2022 $item"),
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

sealed class BookBlock {
    data class Heading(val text: String, val level: Int) : BookBlock()
    data class Paragraph(val text: String) : BookBlock()
    data class Code(val code: String, val language: String) : BookBlock()
    data class BulletList(val items: List<String>) : BookBlock()
}

private fun parseBookContent(content: String): List<BookBlock> {
    val blocks = mutableListOf<BookBlock>()
    val lines = content.split("\n")
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        when {
            // Code blocks
            line.trimStart().startsWith("```") -> {
                val language = line.trimStart().removePrefix("```").trim().ifEmpty { "rust" }
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                blocks.add(BookBlock.Code(codeLines.joinToString("\n"), language))
                i++ // skip closing ```
            }
            // Headings
            line.startsWith("### ") -> {
                blocks.add(BookBlock.Heading(line.removePrefix("### ").trim(), 3))
                i++
            }
            line.startsWith("## ") -> {
                blocks.add(BookBlock.Heading(line.removePrefix("## ").trim(), 2))
                i++
            }
            line.startsWith("# ") -> {
                blocks.add(BookBlock.Heading(line.removePrefix("# ").trim(), 1))
                i++
            }
            // Bullet lists
            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                val items = mutableListOf<String>()
                while (i < lines.size && (lines[i].trimStart().startsWith("- ") || lines[i].trimStart().startsWith("* "))) {
                    items.add(lines[i].trimStart().removePrefix("- ").removePrefix("* ").trim())
                    i++
                }
                blocks.add(BookBlock.BulletList(items))
            }
            // Empty lines
            line.isBlank() -> {
                i++
            }
            // Regular paragraphs
            else -> {
                val paragraphLines = mutableListOf<String>()
                while (i < lines.size && lines[i].isNotBlank() &&
                    !lines[i].startsWith("#") &&
                    !lines[i].trimStart().startsWith("```") &&
                    !lines[i].trimStart().startsWith("- ") &&
                    !lines[i].trimStart().startsWith("* ")
                ) {
                    paragraphLines.add(lines[i])
                    i++
                }
                if (paragraphLines.isNotEmpty()) {
                    blocks.add(BookBlock.Paragraph(paragraphLines.joinToString(" ")))
                }
            }
        }
    }

    return blocks
}
