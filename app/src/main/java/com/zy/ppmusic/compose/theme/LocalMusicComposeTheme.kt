package com.zy.ppmusic.compose.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LinkToneLightColors = lightColorScheme(
    primary = LinkTonePrimaryLight,
    onPrimary = LinkToneOnPrimaryLight,
    primaryContainer = LinkTonePrimaryContainerLight,
    onPrimaryContainer = LinkToneOnPrimaryContainerLight,
    secondary = LinkToneSecondaryLight,
    onSecondary = LinkToneOnSecondaryLight,
    secondaryContainer = LinkToneSecondaryContainerLight,
    onSecondaryContainer = LinkToneOnSecondaryContainerLight,
    tertiary = LinkToneTertiaryLight,
    onTertiary = LinkToneOnTertiaryLight,
    tertiaryContainer = LinkToneTertiaryContainerLight,
    onTertiaryContainer = LinkToneOnTertiaryContainerLight,
    error = LinkToneErrorLight,
    onError = LinkToneOnErrorLight,
    errorContainer = LinkToneErrorContainerLight,
    onErrorContainer = LinkToneOnErrorContainerLight,
    background = LinkToneBackgroundLight,
    onBackground = LinkToneOnBackgroundLight,
    surface = LinkToneSurfaceLight,
    onSurface = LinkToneOnSurfaceLight,
    surfaceVariant = LinkToneSurfaceVariantLight,
    onSurfaceVariant = LinkToneOnSurfaceVariantLight,
    outline = LinkToneOutlineLight,
    outlineVariant = LinkToneOutlineVariantLight,
)

private val LinkToneDarkColors = darkColorScheme(
    primary = LinkTonePrimaryDark,
    onPrimary = LinkToneOnPrimaryDark,
    primaryContainer = LinkTonePrimaryContainerDark,
    onPrimaryContainer = LinkToneOnPrimaryContainerDark,
    secondary = LinkToneSecondaryDark,
    onSecondary = LinkToneOnSecondaryDark,
    secondaryContainer = LinkToneSecondaryContainerDark,
    onSecondaryContainer = LinkToneOnSecondaryContainerDark,
    tertiary = LinkToneTertiaryDark,
    onTertiary = LinkToneOnTertiaryDark,
    tertiaryContainer = LinkToneTertiaryContainerDark,
    onTertiaryContainer = LinkToneOnTertiaryContainerDark,
    error = LinkToneErrorDark,
    onError = LinkToneOnErrorDark,
    errorContainer = LinkToneErrorContainerDark,
    onErrorContainer = LinkToneOnErrorContainerDark,
    background = LinkToneBackgroundDark,
    onBackground = LinkToneOnBackgroundDark,
    surface = LinkToneSurfaceDark,
    onSurface = LinkToneOnSurfaceDark,
    surfaceVariant = LinkToneSurfaceVariantDark,
    onSurfaceVariant = LinkToneOnSurfaceVariantDark,
    outline = LinkToneOutlineDark,
    outlineVariant = LinkToneOutlineVariantDark,
)

private val LinkToneShapes = Shapes(
    extraSmall = LinkToneShapeExtraSmall,
    small = LinkToneShapeSmall,
    medium = LinkToneShapeMedium,
    large = LinkToneShapeLarge,
    extraLarge = LinkToneShapeExtraLarge,
)

@Composable
fun LocalMusicComposeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> LinkToneDarkColors
        else -> LinkToneLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = LinkToneShapes,
        content = content,
    )
}
