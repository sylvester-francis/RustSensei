package com.sylvester.rustsensei.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.data.Conversation
import com.sylvester.rustsensei.ui.components.InputBar
import com.sylvester.rustsensei.ui.components.MessageBubble
import com.sylvester.rustsensei.ui.components.StreamingIndicator
import com.sylvester.rustsensei.viewmodel.ChatContext
import com.sylvester.rustsensei.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToSetup: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val modelLoaded by viewModel.modelLoaded.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val chatContext by viewModel.chatContext.collectAsState()
    val conversations by viewModel.getConversations().collectAsState(initial = emptyList())
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Auto-scroll to bottom on new messages
    LaunchedEffect(uiState.messages.size, uiState.streamingText, uiState.followUpSuggestions) {
        if (uiState.messages.isNotEmpty() || uiState.streamingText.isNotEmpty()) {
            val hasBanner = chatContext !is ChatContext.General
            val targetIndex = (if (hasBanner) 1 else 0) +
                    uiState.messages.size +
                    (if (uiState.streamingText.isNotEmpty()) 1 else 0) +
                    (if (uiState.isGenerating && uiState.streamingText.isEmpty()) 1 else 0) +
                    (if (!uiState.isGenerating && uiState.inferenceTimeMs > 0) 1 else 0) +
                    (if (!uiState.isGenerating && uiState.followUpSuggestions.isNotEmpty() &&
                        uiState.messages.isNotEmpty() && uiState.messages.last().role == "assistant") 1 else 0)
            if (targetIndex > 0) {
                listState.animateScrollToItem(targetIndex - 1)
            }
        }
    }

    // Start a new conversation ONLY if none has ever been created.
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
                onConversationSelected = { id ->
                    viewModel.loadConversation(id)
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
            // Compact top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onNavigateBack != null) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Conversations",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Rust Sensei",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                if (uiState.messages.isNotEmpty()) {
                    IconButton(onClick = {
                        val shareText = viewModel.exportConversation()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share conversation")
                        context.startActivity(shareIntent)
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share conversation",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(onClick = { viewModel.startNewConversation() }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New conversation",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            )

            // Messages list
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Context indicator banner
                when (val ctx = chatContext) {
                    is ChatContext.BookSection -> {
                        item(key = "context_banner") {
                            ContextBanner(
                                text = "\uD83D\uDCD6 Answering with context from: ${ctx.sectionId}",
                                onDismiss = { viewModel.clearChatContext() }
                            )
                        }
                    }
                    is ChatContext.Exercise -> {
                        item(key = "context_banner") {
                            ContextBanner(
                                text = "\uD83D\uDCBB Helping with exercise: ${ctx.exerciseId}",
                                onDismiss = { viewModel.clearChatContext() }
                            )
                        }
                    }
                    is ChatContext.General -> { /* no banner for general context */ }
                }

                if (!modelLoaded) {
                    // No model -- show download prompt
                    item(key = "no_model") {
                        NoModelState(onDownload = onNavigateToSetup)
                    }
                } else if (uiState.messages.isEmpty() && !uiState.isGenerating) {
                    item(key = "welcome") {
                        WelcomeState(onPromptSelected = { prompt ->
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
                    item(key = "streaming") {
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
                    item(key = "typing_indicator") {
                        StreamingIndicator(
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                // Inference stats -- monospace, primary at 65% alpha
                if (!uiState.isGenerating && uiState.inferenceTimeMs > 0) {
                    item(key = "inference_stats") {
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
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                // Follow-up suggestion chips
                if (!uiState.isGenerating && uiState.followUpSuggestions.isNotEmpty() &&
                    uiState.messages.isNotEmpty() && uiState.messages.last().role == "assistant"
                ) {
                    item(key = "follow_ups") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.followUpSuggestions.forEach { suggestion ->
                                AssistChip(
                                    onClick = {
                                        viewModel.sendMessage(suggestion)
                                    },
                                    label = {
                                        Text(
                                            text = suggestion,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontSize = 12.sp
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    ),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Input bar -- only show when model is loaded
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

/**
 * Welcome / empty state shown when no messages exist and the model is loaded.
 * Displays a crab avatar, intro text, privacy badge, and quick-prompt suggestion buttons.
 */
@Composable
private fun WelcomeState(onPromptSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 56sp crab emoji avatar — larger for visual weight
        Text(
            text = "\uD83E\uDD80",
            fontSize = 56.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // headlineSmall (monospace via theme typography)
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

        Spacer(modifier = Modifier.height(6.dp))

        // Privacy badge -- labelSmall mono, primary at 50%
        Text(
            text = "Offline \u00B7 Private \u00B7 On-device AI",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 3 suggestion outlined buttons — taller for better touch targets
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
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
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

/**
 * State shown when no AI model has been downloaded yet.
 * Displays the crab avatar, explanation text, and a full-width download button.
 */
@Composable
private fun NoModelState(onDownload: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Crab emoji — larger for visual weight
        Text(text = "\uD83E\uDD80", fontSize = 56.sp)

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

        Spacer(modifier = Modifier.height(28.dp))

        // Primary filled, full-width, 56dp pill shape
        Button(
            onClick = onDownload,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                Icons.Default.Download,
                contentDescription = "Download model",
                modifier = Modifier.padding(end = 10.dp)
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

/**
 * Context banner displayed when the chat has contextual information
 * from a book section or exercise. Uses monospace labelSmall in primary color,
 * surfaceContainer background with 1dp primary border at 15%.
 */
@Composable
private fun ContextBanner(
    text: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Dismiss context",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Conversation history drawer using ModalDrawerSheet.
 * Shows a title, new conversation item, divider, and list of existing conversations.
 */
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
        // "Conversations" title
        Text(
            text = "Conversations",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(16.dp)
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))

        // New Conversation item
        NavigationDrawerItem(
            label = {
                Text(
                    "New Conversation",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            },
            selected = false,
            onClick = onNewConversation,
            icon = {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "New conversation",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.padding(horizontal = 12.dp),
            shape = RoundedCornerShape(8.dp)
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        )

        // List of existing conversations
        conversations.forEach { conversation ->
            NavigationDrawerItem(
                label = {
                    Text(
                        text = conversation.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
