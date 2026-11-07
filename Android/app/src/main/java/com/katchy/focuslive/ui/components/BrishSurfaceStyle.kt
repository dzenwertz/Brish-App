package com.katchy.focuslive.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.katchy.focuslive.ui.theme.BrishCard
import com.katchy.focuslive.ui.theme.BrishHairline
import com.katchy.focuslive.ui.theme.BrishInk
import com.katchy.focuslive.ui.theme.BrishInsetSoft
import com.katchy.focuslive.ui.theme.BrishNightCard
import com.katchy.focuslive.ui.theme.BrishNightInset
import com.katchy.focuslive.ui.theme.BrishNightText
import com.katchy.focuslive.ui.theme.isDarkTheme

@Composable
fun brishPanelColor(): Color =
    if (isDarkTheme()) BrishNightCard else BrishCard

@Composable
fun brishMutedPanelColor(): Color =
    if (isDarkTheme()) BrishNightInset else BrishInsetSoft

@Composable
fun brishPanelBorder(): BorderStroke = BorderStroke(
    1.dp,
    if (isDarkTheme()) Color.White.copy(alpha = 0.07f) else BrishHairline
)

@Composable
fun brishPrimaryActionColor(): Color =
    if (isDarkTheme()) BrishNightText else BrishInk

@Composable
fun brishPrimaryActionContentColor(): Color =
    if (isDarkTheme()) BrishInk else Color.White
