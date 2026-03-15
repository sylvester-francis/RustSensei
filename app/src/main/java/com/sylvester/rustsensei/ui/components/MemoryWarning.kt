package com.sylvester.rustsensei.ui.components

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.sylvester.rustsensei.llm.ModelInfo

@Composable
fun MemoryWarningDialog(
    model: ModelInfo,
    onProceed: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Low Memory Warning", color = MaterialTheme.colorScheme.error) },
        text = {
            Text(
                "${model.displayName} requires ${model.ramRequired}. " +
                "Your device may not have enough free memory. " +
                "The app could become slow or crash. Consider using a smaller model."
            )
        },
        confirmButton = {
            TextButton(onClick = onProceed) { Text("Load Anyway") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun isMemoryLow(context: Context, model: ModelInfo): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        ?: return false
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)

    val deviceMemGb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        memoryInfo.advertisedMem / (1024f * 1024 * 1024)
    } else {
        memoryInfo.totalMem / (1024f * 1024 * 1024)
    }

    // Parse the required RAM from model.ramRequired (e.g., "~4 GB RAM")
    val requiredGb = model.ramRequired
        .replace("~", "")
        .replace("GB RAM", "")
        .trim()
        .toFloatOrNull() ?: return false

    return deviceMemGb < requiredGb
}
