package com.sylvester.rustsensei.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.ui.theme.DarkSurfaceContainer
import com.sylvester.rustsensei.ui.theme.RustOrange
import com.sylvester.rustsensei.ui.theme.SecondaryText
import com.sylvester.rustsensei.viewmodel.SearchResult
import com.sylvester.rustsensei.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        // Search bar row with pill-shaped input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.clearSearch()
                onNavigateBack()
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            TextField(
                value = uiState.query,
                onValueChange = { viewModel.updateQuery(it) },
                placeholder = {
                    Text(
                        "Search sections, exercises, glossary...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceContainer,
                    unfocusedContainerColor = DarkSurfaceContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = RustOrange
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace
                ),
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = SecondaryText
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(20.dp),
                                tint = SecondaryText
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))
        }

        // Separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RustOrange.copy(alpha = 0.08f))
        )

        if (uiState.isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = RustOrange
                )
            }
        } else if (uiState.query.isBlank()) {
            // Show recent searches
            if (uiState.recentSearches.isNotEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = SecondaryText
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Recent Searches",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { viewModel.clearRecentSearches() }) {
                                Text(
                                    "Clear All",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RustOrange
                                )
                            }
                        }
                    }
                    items(uiState.recentSearches) { search ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectRecentSearch(search) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = SecondaryText.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = search,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                // Empty state: no recent searches
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = SecondaryText.copy(alpha = 0.25f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Search across all content",
                            style = MaterialTheme.typography.bodyLarge,
                            color = SecondaryText.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Chapters, exercises, reference guides",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        } else if (uiState.results.isEmpty()) {
            // No results found
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = SecondaryText.copy(alpha = 0.25f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No results found",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try different keywords",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            // Group results by type
            val grouped = uiState.results.groupBy { it.type }
            val typeOrder = listOf("section", "exercise", "reference", "glossary")

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (type in typeOrder) {
                    val typeResults = grouped[type] ?: continue
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = typeLabel(type),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = RustOrange,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(typeResults, key = { "${it.type}-${it.id}" }) { result ->
                        SearchResultRow(result = result)
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = RustOrange.copy(alpha = 0.06f),
                            modifier = Modifier.padding(start = 48.dp)
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: SearchResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* navigation can be added later */ }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            typeIcon(result.type),
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp),
            tint = RustOrange.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = result.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (result.matchSnippet.isNotBlank()) {
                Text(
                    text = result.matchSnippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText.copy(alpha = 0.45f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

private fun typeLabel(type: String): String = when (type) {
    "section" -> "// SECTIONS"
    "exercise" -> "// EXERCISES"
    "reference" -> "// REFERENCE"
    "glossary" -> "// GLOSSARY"
    else -> "// OTHER"
}

private fun typeIcon(type: String): ImageVector = when (type) {
    "section" -> Icons.Default.Book
    "exercise" -> Icons.Default.Code
    "reference" -> Icons.Default.LibraryBooks
    "glossary" -> Icons.Default.MenuBook
    else -> Icons.Default.Search
}
