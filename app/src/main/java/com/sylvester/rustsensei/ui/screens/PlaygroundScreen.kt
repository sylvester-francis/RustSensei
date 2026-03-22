package com.sylvester.rustsensei.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.R
import com.sylvester.rustsensei.llm.ModelReadyState
import com.sylvester.rustsensei.ui.theme.Alpha
import com.sylvester.rustsensei.ui.theme.Dimens
import com.sylvester.rustsensei.ui.theme.Spacing
import com.sylvester.rustsensei.viewmodel.PlaygroundViewModel
import com.sylvester.rustsensei.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaygroundScreen(
    viewModel: PlaygroundViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val modelState by viewModel.modelState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.playground_title),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.ScreenPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Subtitle
            Text(
                text = stringResource(R.string.playground_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.LG)
            )

            // Code editor
            OutlinedTextField(
                value = uiState.code,
                onValueChange = { viewModel.updateCode(it) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 8,
                maxLines = 16,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.current.accent,
                    cursorColor = AppColors.current.accent
                ),
                shape = RoundedCornerShape(Dimens.CardRadius)
            )

            Spacer(modifier = Modifier.height(Spacing.LG))

            // Run / Stop buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (uiState.isRunning) viewModel.stop() else viewModel.run()
                    },
                    enabled = uiState.code.isNotBlank()
                            && modelState == ModelReadyState.READY,
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.ButtonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isRunning)
                            MaterialTheme.colorScheme.error
                        else
                            AppColors.current.accent
                    ),
                    shape = RoundedCornerShape(Dimens.CardRadius)
                ) {
                    if (uiState.isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.IconSM),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = Spacing.XXS
                        )
                        Spacer(modifier = Modifier.width(Spacing.SM))
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.IconSM)
                        )
                        Spacer(modifier = Modifier.width(Spacing.XS))
                        Text(
                            text = stringResource(R.string.playground_stop),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.IconSM)
                        )
                        Spacer(modifier = Modifier.width(Spacing.XS))
                        Text(
                            text = stringResource(R.string.playground_run),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Clear button
                if (uiState.output.isNotBlank() || uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.width(Spacing.SM))
                    OutlinedButton(
                        onClick = { viewModel.clearOutput() },
                        modifier = Modifier.height(Dimens.ButtonHeight),
                        shape = RoundedCornerShape(Dimens.CardRadius),
                        border = BorderStroke(
                            Dimens.Divider,
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.MUTED)
                        )
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.playground_clear),
                            modifier = Modifier.size(Dimens.IconSM)
                        )
                    }
                }
            }

            // Model not ready warning
            if (modelState != ModelReadyState.READY) {
                Spacer(modifier = Modifier.height(Spacing.SM))
                Text(
                    text = stringResource(R.string.playground_model_needed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = Alpha.SOFT)
                )
            }

            // Error message
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(Spacing.MD))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(Dimens.CardRadius)
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(Dimens.CardPadding)
                    )
                }
            }

            // Output console
            if (uiState.output.isNotBlank() || uiState.isRunning) {
                Spacer(modifier = Modifier.height(Spacing.XL))

                Text(
                    text = stringResource(R.string.playground_output),
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.current.accent,
                    modifier = Modifier.padding(bottom = Spacing.SM)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.CardRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    border = BorderStroke(
                        Dimens.Divider,
                        AppColors.current.accent.copy(alpha = Alpha.BORDER)
                    )
                ) {
                    Box(modifier = Modifier.padding(Dimens.CardPadding)) {
                        Text(
                            text = uiState.output.ifBlank {
                                stringResource(R.string.playground_running)
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            ),
                            color = AppColors.current.success
                        )
                    }
                }

                // Elapsed time
                if (uiState.elapsedMs > 0 && !uiState.isRunning) {
                    Spacer(modifier = Modifier.height(Spacing.XS))
                    Text(
                        text = "${uiState.elapsedMs}ms",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.SECONDARY)
                    )
                }
            }

            // Simulated output disclaimer
            Spacer(modifier = Modifier.height(Spacing.LG))
            Text(
                text = stringResource(R.string.playground_simulated_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.HINT)
            )

            Spacer(modifier = Modifier.height(Spacing.XXXL))
        }
    }
}
