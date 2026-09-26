package com.ministerio.jovenes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Indigo = Color(0xFF4F46E5)
val DeepIndigo = Color(0xFF312E81)
val Teal = Color(0xFF0D9488)
val Amber = Color(0xFFF59E0B)
val Coral = Color(0xFFF43F5E)
val Slate = Color(0xFF334155)

private val LightColors = lightColorScheme(
    primary=Indigo, onPrimary=Color.White, primaryContainer=Color(0xFFE0E7FF), onPrimaryContainer=DeepIndigo,
    secondary=Teal, onSecondary=Color.White, secondaryContainer=Color(0xFFCCFBF1),
    tertiary=Amber, background=Color(0xFFF8FAFC), surface=Color.White,
    surfaceVariant=Color(0xFFF1F5F9), onSurface=Color(0xFF0F172A), error=Coral
)
private val DarkColors = darkColorScheme(
    primary=Color(0xFFA5B4FC), primaryContainer=DeepIndigo, secondary=Color(0xFF5EEAD4),
    tertiary=Color(0xFFFCD34D), background=Color(0xFF0F172A), surface=Color(0xFF1E293B)
)

@Composable fun MinistryTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=if(isSystemInDarkTheme()) DarkColors else LightColors,
        typography=Typography(
            headlineLarge=MaterialTheme.typography.headlineLarge.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.Bold),
            headlineMedium=MaterialTheme.typography.headlineMedium.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.Bold),
            titleLarge=MaterialTheme.typography.titleLarge.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
        ), content=content)
}
