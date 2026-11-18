package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.NoFood
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WorkOutline
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalPulseColors

@Composable
fun PulseCard(
    modifier: Modifier = Modifier,
    containerColor: Color = LocalPulseColors.current.cardBackground,
    borderColor: Color = LocalPulseColors.current.cardBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
fun PulseLogoMark(
    modifier: Modifier = Modifier,
    color: Color = LocalPulseColors.current.primaryAccent,
    strokeWidth: Dp = 4.dp
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, h * 0.55f)
            lineTo(w * 0.28f, h * 0.55f)
            lineTo(w * 0.42f, h * 0.14f)
            lineTo(w * 0.54f, h * 0.88f)
            lineTo(w * 0.67f, h * 0.38f)
            lineTo(w * 0.76f, h * 0.55f)
            lineTo(w, h * 0.55f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
fun CircularProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 144.dp,
    strokeWidth: Dp = 11.dp,
    activeColor: Color = LocalPulseColors.current.primaryAccent,
    trackColor: Color = LocalPulseColors.current.cardBorder,
    centerContent: @Composable () -> Unit
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 550),
        label = "ringProgress"
    )
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (animatedProgress > 0f) {
                drawArc(
                    color = activeColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        centerContent()
    }
}

@Composable
fun PulseEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionButton: (@Composable () -> Unit)? = null
) {
    val colors = LocalPulseColors.current
    PulseCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.primaryAccent.copy(alpha = 0.14f))
                    .border(1.dp, colors.primaryAccent.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            if (actionButton != null) {
                Spacer(Modifier.height(16.dp))
                actionButton()
            }
        }
    }
}

@Composable
fun HabitIconBadge(
    iconKey: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 44.dp
) {
    val accent = parseColorHex(colorHex)
    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.18f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = habitIconForKey(iconKey),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(badgeSize * 0.52f)
        )
    }
}

data class HabitIconOption(val key: String, val icon: ImageVector)

val pulseHabitIconOptions = listOf(
    HabitIconOption("book", Icons.AutoMirrored.Rounded.MenuBook),
    HabitIconOption("fitness", Icons.Rounded.FitnessCenter),
    HabitIconOption("run", Icons.AutoMirrored.Rounded.DirectionsRun),
    HabitIconOption("water", Icons.Rounded.WaterDrop),
    HabitIconOption("sleep", Icons.Rounded.Bedtime),
    HabitIconOption("mindfulness", Icons.Rounded.SelfImprovement),
    HabitIconOption("work", Icons.Rounded.WorkOutline),
    HabitIconOption("code", Icons.Rounded.Code),
    HabitIconOption("no_phone", Icons.Rounded.PhoneDisabled),
    HabitIconOption("block", Icons.Rounded.Block),
    HabitIconOption("no_sugar", Icons.Rounded.NoFood),
    HabitIconOption("food", Icons.Rounded.Fastfood),
    HabitIconOption("drink", Icons.Rounded.LocalDrink),
    HabitIconOption("finance", Icons.Rounded.AccountBalanceWallet),
    HabitIconOption("social", Icons.Rounded.Groups),
    HabitIconOption("check", Icons.Rounded.CheckCircle),
    HabitIconOption("star", Icons.Rounded.Star)
)

val pulseHabitColorPalette = listOf(
    "#27C6D9",
    "#35C98B",
    "#F07878",
    "#8B7CF6",
    "#F5B74C",
    "#3B82F6",
    "#EC4899",
    "#14B8A6"
)

fun habitIconForKey(key: String): ImageVector =
    pulseHabitIconOptions.firstOrNull { it.key == key }?.icon ?: Icons.Rounded.CheckCircle

fun parseColorHex(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000L or value)
            8 -> Color(value)
            else -> Color(0xFF27C6D9)
        }
    } catch (_: Exception) {
        Color(0xFF27C6D9)
    }
}

@Composable
fun SectionHeaderRow(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val colors = LocalPulseColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        if (actionLabel != null && onActionClick != null) {
            androidx.compose.material3.TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryAccent
                )
            }
        }
    }
}
