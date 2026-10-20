package com.katchy.focuslive.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.katchy.focuslive.ui.theme.isDarkTheme

@Composable
fun brishPanelColor(): Color =
    if (isDarkTheme()) Color(0xFF15171E) else Color.White

@Composable
fun brishMutedPanelColor(): Color =
    if (isDarkTheme()) Color(0xFF1E212B) else Color(0xFFF7F8FA)

@Composable
fun brishPanelBorder(): BorderStroke = BorderStroke(
    1.dp,
    if (isDarkTheme()) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)
)

@Composable
fun brishPrimaryActionColor(): Color =
    if (isDarkTheme()) Color(0xFFF3F4F6) else Color(0xFF111827)

@Composable
fun brishPrimaryActionContentColor(): Color =
    if (isDarkTheme()) Color(0xFF111827) else Color.White
