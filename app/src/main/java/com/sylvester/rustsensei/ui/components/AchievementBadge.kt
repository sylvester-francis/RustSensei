package com.sylvester.rustsensei.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.ui.theme.PrimaryGlow

@Composable
fun AchievementBadge(
    icon: String,
    title: String,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val badgeAlpha = if (isUnlocked) 1f else 0.6f
    val badgeBackground = if (isUnlocked)
        MaterialTheme.colorScheme.surfaceContainerHigh
    else
        MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .semantics {
                contentDescription = "$title: ${if (isUnlocked) "unlocked" else "locked"}"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .alpha(badgeAlpha)
                .then(
                    if (isUnlocked) {
                        Modifier.shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            spotColor = PrimaryGlow,
                            ambientColor = PrimaryGlow
                        )
                    } else {
                        Modifier
                    }
                )
                .clip(CircleShape)
                .background(badgeBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 28.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = if (isUnlocked)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
