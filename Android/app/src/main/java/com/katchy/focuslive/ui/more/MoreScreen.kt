package com.katchy.focuslive.ui.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.ui.components.BrishTopHeader
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.util.bounceClick
import java.text.NumberFormat
import java.util.Locale

private data class ToolRowItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val statusBadge: String? = null,
    val badgeColor: Color? = null,
    val onClick: () -> Unit
)

@Composable
fun MoreScreen(
    onNavigateToPlanner: () -> Unit,
    onNavigateToHabits: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    viewModel: MoreViewModel = hiltViewModel()
) {
    val isDark = isDarkTheme()
    val today by viewModel.state.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

    val toolItems = listOf(
        ToolRowItem(
            title = "Agenda",
            subtitle = "Planifica días, semanas y organiza eventos",
            icon = Icons.Rounded.CalendarToday,
            accentColor = Color(0xFF0284C7),
            statusBadge = if (today.pendingTasks > 0) "${today.pendingTasks} pendiente${if (today.pendingTasks == 1) "" else "s"}" else "Al día",
            badgeColor = if (today.pendingTasks > 0) Color(0xFF0284C7) else Color(0xFF10B981),
            onClick = onNavigateToPlanner
        ),
        ToolRowItem(
            title = "Hábitos",
            subtitle = "Seguimiento de disciplina y rutinas activas",
            icon = Icons.Rounded.CheckCircleOutline,
            accentColor = Color(0xFF10B981),
            statusBadge = if (today.habitsTotal > 0) "${today.habitsDone}/${today.habitsTotal}" else null,
            badgeColor = Color(0xFF10B981),
            onClick = onNavigateToHabits
        ),
        ToolRowItem(
            title = "Finanzas",
            subtitle = "Control de ingresos, gastos y metas de ahorro",
            icon = Icons.Rounded.AccountBalanceWallet,
            accentColor = Color(0xFF8B5CF6),
            statusBadge = if (today.expensesToday > 0) currencyFormat.format(today.expensesToday) else "Al día",
            badgeColor = Color(0xFF8B5CF6),
            onClick = onNavigateToFinance
        ),
        ToolRowItem(
            title = "Ajustes",
            subtitle = "Temas, sonido de enfoque, cuenta y respaldo",
            icon = Icons.Rounded.Settings,
            accentColor = Color(0xFF64748B),
            statusBadge = null,
            badgeColor = null,
            onClick = onNavigateToSettings
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BrishTopHeader(
                title = "Más",
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToChat = onNavigateToChat
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "Tu día, organizado",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.4).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Rutinas, agenda y finanzas en un solo lugar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }

            // Quick Today Status Card
            item {
                TodayGlanceCard(
                    state = today,
                    isDark = isDark,
                    onPlanner = onNavigateToPlanner,
                    onHabits = onNavigateToHabits,
                    onFinance = onNavigateToFinance
                )
            }

            // Section Label
            item {
                Text(
                    text = "Herramientas",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            // Grouped Navigation Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.18f else 0.45f)
                    ),
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        toolItems.forEachIndexed { index, tool ->
                            GroupedToolRow(tool = tool, isDark = isDark)
                            if (index < toolItems.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.12f else 0.35f)
                                )
                            }
                        }
                    }
                }
            }

            // App Version Footer
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Brish VP 1.1 · Modo Enfoque Activo",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayGlanceCard(
    state: MoreTodayState,
    isDark: Boolean,
    onPlanner: () -> Unit,
    onHabits: () -> Unit,
    onFinance: () -> Unit
) {
    val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE")).format(state.expensesToday)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.18f else 0.45f)
        ),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Resumen de hoy",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (state.pendingTasks == 0 && state.habitsDone == state.habitsTotal && state.habitsTotal > 0)
                            "¡Todo al día por hoy!"
                        else
                            "Progreso del día en tiempo real",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.4f else 0.7f)
                ) {
                    Text(
                        text = "HOY",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlanceMetricItem(
                    icon = Icons.Rounded.TaskAlt,
                    value = state.pendingTasks.toString(),
                    label = "pendientes",
                    accent = Color(0xFF0284C7),
                    onClick = onPlanner,
                    modifier = Modifier.weight(1f)
                )
                GlanceMetricItem(
                    icon = Icons.Rounded.LocalFireDepartment,
                    value = "${state.habitsDone}/${state.habitsTotal}",
                    label = "hábitos",
                    accent = Color(0xFF10B981),
                    onClick = onHabits,
                    modifier = Modifier.weight(1f)
                )
                GlanceMetricItem(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    value = currency,
                    label = "gastos hoy",
                    accent = Color(0xFF8B5CF6),
                    onClick = onFinance,
                    modifier = Modifier.weight(1.1f)
                )
            }
        }
    }
}

@Composable
private fun GlanceMetricItem(
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        modifier = modifier
            .height(78.dp)
            .bounceClick()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GroupedToolRow(
    tool: ToolRowItem,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = tool.onClick)
            .bounceClick(scaleDown = 0.99f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Container
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(tool.accentColor.copy(alpha = if (isDark) 0.18f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                tool.icon,
                contentDescription = null,
                tint = tool.accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        // Titles
        Column(Modifier.weight(1f)) {
            Text(
                text = tool.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = tool.subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Status Badge (if any)
        if (tool.statusBadge != null && tool.badgeColor != null) {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = tool.badgeColor.copy(alpha = if (isDark) 0.15f else 0.1f),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = tool.statusBadge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tool.badgeColor,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }

        // Chevron
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
