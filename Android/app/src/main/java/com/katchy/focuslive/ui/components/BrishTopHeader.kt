package com.katchy.focuslive.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.model.AppTheme
import com.katchy.focuslive.ui.settings.SettingsViewModel
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.util.bounceClick

@Composable
fun BrishTopHeader(
    title: String = "Brish",
    onBack: (() -> Unit)? = null,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    showThemeToggle: Boolean = false,
    showChat: Boolean = true,
    showSettings: Boolean = true,
    actions: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isDarkTheme()
    val settingsVm: SettingsViewModel = hiltViewModel()
    val ink = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
    val chipBg = if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)
    val chipBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE5E7EB)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .height(48.dp)
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(chipBg)
                    .bounceClick()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = ink,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        if (showThemeToggle && onBack == null) {
            IconButton(
                onClick = {
                    val next = if (isDark) AppTheme.LIGHT else AppTheme.DARK
                    settingsVm.setTheme(next)
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(chipBg)
                    .bounceClick()
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    contentDescription = if (isDark) "Cambiar a tema claro" else "Cambiar a tema oscuro",
                    tint = ink,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f).padding(start = if (showThemeToggle && onBack == null) 12.dp else 0.dp, end = 12.dp),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (actions != null) {
                actions()
            }

            if (showChat) Surface(
                onClick = onNavigateToChat,
                shape = RoundedCornerShape(100.dp),
                color = chipBg,
                modifier = Modifier
                    .height(48.dp)
                    .bounceClick()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Brish IA",
                        tint = ink,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "IA",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = ink
                    )
                }
            }

            if (showSettings) IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(chipBg)
                    .bounceClick()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Ajustes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
