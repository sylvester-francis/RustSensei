package com.sylvester.rustsensei.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.data.Conversation
import com.sylvester.rustsensei.ui.components.InputBar
import com.sylvester.rustsensei.ui.components.MessageBubble
import com.sylvester.rustsensei.ui.components.QuickPromptChips
import com.sylvester.rustsensei.ui.components.StreamingIndicator
import com.sylvester.rustsensei.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
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
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("RustSensei") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Conversations")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.startNewConversation() }) {
                            Icon(Icons.Default.Add, contentDescription = "New conversation")
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                // Offline badge
                Text(
                    text = "Offline Mode \u2014 No data leaves your device",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

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
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
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
}

@Composable
private fun EmptyState(onPromptSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "\uD83E\uDD80",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome to RustSensei",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your offline Rust programming tutor.\nAsk me anything about Rust!",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        val suggestions = listOf(
            "I'm new to Rust. Where do I start?",
            "Explain ownership like I'm a Go developer",
            "Help me understand the borrow checker"
        )
        suggestions.forEach { suggestion ->
            androidx.compose.material3.OutlinedButton(
                onClick = { onPromptSelected(suggestion) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(suggestion, fontSize = 13.sp)
            }
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
    ModalDrawerSheet {
        Text(
            text = "Conversations",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        HorizontalDivider()
        NavigationDrawerItem(
            label = { Text("New Conversation") },
            selected = false,
            onClick = onNewConversation,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        conversations.forEach { conversation ->
            NavigationDrawerItem(
                label = {
                    Text(
                        text = conversation.title,
                        maxLines = 1
                    )
                },
                selected = conversation.id == currentConversationId,
                onClick = { onConversationSelected(conversation.id) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}
