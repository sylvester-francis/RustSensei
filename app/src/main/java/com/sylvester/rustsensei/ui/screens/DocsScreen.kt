package com.sylvester.rustsensei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.R
import com.sylvester.rustsensei.content.DocEntry
import com.sylvester.rustsensei.content.DocIndexEntry
import com.sylvester.rustsensei.content.DocMethod
import com.sylvester.rustsensei.ui.components.CodeBlock
import com.sylvester.rustsensei.ui.theme.Alpha
import com.sylvester.rustsensei.ui.theme.Dimens
import com.sylvester.rustsensei.ui.theme.Spacing
import com.sylvester.rustsensei.viewmodel.DocsViewModel
import com.sylvester.rustsensei.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocsScreen(
    viewModel: DocsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.currentDoc != null) {
        viewModel.goBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.currentDoc?.typeName
                            ?: stringResource(R.string.docs_title),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.currentDoc != null) {
                                viewModel.goBack()
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (uiState.currentDoc != null) {
            DocDetailView(
                doc = uiState.currentDoc!!,
                modifier = Modifier.padding(padding)
            )
        } else {
            DocIndexView(
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun DocIndexView(
    viewModel: DocsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding)
    ) {
        item {
            Text(
                text = stringResource(R.string.docs_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.MD)
            )
        }

        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.search(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.LG),
                placeholder = {
                    Text(stringResource(R.string.docs_search_hint))
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.current.accent,
                    cursorColor = AppColors.current.accent
                ),
                shape = RoundedCornerShape(Dimens.CardRadius)
            )
        }

        if (uiState.filteredIndex.isEmpty() && !uiState.isLoading) {
            item {
                Text(
                    text = stringResource(R.string.docs_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.XXL)
                )
            }
        }

        items(uiState.filteredIndex, key = { it.id }) { entry ->
            DocTypeCard(
                entry = entry,
                onClick = { viewModel.openDoc(entry.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(Spacing.XXXL))
        }
    }
}

@Composable
private fun DocTypeCard(
    entry: DocIndexEntry,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.XS),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AppColors.current.cyan
                )
                Spacer(modifier = Modifier.height(Spacing.XXS))
                Text(
                    text = entry.module,
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

@Composable
private fun DocDetailView(
    doc: DocEntry,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding)
    ) {
        // Signature
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.LG),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.CardPadding)
                ) {
                    Text(
                        text = doc.module,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.SM))
                    Text(
                        text = doc.signature,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.current.cyan
                    )
                    Spacer(modifier = Modifier.height(Spacing.MD))
                    Text(
                        text = doc.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Methods header
        item {
            Text(
                text = stringResource(R.string.docs_methods),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.current.accent,
                modifier = Modifier.padding(bottom = Spacing.SM)
            )
        }

        // Method list
        items(doc.methods, key = { it.name }) { method ->
            MethodItem(method = method)
        }

        item {
            Spacer(modifier = Modifier.height(Spacing.XXXL))
        }
    }
}

@Composable
private fun MethodItem(method: DocMethod) {
    var expanded by rememberSaveable(method.name) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.XS),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column {
            // Clickable header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(Dimens.CardPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = method.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.XXS))
                    Text(
                        text = method.signature,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = AppColors.current.cyan,
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.SM))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.SECONDARY),
                    modifier = Modifier.size(Dimens.IconMD)
                )
            }

            // Expandable detail
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = Dimens.CardPadding,
                        end = Dimens.CardPadding,
                        bottom = Dimens.CardPadding
                    )
                ) {
                    HorizontalDivider(
                        thickness = Dimens.Divider,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.BORDER),
                        modifier = Modifier.padding(bottom = Spacing.MD)
                    )

                    Text(
                        text = method.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(Spacing.MD))

                    Text(
                        text = stringResource(R.string.docs_example),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.current.accent,
                        modifier = Modifier.padding(bottom = Spacing.SM)
                    )

                    CodeBlock(
                        code = method.example,
                        language = "rust"
                    )
                }
            }
        }
    }
}
