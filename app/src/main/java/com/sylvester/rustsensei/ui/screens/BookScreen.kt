package com.sylvester.rustsensei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.ui.components.BookContentRenderer
import com.sylvester.rustsensei.viewmodel.BookScreenMode
import com.sylvester.rustsensei.viewmodel.BookViewModel

@Composable
fun BookScreen(
    viewModel: BookViewModel,
    onAskSensei: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.mode != BookScreenMode.INDEX) {
        viewModel.navigateBack()
    }

    // P1 Fix #9: show error snackbar/dialog when content fails to load
    uiState.errorMessage?.let { error ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    when (uiState.mode) {
        BookScreenMode.INDEX -> BookIndexView(viewModel = viewModel)
        BookScreenMode.CHAPTER -> ChapterView(viewModel = viewModel)
        BookScreenMode.SECTION -> SectionView(
            viewModel = viewModel,
            onAskSensei = onAskSensei
        )
    }
}

@Composable
private fun BookIndexView(viewModel: BookViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        item {
            // headlineMedium (monospace via theme)
            Text(
                text = "The Rust Programming Language",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Continue Reading — terminal-style with ">" prefix
        if (uiState.lastReadChapterId != null && uiState.lastReadSectionId != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.openChapter(uiState.lastReadChapterId!!)
                            viewModel.openSection(
                                uiState.lastReadChapterId!!,
                                uiState.lastReadSectionId!!
                            )
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ">",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue: ${uiState.lastReadSectionId!!.replace("-", " ")}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        itemsIndexed(uiState.chapters, key = { _, ch -> ch.id }) { index, chapter ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openChapter(chapter.id) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chapter number in primary, monospace, labelLarge
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(32.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    // Title in titleSmall
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    // Section count in labelSmall
                    Text(
                        text = "${chapter.sectionIds.size} sections",
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
            // Orange-tinted neon dividers at 8% alpha
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier.padding(start = 48.dp)
            )
        }
    }
}

@Composable
private fun ChapterView(viewModel: BookViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val chapter = uiState.currentChapter ?: return

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
                text = chapter.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
        ) {
            items(chapter.sections, key = { it.id }) { section ->
                val progress = uiState.sectionProgress[section.id]
                val isCompleted = progress?.isCompleted == true

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.openSection(uiState.currentChapterId!!, section.id)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCompleted) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = Color(0xFF3FB950),
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        // Section title in bodyLarge
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        if (progress != null && !isCompleted && progress.readPercent > 0f) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress.readPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                strokeCap = StrokeCap.Round,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
                // Orange-tinted neon dividers
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 50.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionView(
    viewModel: BookViewModel,
    onAskSensei: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val section = uiState.currentSection ?: return
    val scrollState = rememberScrollState()

    // Fix #1 & #8: Track scroll progress, mark complete only once (via ViewModel flag)
    LaunchedEffect(scrollState) {
        var lastSaveTime = 0L
        snapshotFlow { scrollState.value to scrollState.maxValue }
            .collect { (current, max) ->
                if (max > 0) {
                    val percent = current.toFloat() / max.toFloat()
                    val now = System.currentTimeMillis()
                    if (now - lastSaveTime > 5000) {
                        viewModel.updateReadProgress(percent)
                        lastSaveTime = now
                    }
                    // Fix #1: markSectionComplete checks the flag internally, safe to call
                    if (percent >= 0.95f) {
                        viewModel.markSectionComplete()
                    }
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
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
            IconButton(onClick = { viewModel.toggleBookmark() }) {
                Icon(
                    if (uiState.sectionProgress[section.id]?.bookmarked == true)
                        Icons.Default.Bookmark
                    else
                        Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (uiState.sectionProgress[section.id]?.bookmarked == true)
                        MaterialTheme.colorScheme.secondary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Content — bodyLarge at 26sp line height
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            BookContentRenderer(content = section.content)

            section.codeExamples.forEach { example ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = example.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                com.sylvester.rustsensei.ui.components.CodeBlock(
                    code = example.code,
                    language = "rust"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom navigation — neon divider
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { viewModel.navigateToPreviousSection() }) {
                Icon(
                    Icons.AutoMirrored.Filled.NavigateBefore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Previous", style = MaterialTheme.typography.labelLarge)
            }
            TextButton(onClick = { onAskSensei(section.content, "") }) {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ask Sensei", style = MaterialTheme.typography.labelLarge)
            }
            TextButton(onClick = { viewModel.navigateToNextSection() }) {
                Text("Next", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
