package com.sylvester.rustsensei.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.sylvester.rustsensei.ui.theme.NeonCyan
import com.sylvester.rustsensei.ui.theme.RustOrange
import com.sylvester.rustsensei.ui.theme.Spacing
import com.sylvester.rustsensei.viewmodel.ExplainErrorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplainErrorScreen(
    viewModel: ExplainErrorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val modelState by viewModel.modelState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.explain_error_title),
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
                text = stringResource(R.string.explain_error_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.LG)
            )

            // Error code input
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = { viewModel.updateInput(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.explain_error_input_label)) },
                placeholder = { Text(stringResource(R.string.explain_error_input_hint)) },
                minLines = 4,
                maxLines = 8,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RustOrange,
                    cursorColor = RustOrange
                ),
                shape = RoundedCornerShape(Dimens.CardRadius)
            )

            // Detected error code badge
            if (uiState.detectedCode != null) {
                Spacer(modifier = Modifier.height(Spacing.SM))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = NeonCyan.copy(alpha = Alpha.BORDER)
                    ),
                    shape = RoundedCornerShape(Spacing.SM)
                ) {
                    Text(
                        text = stringResource(R.string.explain_error_detected, uiState.detectedCode!!),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan,
                        modifier = Modifier.padding(
                            horizontal = Spacing.MD,
                            vertical = Spacing.XS
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.LG))

            // Explain button
            Button(
                onClick = { viewModel.explain() },
                enabled = uiState.inputText.isNotBlank()
                        && !uiState.isExplaining
                        && modelState == ModelReadyState.READY,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ButtonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RustOrange
                ),
                shape = RoundedCornerShape(Dimens.CardRadius)
            ) {
                if (uiState.isExplaining) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.IconSM),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = Spacing.XXS
                    )
                    Spacer(modifier = Modifier.width(Spacing.SM))
                }
                Text(
                    text = if (uiState.isExplaining) stringResource(R.string.explain_error_explaining)
                           else stringResource(R.string.explain_error_button),
                    fontWeight = FontWeight.Bold
                )
            }

            // Model not ready warning
            if (modelState != ModelReadyState.READY) {
                Spacer(modifier = Modifier.height(Spacing.SM))
                Text(
                    text = stringResource(R.string.explain_error_model_needed),
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

            // Explanation result
            if (uiState.explanationText.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.XL))

                Text(
                    text = stringResource(R.string.explain_error_result_header),
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = RustOrange,
                    modifier = Modifier.padding(bottom = Spacing.SM)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.CardRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = BorderStroke(
                        Dimens.Divider,
                        RustOrange.copy(alpha = Alpha.BORDER)
                    )
                ) {
                    Text(
                        text = uiState.explanationText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(Dimens.CardPadding)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.XXXL))
        }
    }
}
