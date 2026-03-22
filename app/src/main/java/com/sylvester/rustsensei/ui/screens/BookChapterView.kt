package com.sylvester.rustsensei.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sylvester.rustsensei.ui.theme.Alpha
import com.sylvester.rustsensei.ui.theme.Dimens
import com.sylvester.rustsensei.ui.theme.Spacing
import com.sylvester.rustsensei.viewmodel.BookViewModel
import com.sylvester.rustsensei.ui.theme.AppColors

@Composable
internal fun BookChapterView(viewModel: BookViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val chapter = uiState.currentChapter ?: return
    val cardShape = MaterialTheme.shapes.medium
    val cardColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(modifier = Modifier.fillMaxSize()) {
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
                text = chapter.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Dimens.ScreenPadding),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.DIVIDER / 2)
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Spacing.SM),
            modifier = Modifier.fillMaxSize()
        ) {
            items(chapter.sections, key = { it.id }) { section ->
                val progress = uiState.sectionProgress[section.id]
                val isCompleted = progress?.isCompleted == true
                val hasProgress = progress != null && !isCompleted && progress.readPercent > 0f
                val chapterId = uiState.currentChapterId

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.XS)
                        .clickable {
                            chapterId?.let { viewModel.openSection(it, section.id) }
                        }
                        .semantics {
                            contentDescription = when {
                                isCompleted -> "${section.title}, completed"
                                hasProgress -> "${section.title}, in progress"
                                else -> section.title
                            }
                        },
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.CardPadding, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isCompleted) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = AppColors.current.success,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.HINT),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Spacing.MD))
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (hasProgress && progress != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { progress.readPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                strokeCap = StrokeCap.Round,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "chapter-bottom-spacer") {
                Spacer(modifier = Modifier.height(Spacing.LG))
            }
        }
    }
}
