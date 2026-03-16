package com.sylvester.rustsensei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.ui.components.BookContentRenderer
import com.sylvester.rustsensei.ui.components.CodeBlock
import com.sylvester.rustsensei.viewmodel.ReferenceScreenMode
import com.sylvester.rustsensei.viewmodel.ReferenceViewModel
import org.json.JSONObject

private fun sectionIcon(id: String): ImageVector = when (id) {
    "cheatsheets" -> Icons.Default.Speed
    "compiler-errors" -> Icons.Default.BugReport
    "comparisons" -> Icons.Default.CompareArrows
    "challenges" -> Icons.Default.Code
    "crates" -> Icons.Default.Extension
    "async-rust" -> Icons.Default.Speed
    "cli-guide" -> Icons.Default.Terminal
    "design-patterns" -> Icons.Default.Psychology
    "glossary" -> Icons.Default.MenuBook
    "testing" -> Icons.Default.Lightbulb
    "unsafe-guide" -> Icons.Default.Security
    "interview-prep" -> Icons.Default.Quiz
    else -> Icons.Default.Book
}

@Composable
fun ReferenceScreen(viewModel: ReferenceViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.mode != ReferenceScreenMode.INDEX) {
        viewModel.navigateBack()
    }

    uiState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Unavailable") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
            }
        )
    }

    when (uiState.mode) {
        ReferenceScreenMode.INDEX -> ReferenceIndexView(viewModel)
        ReferenceScreenMode.SECTION_LIST -> SectionListView(viewModel)
        ReferenceScreenMode.ITEM_DETAIL -> ItemDetailView(viewModel)
    }
}

@Composable
private fun ReferenceIndexView(viewModel: ReferenceViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            // headlineMedium (monospace via theme)
            Text(
                text = "Reference",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Quick lookup, cheat sheets, and deep dives",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(uiState.sections, key = { it.id }) { section ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openSection(section.id) }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    sectionIcon(section.id),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${section.items.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
            // Neon-tinted divider at 8% alpha
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier.padding(start = 48.dp)
            )
        }
    }
}

@Composable
private fun SectionListView(viewModel: ReferenceViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val section = uiState.currentSection ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(section.items) { itemId ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openItem(section.id, itemId) }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = itemId.replace("-", " ").replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Neon-tinted divider
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ItemDetailView(viewModel: ReferenceViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val json = uiState.currentItemJson ?: return

    val title = json.optString("title", uiState.currentItemId ?: "")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Render based on content type — with 24dp between sections, 12dp between code blocks and text
            when {
                // Cheat sheet format
                json.has("sections") && json.optJSONArray("sections") != null -> {
                    val sections = json.getJSONArray("sections")
                    for (i in 0 until sections.length()) {
                        val sec = sections.getJSONObject(i)
                        Text(
                            text = sec.getString("heading"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = if (i > 0) 24.dp else 0.dp, bottom = 12.dp)
                        )
                        val entries = sec.getJSONArray("entries")
                        for (j in 0 until entries.length()) {
                            val entry = entries.getJSONObject(j)
                            Text(
                                text = entry.getString("label"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CodeBlock(code = entry.getString("code"), language = "rust")
                            if (entry.has("note")) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = entry.getString("note"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Compiler error format
                json.has("error_message") -> {
                    json.optString("explanation", "").let {
                        if (it.isNotEmpty()) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 26.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                    Text(
                        "Error message:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CodeBlock(code = json.getString("error_message"), language = "text")
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "Bad code:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CodeBlock(code = json.optString("example_bad", ""), language = "rust")
                    Spacer(modifier = Modifier.height(24.dp))

                    val fixes = json.optJSONArray("fixes")
                    if (fixes != null && fixes.length() > 0) {
                        Text(
                            "Fixes:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        for (i in 0 until fixes.length()) {
                            val fix = fixes.getJSONObject(i)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${i + 1}. ${fix.getString("description")}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CodeBlock(code = fix.getString("code"), language = "rust")
                        }
                    }

                    json.optString("tip", "").let {
                        if (it.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Tip: $it",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                // Comparison format
                json.has("comparisons") -> {
                    json.optString("description", "").let {
                        if (it.isNotEmpty()) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 26.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                    val comparisons = json.getJSONArray("comparisons")
                    for (i in 0 until comparisons.length()) {
                        val cmp = comparisons.getJSONObject(i)
                        Text(
                            text = cmp.getString("concept"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = if (i > 0) 24.dp else 0.dp)
                        )
                        // Show each language's code
                        for (lang in listOf("go", "python", "typescript", "rust")) {
                            if (cmp.has(lang)) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = lang.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                CodeBlock(code = cmp.getString(lang), language = lang)
                            }
                        }
                        cmp.optString("note", "").let {
                            if (it.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Generic content format (crates, guides, challenges, etc.)
                else -> {
                    // Try rendering known fields in order
                    json.optString("description", "").let {
                        if (it.isNotEmpty()) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 26.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                    json.optString("content", "").let {
                        if (it.isNotEmpty()) {
                            BookContentRenderer(content = it)
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                    json.optString("quickStart", "").let {
                        if (it.isNotEmpty()) {
                            Text(
                                "Quick Start:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CodeBlock(code = it, language = "rust")
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                    json.optString("starterCode", "").let {
                        if (it.isNotEmpty()) {
                            Text(
                                "Starter Code:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CodeBlock(code = it, language = "rust")
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                    json.optString("solution", "").let {
                        if (it.isNotEmpty()) {
                            Text(
                                "Solution:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CodeBlock(code = it, language = "rust")
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                    // Code examples array
                    json.optJSONArray("codeExamples")?.let { examples ->
                        for (i in 0 until examples.length()) {
                            val ex = examples.getJSONObject(i)
                            Text(
                                text = ex.getString("description"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CodeBlock(code = ex.getString("code"), language = "rust")
                        }
                    }
                    // Common patterns array
                    json.optJSONArray("commonPatterns")?.let { patterns ->
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Common Patterns:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        for (i in 0 until patterns.length()) {
                            val p = patterns.getJSONObject(i)
                            Text(
                                text = p.getString("title"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CodeBlock(code = p.getString("code"), language = "rust")
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    // Key points array
                    json.optJSONArray("keyPoints")?.let { points ->
                        Text(
                            "Key Points:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        for (i in 0 until points.length()) {
                            Text(
                                text = "\u2022 ${points.getString(i)}",
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    // Mappings array (concepts-mapping)
                    json.optJSONArray("mappings")?.let { mappings ->
                        for (i in 0 until mappings.length()) {
                            val m = mappings.getJSONObject(i)
                            Text(
                                text = m.getString("concept"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = if (i > 0) 24.dp else 0.dp)
                            )
                            for (lang in listOf("go", "python", "typescript", "rust")) {
                                if (m.has(lang)) {
                                    Text(
                                        text = "${lang.replaceFirstChar { it.uppercase() }}: ${m.getString(lang)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                    )
                                }
                            }
                            m.optString("key_insight", "").let {
                                if (it.isNotEmpty()) {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    // Glossary terms
                    json.optJSONArray("terms")?.let { terms ->
                        for (i in 0 until terms.length()) {
                            val t = terms.getJSONObject(i)
                            Text(
                                text = t.getString("term"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = if (i > 0) 24.dp else 0.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = t.getString("definition"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
