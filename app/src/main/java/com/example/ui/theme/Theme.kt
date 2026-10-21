package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = BakeryAmberPrimaryLight,
        onPrimary = Color.White,
        primaryContainer = BakeryPrimaryContainerDark,
        onPrimaryContainer = BakeryOnPrimaryContainerDark,
        secondary = BakeryMochaSecondaryLight,
        onSecondary = Color.White,
        secondaryContainer = BakerySecondaryContainerDark,
        onSecondaryContainer = BakeryOnSecondaryContainerDark,
        tertiary = BakeryWheatTertiaryLight,
        onTertiary = Color.Black,
        tertiaryContainer = BakeryTertiaryContainerDark,
        onTertiaryContainer = BakeryOnTertiaryContainerDark,
        background = BakeryBackgroundDark,
        onBackground = BakeryOnSurfaceDark,
        surface = BakerySurfaceDark,
        onSurface = BakeryOnSurfaceDark,
        surfaceVariant = BakerySurfaceVariantDark,
        onSurfaceVariant = BakeryOnSurfaceVariantDark,
        outline = BakeryOutlineDark
    )

private val LightColorScheme =
    lightColorScheme(
        primary = BakeryAmberPrimary,
        onPrimary = Color.White,
        primaryContainer = BakeryPrimaryContainerLight,
        onPrimaryContainer = BakeryOnPrimaryContainerLight,
        secondary = BakeryMochaSecondary,
        onSecondary = Color.White,
        secondaryContainer = BakerySecondaryContainerLight,
        onSecondaryContainer = BakeryOnSecondaryContainerLight,
        tertiary = BakeryWheatTertiary,
        onTertiary = Color.White,
        tertiaryContainer = BakeryTertiaryContainerLight,
        onTertiaryContainer = BakeryOnTertiaryContainerLight,
        background = BakeryBackgroundLight,
        onBackground = BakeryOnSurfaceLight,
        surface = BakerySurfaceLight,
        onSurface = BakeryOnSurfaceLight,
        surfaceVariant = BakerySurfaceVariantLight,
        onSurfaceVariant = BakeryOnSurfaceVariantLight,
        outline = BakeryOutlineLight
    )

@Composable
fun BakeStockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    isWhiteBackground: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseColorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    val colorScheme = if (isWhiteBackground) {
        LightColorScheme.copy(
            background = Color.White,
            surface = Color.White,
            surfaceVariant = Color(0xFFF4F4F5),
            onBackground = Color(0xFF111827),
            onSurface = Color(0xFF111827),
            onSurfaceVariant = Color(0xFF374151),
            outline = Color(0xFFE5E7EB)
        )
    } else {
        baseColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = BakeryTypography, content = content)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    isWhiteBackground: Boolean = false,
    content: @Composable () -> Unit
) {
    BakeStockTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, isWhiteBackground = isWhiteBackground, content = content)
}
