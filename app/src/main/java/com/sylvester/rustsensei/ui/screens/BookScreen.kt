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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.ui.components.BookContentRenderer
import com.sylvester.rustsensei.viewmodel.BookScreenMode
import com.sylvester.rustsensei.viewmodel.BookViewModel
import com.sylvester.rustsensei.viewmodel.ReferenceScreenMode
import com.sylvester.rustsensei.viewmodel.ReferenceViewModel
import kotlinx.coroutines.launch

private fun referenceSectionIcon(id: String): ImageVector = when (id) {
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
fun BookScreen(
    viewModel: BookViewModel,
    referenceViewModel: ReferenceViewModel,
    onOpenReference: (sectionId: String) -> Unit,
    onAskSensei: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val refState by referenceViewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.mode != BookScreenMode.INDEX || refState.mode != ReferenceScreenMode.INDEX) {
        if (refState.mode != ReferenceScreenMode.INDEX) {
            referenceViewModel.navigateBack()
        } else {
            viewModel.navigateBack()
        }
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

    // Reference error dialog
    refState.errorMessage?.let { error ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { referenceViewModel.clearError() },
            title = { Text("Unavailable") },
            text = { Text(error) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { referenceViewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    // If reference is navigated into (SECTION_LIST or ITEM_DETAIL), show ReferenceScreen content inline
    if (refState.mode != ReferenceScreenMode.INDEX) {
        ReferenceScreen(viewModel = referenceViewModel)
        return
    }

    when (uiState.mode) {
        BookScreenMode.INDEX -> BookIndexView(
            viewModel = viewModel,
            referenceViewModel = referenceViewModel,
            onOpenReference = onOpenReference
        )
        BookScreenMode.CHAPTER -> ChapterView(viewModel = viewModel)
        BookScreenMode.SECTION -> SectionView(
            viewModel = viewModel,
            onAskSensei = onAskSensei
        )
    }
}

@Composable
private fun BookIndexView(
    viewModel: BookViewModel,
    referenceViewModel: ReferenceViewModel,
    onOpenReference: (sectionId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val refState by referenceViewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        // Continue Reading card — prominent, at top
        val lastChapterId = uiState.lastReadChapterId
        val lastSectionId = uiState.lastReadSectionId
        if (lastChapterId != null && lastSectionId != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .clickable {
                            viewModel.openChapter(lastChapterId)
                            viewModel.openSection(lastChapterId, lastSectionId)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .semantics { contentDescription = "Continue reading: ${lastSectionId.replace("-", " ")}" },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "\u25B6",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Continue Reading",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = lastSectionId.replace("-", " "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Section header: "The Rust Book"
        item {
            Text(
                text = "The Rust Book",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        itemsIndexed(uiState.chapters, key = { _, ch -> ch.id }) { index, chapter ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openChapter(chapter.id) }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .semantics { contentDescription = "Chapter ${index + 1}: ${chapter.title}, ${chapter.sectionIds.size} sections" },
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

        // Section header: "Quick Reference" with 32dp top margin
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Quick Reference",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Reference categories from ReferenceViewModel
        items(refState.sections, key = { "ref-${it.id}" }) { section ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenReference(section.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .semantics { contentDescription = "${section.title}, ${section.items.size} items" },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    referenceSectionIcon(section.id),
                    contentDescription = section.title,
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
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier.padding(start = 54.dp)
            )
        }

        // Bottom breathing room
        item {
            Spacer(modifier = Modifier.height(16.dp))
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
                val chapterId = uiState.currentChapterId

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            chapterId?.let { viewModel.openSection(it, section.id) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionView(
    viewModel: BookViewModel,
    onAskSensei: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val section = uiState.currentSection ?: return
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Notes bottom sheet state
    var showNotesSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var noteText by remember(uiState.currentSectionId) { mutableStateOf("") }
    var currentNoteId by remember(uiState.currentSectionId) { mutableStateOf<Long?>(null) }

    // Load existing note for this section
    LaunchedEffect(uiState.currentSectionId) {
        val sectionId = uiState.currentSectionId ?: return@LaunchedEffect
        val notes = viewModel.getNotesForSection(sectionId)
        if (notes.isNotEmpty()) {
            noteText = notes.first().content
            currentNoteId = notes.first().id
        } else {
            noteText = ""
            currentNoteId = null
        }
    }

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

    // Notes bottom sheet
    if (showNotesSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                // Save note on dismiss
                val sectionId = uiState.currentSectionId
                if (sectionId != null && noteText.isNotBlank()) {
                    viewModel.saveNote(sectionId, noteText, currentNoteId)
                } else if (sectionId != null && noteText.isBlank() && currentNoteId != null) {
                    viewModel.deleteNote(currentNoteId!!)
                    currentNoteId = null
                }
                showNotesSheet = false
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                TextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = {
                        Text(
                            "Write your notes here...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (currentNoteId != null) {
                        TextButton(
                            onClick = {
                                viewModel.deleteNote(currentNoteId!!)
                                noteText = ""
                                currentNoteId = null
                                scope.launch {
                                    sheetState.hide()
                                    showNotesSheet = false
                                }
                            }
                        ) {
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Button(
                        onClick = {
                            val sectionId = uiState.currentSectionId
                            if (sectionId != null && noteText.isNotBlank()) {
                                viewModel.saveNote(sectionId, noteText, currentNoteId)
                            }
                            scope.launch {
                                sheetState.hide()
                                showNotesSheet = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Save", style = MaterialTheme.typography.labelLarge)
                    }
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
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { viewModel.navigateToPreviousSection() }) {
                Icon(
                    Icons.AutoMirrored.Filled.NavigateBefore,
                    contentDescription = "Previous section",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("Prev", style = MaterialTheme.typography.labelMedium)
            }
            TextButton(onClick = { showNotesSheet = true }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Open notes",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("Notes", style = MaterialTheme.typography.labelMedium)
            }
            TextButton(onClick = { onAskSensei(section.content, "") }) {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = "Ask Sensei about this section",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("Sensei", style = MaterialTheme.typography.labelMedium)
            }
            TextButton(onClick = { viewModel.navigateToNextSection() }) {
                Text("Next", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = "Next section",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
