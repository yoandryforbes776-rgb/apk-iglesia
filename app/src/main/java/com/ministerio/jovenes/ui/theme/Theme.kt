package com.ministerio.jovenes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

val Indigo = Color(0xFF4F46E5)
val DeepIndigo = Color(0xFF312E81)
val Teal = Color(0xFF0D9488)
val Amber = Color(0xFFF59E0B)
val Coral = Color(0xFFF43F5E)
val Slate = Color(0xFF334155)

private val LightColors = lightColorScheme(
    primary=Indigo, onPrimary=Color.White, primaryContainer=Color(0xFFE5E7FF), onPrimaryContainer=DeepIndigo,
    secondary=Teal, onSecondary=Color.White, secondaryContainer=Color(0xFFCCFBF1), onSecondaryContainer=Color(0xFF064E3B),
    tertiary=Amber, tertiaryContainer=Color(0xFFFEF3C7), onTertiaryContainer=Color(0xFF78350F),
    background=Color(0xFFF7F8FC), surface=Color.White, surfaceVariant=Color(0xFFF0F2F8),
    onSurface=Color(0xFF111827), onSurfaceVariant=Color(0xFF5B6475), outline=Color(0xFFCBD1DC), error=Coral
)
private val DarkColors = darkColorScheme(
    primary=Color(0xFFA5B4FC), primaryContainer=DeepIndigo, secondary=Color(0xFF5EEAD4),
    tertiary=Color(0xFFFCD34D), background=Color(0xFF0C1220), surface=Color(0xFF182033),
    surfaceVariant=Color(0xFF252F43), onSurface=Color(0xFFF8FAFC), onSurfaceVariant=Color(0xFFCBD5E1)
)

private val MinistryTypography = Typography(
    displaySmall=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.ExtraBold,fontSize=36.sp,lineHeight=42.sp),
    headlineLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.ExtraBold,fontSize=30.sp,lineHeight=36.sp),
    headlineMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=25.sp,lineHeight=31.sp),
    headlineSmall=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=21.sp,lineHeight=27.sp),
    titleLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=19.sp,lineHeight=25.sp),
    titleMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=16.sp),
    bodyLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Normal,fontSize=16.sp,lineHeight=24.sp),
    bodyMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Normal,fontSize=14.sp,lineHeight=20.sp),
    labelLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=14.sp),
    labelMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=12.sp,letterSpacing=.2.sp)
)

@Composable fun MinistryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme=if(isSystemInDarkTheme()) DarkColors else LightColors,
        typography=MinistryTypography,
        shapes=Shapes(
            extraSmall=androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            small=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium=androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            large=androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
            extraLarge=androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
        ),
        content=content
    )
}
