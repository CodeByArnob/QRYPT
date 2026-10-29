package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.BarcodeFormatType
import com.example.domain.model.ParsedPayloadType
import com.example.domain.model.SecurityRiskLevel
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.LocalLiquidGlass
import com.example.ui.theme.SafeEmerald

@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLiquidGlass.current
    val bgColor = MaterialTheme.colorScheme.background

    CompositionLocalProvider(LocalContentColor provides glass.textPrimary) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(bgColor)
                .drawBehind {
                    // Restrained top-left vivid cyan ambient optical glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glass.primaryAccent.copy(alpha = if (glass.isDark) 0.14f else 0.10f),
                                glass.secondaryAccent.copy(alpha = if (glass.isDark) 0.05f else 0.03f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.15f, size.height * 0.06f),
                            radius = size.width * 0.75f
                        ),
                        radius = size.width * 0.75f,
                        center = Offset(size.width * 0.15f, size.height * 0.06f)
                    )
                    // Restrained bottom-right electric blue reflection
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glass.secondaryAccent.copy(alpha = if (glass.isDark) 0.12f else 0.08f),
                                glass.primaryAccent.copy(alpha = if (glass.isDark) 0.04f else 0.02f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.88f, size.height * 0.88f),
                            radius = size.width * 0.72f
                        ),
                        radius = size.width * 0.72f,
                        center = Offset(size.width * 0.88f, size.height * 0.88f)
                    )
                },
            content = content
        )
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    accentTint: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLiquidGlass.current
    val topFill = if (accentTint != null) {
        accentTint.copy(alpha = if (glass.isDark) 0.20f else 0.12f)
    } else {
        glass.glassSurfaceTop
    }
    val bottomFill = glass.glassSurfaceBottom

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            accentTint?.copy(alpha = 0.55f) ?: glass.glassSpecularBorderTop,
            glass.primaryAccent.copy(alpha = if (glass.isDark) 0.22f else 0.16f),
            glass.glassSpecularBorderBottom
        )
    )

    val sheenColor = if (glass.isDark) {
        Color.White.copy(alpha = 0.035f)
    } else {
        Color.White.copy(alpha = 0.45f)
    }

    val cardModifier = modifier
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(colors = listOf(topFill, bottomFill)),
            shape = shape
        )
        .drawWithContent {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(sheenColor, Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.36f
                )
            )
            drawContent()
        }
        .border(width = 1.dp, brush = borderBrush, shape = shape)
        .let { mod ->
            if (onClick != null) {
                mod.clickable(onClick = onClick)
            } else {
                mod
            }
        }
        .padding(contentPadding)

    CompositionLocalProvider(LocalContentColor provides glass.textPrimary) {
        Box(modifier = cardModifier, content = content)
    }
}

@Composable
fun LiquidGlassPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null
) {
    val glass = LocalLiquidGlass.current
    val bgBrush = if (selected) {
        Brush.horizontalGradient(
            listOf(
                glass.primaryAccent.copy(alpha = if (glass.isDark) 0.34f else 0.20f),
                glass.secondaryAccent.copy(alpha = if (glass.isDark) 0.22f else 0.14f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(glass.glassSurfaceTop, glass.glassSurfaceBottom)
        )
    }

    val borderColor by animateColorAsState(
        targetValue = if (selected) glass.primaryAccent else glass.glassSpecularBorderTop.copy(alpha = 0.45f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pillBorder"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) glass.primaryAccent else glass.textPrimary,
        label = "pillText"
    )

    val shape = CircleShape
    Row(
        modifier = modifier
            .let { if (onClick != null) it.minimumInteractiveComponentSize() else it }
            .size(width = Dp.Unspecified, height = 44.dp)
            .clip(shape)
            .background(brush = bgBrush, shape = shape)
            .border(width = if (selected) 1.5.dp else 1.dp, color = borderColor, shape = shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription ?: text,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun FormatBadge(
    format: BarcodeFormatType,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlass.current
    val badgeColor = if (format.is2D) glass.primaryAccent else glass.secondaryAccent
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = badgeColor.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.55f))
    ) {
        Text(
            text = format.shortTag,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SecurityRiskBadge(
    riskLevel: SecurityRiskLevel,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlass.current
    val (color, icon) = when (riskLevel) {
        SecurityRiskLevel.SAFE -> SafeEmerald to Icons.Default.CheckCircle
        SecurityRiskLevel.CAUTION -> CautionAmber to Icons.Default.WarningAmber
        SecurityRiskLevel.SUSPICIOUS -> CautionAmber.copy(alpha = 0.9f) to Icons.Default.WarningAmber
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = if (glass.isDark) 0.10f else 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = riskLevel.label,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = riskLevel.label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1
            )
        }
    }
}

fun iconForPayloadType(type: ParsedPayloadType): ImageVector {
    return when (type) {
        ParsedPayloadType.URL -> Icons.Default.Language
        ParsedPayloadType.WIFI -> Icons.Default.Wifi
        ParsedPayloadType.CONTACT_VCARD -> Icons.Default.ContactPage
        ParsedPayloadType.EMAIL -> Icons.Default.Email
        ParsedPayloadType.PHONE -> Icons.Default.Call
        ParsedPayloadType.SMS -> Icons.Default.Sms
        ParsedPayloadType.GEO -> Icons.Default.LocationOn
        ParsedPayloadType.CALENDAR -> Icons.Default.CalendarMonth
        ParsedPayloadType.ISBN -> Icons.Default.Book
        ParsedPayloadType.PRODUCT -> Icons.Default.Inventory2
        ParsedPayloadType.PLAIN_TEXT -> Icons.Default.TextFields
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    size: Dp = 46.dp
) {
    val glass = LocalLiquidGlass.current
    val bg = if (active) {
        glass.primaryAccent.copy(alpha = 0.28f)
    } else {
        glass.glassSurfaceTop
    }
    val border = if (active) glass.primaryAccent else glass.glassSpecularBorderTop.copy(alpha = 0.5f)
    val tint = if (active) glass.primaryAccent else glass.textPrimary

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(1.2.dp, border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}
