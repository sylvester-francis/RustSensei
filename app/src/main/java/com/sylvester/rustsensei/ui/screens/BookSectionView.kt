package com.sylvester.rustsensei.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.ui.components.BookContentRenderer
import com.sylvester.rustsensei.ui.components.CodeBlock
import com.sylvester.rustsensei.ui.theme.Alpha
import com.sylvester.rustsensei.ui.theme.Dimens
import com.sylvester.rustsensei.ui.theme.Spacing
import com.sylvester.rustsensei.viewmodel.BookViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookSectionView(
    viewModel: BookViewModel,
    onAskSensei: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val section = uiState.currentSection ?: return
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var showNotesSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var noteText by remember(uiState.currentSectionId) { mutableStateOf("") }
    var currentNoteId by remember(uiState.currentSectionId) { mutableStateOf<Long?>(null) }

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

    val scrollPercent = remember(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue > 0) scrollState.value.toFloat() / scrollState.maxValue.toFloat()
        else 0f
    }

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
                    if (percent >= 0.95f) viewModel.markSectionComplete()
                }
            }
    }

    // Notes bottom sheet
    if (showNotesSheet) {
        NotesBottomSheet(
            sectionTitle = section.title,
            noteText = noteText,
            onNoteTextChange = { noteText = it },
            hasExistingNote = currentNoteId != null,
            onSave = {
                val sectionId = uiState.currentSectionId
                if (sectionId != null && noteText.isNotBlank()) {
                    viewModel.saveNote(sectionId, noteText, currentNoteId)
                }
                scope.launch { sheetState.hide(); showNotesSheet = false }
            },
            onDelete = {
                currentNoteId?.let { viewModel.deleteNote(it) }
                noteText = ""
                currentNoteId = null
                scope.launch { sheetState.hide(); showNotesSheet = false }
            },
            onDismiss = {
                val sectionId = uiState.currentSectionId
                if (sectionId != null && noteText.isNotBlank()) {
                    viewModel.saveNote(sectionId, noteText, currentNoteId)
                } else if (sectionId != null && noteText.isBlank() && currentNoteId != null) {
                    currentNoteId?.let { viewModel.deleteNote(it) }
                    currentNoteId = null
                }
                showNotesSheet = false
            },
            sheetState = sheetState
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.XS, vertical = Spacing.XS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier
                    .size(Dimens.CompactTopBarHeight)
                    .semantics { contentDescription = "Navigate back" }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = uiState.currentChapter?.title ?: section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val isBookmarked = uiState.sectionProgress[section.id]?.bookmarked == true
            IconButton(
                onClick = { viewModel.toggleBookmark() },
                modifier = Modifier
                    .size(Dimens.CompactTopBarHeight)
                    .semantics {
                        contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark"
                    }
            ) {
                Icon(
                    if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = if (isBookmarked) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Reading progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Alpha.MUTED))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = scrollPercent.coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = Alpha.SOFT)
                            )
                        )
                    )
            )
        }

        // Section title
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Spacing.MD),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = Dimens.ScreenPadding)
        ) {
            BookContentRenderer(content = section.content)

            if (section.codeExamples.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.LG))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.DIVIDER / 2)
                )
                Spacer(modifier = Modifier.height(Spacing.MD))

                section.codeExamples.forEach { example ->
                    Text(
                        text = example.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 26.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CodeBlock(code = example.code, language = "rust")
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(Spacing.XXL))
        }

        // Footer navigation
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.DIVIDER / 2)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.XS, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { viewModel.navigateToPreviousSection() },
                modifier = Modifier
                    .height(Dimens.CompactTopBarHeight)
                    .semantics { contentDescription = "Previous section" }
            ) {
                Icon(Icons.AutoMirrored.Filled.NavigateBefore, null, modifier = Modifier.size(Dimens.IconSM))
                Spacer(modifier = Modifier.width(Spacing.XXS))
                Text("Prev", style = MaterialTheme.typography.labelMedium)
            }

            TextButton(
                onClick = { showNotesSheet = true },
                modifier = Modifier
                    .height(Dimens.CompactTopBarHeight)
                    .semantics { contentDescription = "Open notes for this section" }
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(Spacing.XS))
                Text("Notes", style = MaterialTheme.typography.labelMedium)
            }

            TextButton(
                onClick = { onAskSensei(section.content, "") },
                modifier = Modifier
                    .height(Dimens.CompactTopBarHeight)
                    .semantics { contentDescription = "Ask Sensei about this section" }
            ) {
                Icon(Icons.Default.Chat, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(Spacing.XS))
                Text("Sensei", style = MaterialTheme.typography.labelMedium)
            }

            TextButton(
                onClick = { viewModel.navigateToNextSection() },
                modifier = Modifier
                    .height(Dimens.CompactTopBarHeight)
                    .semantics { contentDescription = "Next section" }
            ) {
                Text("Next", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(Spacing.XXS))
                Icon(Icons.AutoMirrored.Filled.NavigateNext, null, modifier = Modifier.size(Dimens.IconSM))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesBottomSheet(
    sectionTitle: String,
    noteText: String,
    onNoteTextChange: (String) -> Unit,
    hasExistingNote: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: androidx.compose.material3.SheetState
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding, vertical = Spacing.SM)
                .padding(bottom = Spacing.XXXL)
        ) {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.XS)
            )
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.SECONDARY),
                modifier = Modifier.padding(bottom = Spacing.MD)
            )
            TextField(
                value = noteText,
                onValueChange = onNoteTextChange,
                placeholder = {
                    Text(
                        "Write your notes here...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.HINT)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Alpha.MUTED),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(Spacing.MD))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (hasExistingNote) {
                    TextButton(onClick = onDelete) {
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.SM))
                }
                Button(
                    onClick = onSave,
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
