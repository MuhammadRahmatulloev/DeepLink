package com.deeplink.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun ScreenEnterAnimation(content: @Composable () -> Unit) {
    androidx.compose.animation.AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(320)) + scaleIn(initialScale = 0.98f),
        label = "screen_enter"
    ) {
        content()
    }
}

@Composable
fun GradientTitle(text: String, modifier: Modifier = Modifier) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    )
    val styledTitle = buildAnnotatedString {
        withStyle(SpanStyle(brush = gradient)) {
            append(text)
        }
    }
    Text(
        text = styledTitle,
        style = MaterialTheme.typography.headlineLarge,
        modifier = modifier
    )
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun AnimatedPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.98f,
        label = "button_scale"
    )
    Button(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .height(54.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
        AnimatedContent(targetState = isLoading, label = "button_loading") { loading ->
            if (loading) {
                Text("Loading...")
            } else {
                Text(text)
            }
        }
    }
}

@Composable
fun EmptyStateIllustration(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun SkeletonBlock(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.44f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
    )
}

@Composable
fun StatusChip(status: String) {
    val normalized = status.lowercase()
    val color = when {
        normalized.contains("done") || normalized.contains("completed") ->
            Color(0xFF2ECC71)
        normalized.contains("pending") || normalized.contains("progress") ->
            Color(0xFFF5A623)
        normalized.contains("fail") || normalized.contains("error") ->
            Color(0xFFE74C3C)
        else -> MaterialTheme.colorScheme.primary
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(status) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.18f),
            disabledContainerColor = color.copy(alpha = 0.18f),
            labelColor = color,
            disabledLabelColor = color
        )
    )
}
