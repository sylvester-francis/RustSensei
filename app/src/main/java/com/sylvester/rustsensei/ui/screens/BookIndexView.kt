package com.sylvester.rustsensei.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sylvester.rustsensei.ui.theme.Alpha
import com.sylvester.rustsensei.ui.theme.Dimens
import com.sylvester.rustsensei.ui.theme.Spacing
import com.sylvester.rustsensei.viewmodel.BookViewModel
import com.sylvester.rustsensei.viewmodel.LearningPathViewModel
import com.sylvester.rustsensei.viewmodel.ReferenceScreenMode
import com.sylvester.rustsensei.viewmodel.ReferenceViewModel
import com.sylvester.rustsensei.viewmodel.ReviewViewModel
import com.sylvester.rustsensei.ui.theme.AppColors

@Composable
internal fun BookIndexView(
    viewModel: BookViewModel,
    referenceViewModel: ReferenceViewModel,
    reviewViewModel: ReviewViewModel? = null,
    learningPathViewModel: LearningPathViewModel? = null,
    onOpenReference: (sectionId: String) -> Unit,
    onNavigateToReview: () -> Unit = {},
    onNavigateToLearningPaths: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val refState by referenceViewModel.uiState.collectAsState()
    val reviewUiState = reviewViewModel?.uiState?.collectAsState()?.value
    val pathUiState = learningPathViewModel?.uiState?.collectAsState()?.value
    var showingReference by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Mode toggle (Book / Reference) ──
        ModeToggleStrip(
            showingReference = showingReference,
            onSelectBook = { showingReference = false },
            onSelectReference = { showingReference = true }
        )

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.DIVIDER / 2)
        )

        if (!showingReference) {
            BookContent(
                uiState = uiState,
                reviewUiState = reviewUiState,
                pathUiState = pathUiState,
                onOpenChapter = { viewModel.openChapter(it) },
                onContinueReading = { chapterId, sectionId ->
                    viewModel.openChapter(chapterId)
                    viewModel.openSection(chapterId, sectionId)
                },
                onNavigateToReview = onNavigateToReview,
                onNavigateToLearningPaths = onNavigateToLearningPaths
            )
        } else {
            ReferenceGrid(
                refState = refState,
                onOpenReference = onOpenReference
            )
        }
    }
}

@Composable
private fun ModeToggleStrip(
    showingReference: Boolean,
    onSelectBook: () -> Unit,
    onSelectReference: () -> Unit
) {
    val cardShape = MaterialTheme.shapes.medium

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Spacing.MD),
        horizontalArrangement = Arrangement.spacedBy(Spacing.SM)
    ) {
        ToggleTab(
            label = "Rust Book",
            icon = Icons.Default.AutoStories,
            selected = !showingReference,
            accentColor = MaterialTheme.colorScheme.primary,
            shape = cardShape,
            onClick = onSelectBook,
            modifier = Modifier.weight(1f)
        )
        ToggleTab(
            label = "Reference",
            icon = Icons.Default.MenuBook,
            selected = showingReference,
            accentColor = MaterialTheme.colorScheme.tertiary,
            shape = cardShape,
            onClick = onSelectReference,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ToggleTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) accentColor.copy(alpha = 0.14f)
    else MaterialTheme.colorScheme.surfaceContainerHigh
    val borderMod = if (selected) Modifier.border(1.dp, accentColor.copy(alpha = Alpha.MUTED), shape)
    else Modifier
    val tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(bg)
            .then(borderMod)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$label tab" },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(Spacing.SM))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = tint
            )
        }
    }
}

@Composable
private fun BookContent(
    uiState: com.sylvester.rustsensei.viewmodel.BookUiState,
    reviewUiState: com.sylvester.rustsensei.viewmodel.ReviewUiState?,
    pathUiState: com.sylvester.rustsensei.viewmodel.PathUiState?,
    onOpenChapter: (String) -> Unit,
    onContinueReading: (chapterId: String, sectionId: String) -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToLearningPaths: () -> Unit
) {
    val cardShape = MaterialTheme.shapes.medium
    val cardColor = MaterialTheme.colorScheme.surfaceContainerHigh

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Spacing.MD)
    ) {
        // Continue reading card
        val lastChapterId = uiState.lastReadChapterId
        val lastSectionId = uiState.lastReadSectionId
        if (lastChapterId != null && lastSectionId != null) {
            item(key = "continue-reading") {
                ContinueReadingCard(
                    sectionId = lastSectionId,
                    cardShape = cardShape,
                    cardColor = cardColor,
                    onClick = { onContinueReading(lastChapterId, lastSectionId) }
                )
            }
        }

        // Review card
        if (reviewUiState != null && reviewUiState.dueCardCount > 0) {
            item(key = "review-card") {
                ReviewPromptCard(
                    dueCount = reviewUiState.dueCardCount,
                    cardShape = cardShape,
                    cardColor = cardColor,
                    onClick = onNavigateToReview
                )
            }
        }

        // Learning paths card
        if (pathUiState != null && pathUiState.paths.isNotEmpty()) {
            item(key = "learning-paths-card") {
                LearningPathPromptCard(
                    pathUiState = pathUiState,
                    cardShape = cardShape,
                    cardColor = cardColor,
                    onClick = onNavigateToLearningPaths
                )
            }
        }

        // Empty state
        if (uiState.chapters.isEmpty()) {
            item(key = "empty-chapters") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.Section),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.MUTED)
                    )
                    Spacer(modifier = Modifier.height(Spacing.LG))
                    Text(
                        text = "No chapters available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.SECONDARY)
                    )
                    Text(
                        text = "Content may still be loading",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.HINT)
                    )
                }
            }
        }

        // Chapter list
        itemsIndexed(uiState.chapters, key = { _, ch -> ch.id }) { index, chapter ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.SM)
                    .clickable { onOpenChapter(chapter.id) }
                    .semantics {
                        contentDescription =
                            "Chapter ${index + 1}: ${chapter.title}, ${chapter.sectionIds.size} sections"
                    },
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.CardPadding, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(Spacing.XXS))
                        val estimatedMinutes = (chapter.sectionIds.size * 4).coerceAtLeast(2)
                        Text(
                            text = "${chapter.sectionIds.size} sections · ~${estimatedMinutes} min",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.HINT),
                        modifier = Modifier.size(Dimens.IconSM)
                    )
                }
            }
        }

        item(key = "bottom-spacer-book") {
            Spacer(modifier = Modifier.height(Spacing.LG))
        }
    }
}

// ── Extracted card composables ──────────────────────────────────────

@Composable
private fun ContinueReadingCard(
    sectionId: String,
    cardShape: androidx.compose.ui.graphics.Shape,
    cardColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.ScreenPadding)
            .clickable(onClick = onClick),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = Alpha.MUTED),
                            MaterialTheme.colorScheme.primary.copy(alpha = Alpha.DIVIDER / 2)
                        )
                    ),
                    shape = cardShape
                )
                .padding(Dimens.CardPadding)
                .semantics {
                    contentDescription = "Continue reading: ${sectionId.replace("-", " ")}"
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Continue Reading",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(Spacing.XXS))
                Text(
                    text = sectionId.replace("-", " "),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.SECONDARY),
                modifier = Modifier.size(Dimens.IconSM)
            )
        }
    }
}

@Composable
private fun ReviewPromptCard(
    dueCount: Int,
    cardShape: androidx.compose.ui.graphics.Shape,
    cardColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.MD),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppColors.current.cyan)
            )
            Spacer(modifier = Modifier.width(Spacing.MD))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$dueCount card${if (dueCount != 1) "s" else ""} due",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap to start review",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AppColors.current.cyan,
                modifier = Modifier.size(Dimens.IconSM)
            )
        }
    }
}

@Composable
private fun LearningPathPromptCard(
    pathUiState: com.sylvester.rustsensei.viewmodel.PathUiState,
    cardShape: androidx.compose.ui.graphics.Shape,
    cardColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val activePath = pathUiState.paths.firstOrNull { path ->
        val completed = path.steps.count { step ->
            pathUiState.stepProgress["${path.id}:${step.id}"] == true
        }
        completed > 0 && completed < path.steps.size
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.MD),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AutoStories,
                contentDescription = "Learning paths",
                modifier = Modifier.size(Dimens.IconMD),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(Spacing.MD))
            Column(modifier = Modifier.weight(1f)) {
                if (activePath != null) {
                    val completedSteps = activePath.steps.count { step ->
                        pathUiState.stepProgress["${activePath.id}:${step.id}"] == true
                    }
                    Text(
                        text = activePath.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$completedSteps of ${activePath.steps.size} steps",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    Text(
                        text = "Learning Paths",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose a guided path",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.HINT),
                modifier = Modifier.size(Dimens.IconSM)
            )
        }
    }
}

@Composable
private fun ReferenceGrid(
    refState: com.sylvester.rustsensei.viewmodel.ReferenceUiState,
    onOpenReference: (String) -> Unit
) {
    val cardShape = MaterialTheme.shapes.medium
    val cardColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPadding)
    ) {
        Spacer(modifier = Modifier.height(Spacing.MD))
        Text(
            text = "Quick Reference",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = Spacing.XS)
        )
        Text(
            text = "${refState.sections.size} guides",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.SOFT),
            modifier = Modifier.padding(bottom = 14.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = Spacing.LG),
            modifier = Modifier.fillMaxSize()
        ) {
            items(refState.sections, key = { "ref-${it.id}" }) { section ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenReference(section.id) }
                        .semantics {
                            contentDescription = "${section.title}, ${section.items.size} items"
                        },
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Icon(
                            referenceSectionIcon(section.id),
                            contentDescription = section.title,
                            modifier = Modifier.size(Dimens.IconMD),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(Spacing.XXS))
                        Text(
                            text = "${section.items.size} items",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.SECONDARY)
                        )
                    }
                }
            }
        }
    }
}
