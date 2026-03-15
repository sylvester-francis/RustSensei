package com.sylvester.rustsensei.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.data.Conversation
import com.sylvester.rustsensei.ui.components.InputBar
import com.sylvester.rustsensei.ui.components.MessageBubble
import com.sylvester.rustsensei.ui.components.QuickPromptChips
import com.sylvester.rustsensei.ui.components.StreamingIndicator
import com.sylvester.rustsensei.viewmodel.ChatContext
import com.sylvester.rustsensei.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val chatContext by viewModel.chatContext.collectAsState()
    val conversations by viewModel.getConversations().collectAsState(initial = emptyList())
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(uiState.messages.size, uiState.streamingText) {
        if (uiState.messages.isNotEmpty() || uiState.streamingText.isNotEmpty()) {
            val targetIndex = uiState.messages.size +
                    (if (uiState.streamingText.isNotEmpty()) 1 else 0) +
                    (if (uiState.isGenerating && uiState.streamingText.isEmpty()) 1 else 0)
            if (targetIndex > 0) {
                listState.animateScrollToItem(targetIndex - 1)
            }
        }
    }

    // Start a new conversation if none exists
    LaunchedEffect(Unit) {
        if (uiState.currentConversationId == null) {
            viewModel.startNewConversation()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationDrawer(
                conversations = conversations,
                currentConversationId = uiState.currentConversationId,
                onConversationSelected = { convId ->
                    viewModel.loadConversation(convId)
                    scope.launch { drawerState.close() }
                },
                onNewConversation = {
                    viewModel.startNewConversation()
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Conversation controls row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Conversations",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))

                // Context indicator as a proper chip/pill
                when (chatContext) {
                    is ChatContext.BookSection -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Book context",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    is ChatContext.Exercise -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Code,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Exercise context",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    else -> {}
                }

                IconButton(onClick = { viewModel.startNewConversation() }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New conversation",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Messages list
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (uiState.messages.isEmpty() && !uiState.isGenerating) {
                    item {
                        EmptyState(onPromptSelected = { prompt ->
                            inputText = prompt
                        })
                    }
                }

                items(uiState.messages, key = { it.id }) { message ->
                    MessageBubble(
                        content = message.content,
                        isUser = message.role == "user",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Streaming response
                if (uiState.streamingText.isNotEmpty()) {
                    item {
                        MessageBubble(
                            content = uiState.streamingText,
                            isUser = false,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                // Typing indicator
                if (uiState.isGenerating && uiState.streamingText.isEmpty()) {
                    item {
                        StreamingIndicator(
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                // Inference time
                if (!uiState.isGenerating && uiState.inferenceTimeMs > 0) {
                    item {
                        Text(
                            text = "Generated in ${uiState.inferenceTimeMs / 1000.0}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.padding(start = 40.dp, top = 2.dp)
                        )
                    }
                }
            }

            // Quick prompts (show when no messages)
            if (uiState.messages.isEmpty() && !uiState.isGenerating) {
                QuickPromptChips(
                    onPromptSelected = { prompt ->
                        inputText = prompt
                    }
                )
            }

            // Input bar
            InputBar(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                onStop = { viewModel.stopGeneration() },
                isGenerating = uiState.isGenerating
            )
        }
    }
}

@Composable
private fun EmptyState(onPromptSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Splash-style crab with gradient background
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(60.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\uD83E\uDD80",
                fontSize = 56.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Welcome to RustSensei",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your offline Rust programming tutor.\nAsk me anything about Rust!",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(28.dp))

        // Suggestion chips with icons
        val suggestions = listOf(
            Triple("I'm new to Rust. Where do I start?", Icons.Default.AutoStories, "Start"),
            Triple("Explain ownership like I'm a Go developer", Icons.Default.Psychology, "Ownership"),
            Triple("Help me understand the borrow checker", Icons.Default.EmojiObjects, "Borrow checker")
        )
        suggestions.forEach { (suggestion, icon, _) ->
            AssistChip(
                onClick = { onPromptSelected(suggestion) },
                label = {
                    Text(
                        suggestion,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun ConversationDrawer(
    conversations: List<Conversation>,
    currentConversationId: Long?,
    onConversationSelected: (Long) -> Unit,
    onNewConversation: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = "Conversations",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        NavigationDrawerItem(
            label = { Text("New Conversation", style = MaterialTheme.typography.bodyLarge) },
            selected = false,
            onClick = onNewConversation,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        conversations.forEach { conversation ->
            NavigationDrawerItem(
                label = {
                    Text(
                        text = conversation.title,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                selected = conversation.id == currentConversationId,
                onClick = { onConversationSelected(conversation.id) },
                modifier = Modifier.padding(horizontal = 12.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
