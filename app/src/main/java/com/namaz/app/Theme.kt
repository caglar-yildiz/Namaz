package com.namaz.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Single source of truth for the app palette; screens must not redeclare these colours.
internal val Emerald = Color(0xFF14594B)
internal val DarkEmerald = Color(0xFF103E36)
internal val Gold = Color(0xFFD7B56D)
internal val Cream = Color(0xFFF8F6F0)
internal val Mint = Color(0xFFE8F2EB)
internal val Ink = Color(0xFF203B34)
/** Secondary text on dark emerald surfaces. */
internal val OnDarkMuted = Color(0xFFDDEAE4)

private val NamazColors = lightColorScheme(
    primary = Emerald, onPrimary = Color.White, secondary = Gold, onSecondary = DarkEmerald,
    background = Cream, onBackground = Ink, surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFE8F0EB), onSurfaceVariant = Color(0xFF3F5A52),
    surfaceContainerLow = Color.White, surfaceContainer = Color.White,
    primaryContainer = Color(0xFFDDEDE5), onPrimaryContainer = DarkEmerald,
    secondaryContainer = Color(0xFFF3E7C8), onSecondaryContainer = DarkEmerald,
    outline = Color(0xFFB8C8BE), outlineVariant = Color(0xFFDCE5DF)
)

private val NamazShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp)
)

@Composable
internal fun NamazTheme(content: @Composable () -> Unit) {
    val base = Typography()
    MaterialTheme(
        colorScheme = NamazColors,
        shapes = NamazShapes,
        typography = base.copy(
            headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        ),
        content = content
    )
}

/** Top-level tab title with an optional muted subtitle. */
@Composable
internal fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(Modifier.padding(top = 4.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        if (subtitle != null) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Heading that separates groups of content inside a screen. */
@Composable
internal fun SectionTitle(title: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        trailing?.invoke()
    }
}

@Composable
internal fun ChevronIcon(tint: Color = Emerald) {
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = tint)
}

@Composable
internal fun BackIcon() {
    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
}
