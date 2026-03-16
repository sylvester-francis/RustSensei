package com.sylvester.rustsensei.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.data.Conversation
import com.sylvester.rustsensei.ui.components.InputBar
import com.sylvester.rustsensei.ui.components.MessageBubble
import com.sylvester.rustsensei.ui.components.StreamingIndicator
import com.sylvester.rustsensei.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToSetup: () -> Unit = {}
) {
    val modelLoaded = viewModel.isAnyModelLoaded()

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
                    (if (uiState.isGenerating && uiState.streamingText.isEmpty()) 1 else 0) +
                    (if (!uiState.isGenerating && uiState.inferenceTimeMs > 0) 1 else 0)
            if (targetIndex > 0) {
                listState.animateScrollToItem(targetIndex - 1)
            }
        }
    }

    // Start a new conversation ONLY if none has ever been created.
    // Key on currentConversationId so this only fires once when it's null,
    // not every time the Chat tab is re-entered.
    val convId = uiState.currentConversationId
    LaunchedEffect(convId) {
        if (convId == null) {
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
            // Clean top row: hamburger + new conversation — 12dp vertical padding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (!modelLoaded) {
                    // No model — show download prompt
                    item {
                        NoModelState(onDownload = onNavigateToSetup)
                    }
                } else if (uiState.messages.isEmpty() && !uiState.isGenerating) {
                    item {
                        EmptyState(onPromptSelected = { prompt ->
                            inputText = prompt
                        })
                    }
                }

                items(uiState.messages, key = { it.id }) { message ->
                    val messageIndex = uiState.messages.indexOf(message)
                    val isUser = message.role == "user"
                    val isFirstInGroup = messageIndex == 0 ||
                            uiState.messages[messageIndex - 1].role != message.role

                    MessageBubble(
                        content = message.content,
                        isUser = isUser,
                        isFirstInGroup = isFirstInGroup,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Streaming response
                if (uiState.streamingText.isNotEmpty()) {
                    item {
                        MessageBubble(
                            content = uiState.streamingText,
                            isUser = false,
                            isFirstInGroup = true,
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

                // Inference stats — monospace, primary at 40% alpha
                if (!uiState.isGenerating && uiState.inferenceTimeMs > 0) {
                    item {
                        val statsText = buildString {
                            append("%.1fs".format(uiState.inferenceTimeMs / 1000.0))
                            if (uiState.lastDecodeTokPerSec > 0f) {
                                append(" \u00B7 %.0f tok/s".format(uiState.lastDecodeTokPerSec))
                            }
                        }
                        Text(
                            text = statsText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }
            }

            // Input bar — only show when model is loaded
            if (modelLoaded) {
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
            .padding(top = 80.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large 80sp crab emoji
        Text(
            text = "\uD83E\uDD80",
            fontSize = 80.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // headlineSmall (monospace via theme)
        Text(
            text = "Ask me anything about Rust",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "I'll explain things like you're coming from Python",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Suggestion buttons — Python-specific, sharp 8dp corners, thin primary border at 20% alpha
        val suggestions = listOf(
            "How is Rust's ownership different from Python's GC?",
            "Show me Rust's match vs Python's match",
            "What's the Rust equivalent of a Python dict?"
        )
        suggestions.forEach { suggestion ->
            OutlinedButton(
                onClick = { onPromptSelected(suggestion) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                )
            ) {
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun NoModelState(onDownload: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "\uD83E\uDD80", fontSize = 80.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Your tutor needs a brain",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Download a small AI model to enable chat.\nAll other features work without it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDownload,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                "Download Model",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
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
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
        NavigationDrawerItem(
            label = { Text("New Conversation", style = MaterialTheme.typography.bodyLarge) },
            selected = false,
            onClick = onNewConversation,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
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
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}
