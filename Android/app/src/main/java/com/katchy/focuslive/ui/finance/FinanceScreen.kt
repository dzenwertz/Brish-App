@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.katchy.focuslive.ui.finance

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.katchy.focuslive.data.model.Goal
import com.katchy.focuslive.data.model.Transaction
import com.katchy.focuslive.ui.components.BrishTopHeader
import com.katchy.focuslive.ui.components.SmoothDialog
import com.katchy.focuslive.ui.theme.isDarkTheme
import com.katchy.focuslive.ui.util.InAppNotificationBus
import com.katchy.focuslive.ui.util.bounceClick
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FinanceScreen(
    onBack: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: FinanceViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val activeFilter by viewModel.filterType.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Transaction?>(null) }

    // Savings Goals sheets
    var showAddGoalSheet by remember { mutableStateOf(false) }
    var goalToDeposit by remember { mutableStateOf<Goal?>(null) }
    var goalToEdit by remember { mutableStateOf<Goal?>(null) }

    // Draft states for new transaction
    var draftTxTitle by rememberSaveable { mutableStateOf("") }
    var draftTxAmount by rememberSaveable { mutableStateOf("") }
    var draftTxType by rememberSaveable { mutableStateOf("EXPENSE") }
    var draftTxCategory by rememberSaveable { mutableStateOf("Comida") }
    var draftTxDateOffset by rememberSaveable { mutableStateOf(0) } // 0: Hoy, 1: Ayer

    // Toggle for displaying all transactions
    var showAllTransactions by remember { mutableStateOf(false) }

    val totalIncome = remember(transactions) { transactions.filter { it.type == "INCOME" }.sumOf { it.amount } }
    val totalExpense = remember(transactions) { transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount } }
    val balance = remember(totalIncome, totalExpense) { totalIncome - totalExpense }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")).apply { maximumFractionDigits = 2 } }
    val isDark = isDarkTheme()
    val haptic = LocalHapticFeedback.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addGoalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val depositSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Accurate category grouping
    val topExpenseCategories = remember(transactions) {
        val expenses = transactions.filter { it.type == "EXPENSE" }
        val grouped = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        val defaults = listOf(
            "Comida" to 0.0,
            "Transporte" to 0.0,
            "Servicios" to 0.0,
            "Compras" to 0.0
        )

        val merged = (grouped + defaults).distinctBy { it.first }.take(4)
        merged
    }

    // Filtered transaction list
    val filteredTransactions = remember(transactions, activeFilter) {
        when (activeFilter) {
            "EXPENSE" -> transactions.filter { it.type == "EXPENSE" }
            "INCOME" -> transactions.filter { it.type == "INCOME" }
            else -> transactions
        }
    }

    val visibleTransactions = remember(filteredTransactions, showAllTransactions) {
        if (showAllTransactions) filteredTransactions else filteredTransactions.take(15)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BrishTopHeader(
                title = "Finanzas",
                onBack = onBack,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToChat = onNavigateToChat
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
        ) {
            // 1. Balance Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF15171E) else Color.White,
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Balance Total",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currencyFormatter.format(balance),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 34.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Real Income & Expense Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.ArrowDownward,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Ingresos: ${currencyFormatter.format(totalIncome)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF10B981),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.ArrowUpward,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Gastos: ${currencyFormatter.format(totalExpense)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons: [ Añadir Ingreso ] [ Registrar Gasto ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    draftTxType = "INCOME"
                                    draftTxCategory = "Salario"
                                    draftTxTitle = ""
                                    draftTxAmount = ""
                                    draftTxDateOffset = 0
                                    showAddSheet = true
                                },
                                shape = RoundedCornerShape(100.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                                    contentColor = if (isDark) Color(0xFF111827) else Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .bounceClick()
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Añadir Ingreso", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            OutlinedButton(
                                onClick = {
                                    draftTxType = "EXPENSE"
                                    draftTxCategory = "Comida"
                                    draftTxTitle = ""
                                    draftTxAmount = ""
                                    draftTxDateOffset = 0
                                    showAddSheet = true
                                },
                                shape = RoundedCornerShape(100.dp),
                                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.25f) else Color(0xFFD1D5DB)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .bounceClick()
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Registrar Gasto", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // 2. Metas de Ahorro (Savings Goals) Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Metas de Ahorro",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        TextButton(
                            onClick = { showAddGoalSheet = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Nueva", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    if (savingsGoals.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0xFF15171E) else Color.White,
                            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddGoalSheet = true }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.TrackChanges,
                                        contentDescription = null,
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Crea tu primera meta de ahorro",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Define un objetivo y acumula tus aportes fácilmente.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(savingsGoals, key = { it.id }) { goal ->
                                SavingsGoalCard(
                                    goal = goal,
                                    isDark = isDark,
                                    currencyFormatter = currencyFormatter,
                                    onDepositClick = { goalToDeposit = goal },
                                    onEditClick = { goalToEdit = goal }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Weekly Spending Bar Chart
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF15171E) else Color.White,
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Resumen de Gastos Semanal",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Deterministic Weekly Spending Bars
                        WeeklySpendingBarChart(
                            transactions = transactions,
                            isDark = isDark,
                            currencyFormatter = currencyFormatter
                        )
                    }
                }
            }

            // 4. Real Category Breakdown Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Gastos por Categoría",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        topExpenseCategories.getOrNull(0)?.let { (cat, amount) ->
                            CategorySummaryCard(
                                icon = getCategoryIcon(cat, false),
                                title = cat,
                                amount = currencyFormatter.format(amount),
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        topExpenseCategories.getOrNull(1)?.let { (cat, amount) ->
                            CategorySummaryCard(
                                icon = getCategoryIcon(cat, false),
                                title = cat,
                                amount = currencyFormatter.format(amount),
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        topExpenseCategories.getOrNull(2)?.let { (cat, amount) ->
                            CategorySummaryCard(
                                icon = getCategoryIcon(cat, false),
                                title = cat,
                                amount = currencyFormatter.format(amount),
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        topExpenseCategories.getOrNull(3)?.let { (cat, amount) ->
                            CategorySummaryCard(
                                icon = getCategoryIcon(cat, false),
                                title = cat,
                                amount = currencyFormatter.format(amount),
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. "Transactions" Section with Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transacciones",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        TextButton(
                            onClick = {
                                draftTxType = "EXPENSE"
                                draftTxCategory = "Comida"
                                draftTxTitle = ""
                                draftTxAmount = ""
                                draftTxDateOffset = 0
                                showAddSheet = true
                            }
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Nueva", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Filter Chips: [ Todos ] [ Gastos ] [ Ingresos ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "Todos",
                            "EXPENSE" to "Gastos",
                            "INCOME" to "Ingresos"
                        ).forEach { (typeKey, label) ->
                            val isSelected = activeFilter == typeKey
                            Surface(
                                onClick = { viewModel.setFilterType(typeKey) },
                                shape = RoundedCornerShape(100.dp),
                                color = if (isSelected) {
                                    if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
                                } else {
                                    if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)
                                },
                                modifier = Modifier.bounceClick()
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) {
                                        if (isDark) Color(0xFF111827) else Color.White
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) Color(0xFF15171E) else Color.White,
                        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1E212B) else Color(0xFFE8EEF8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (isDark) Color.White else Color(0xFF111827),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Sin transacciones registradas",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (activeFilter == "ALL") "Registra tus ingresos y gastos para ver tu balance."
                                else "No hay movimientos en este filtro.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    draftTxType = if (activeFilter == "INCOME") "INCOME" else "EXPENSE"
                                    draftTxCategory = if (draftTxType == "INCOME") "Salario" else "Comida"
                                    draftTxTitle = ""
                                    draftTxAmount = ""
                                    showAddSheet = true
                                },
                                shape = RoundedCornerShape(100.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                                    contentColor = if (isDark) Color(0xFF111827) else Color.White
                                ),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Registrar Movimiento", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(visibleTransactions, key = { it.id }) { tx ->
                    TransactionItemRow(
                        transaction = tx,
                        isDark = isDark,
                        currencyFormatter = currencyFormatter,
                        onClick = { transactionToEdit = tx }
                    )
                }

                // Show more / Show less button if there are more than 15 transactions
                if (filteredTransactions.size > 15) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(
                                onClick = { showAllTransactions = !showAllTransactions },
                                modifier = Modifier.bounceClick()
                            ) {
                                Text(
                                    text = if (!showAllTransactions) "Ver todas (${filteredTransactions.size})" else "Mostrar menos",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (!showAllTransactions) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // 6. ADD TRANSACTION BOTTOM SHEET
    // ==========================================
    if (showAddSheet) {
        val expenseCategoryOptions = remember {
            listOf("Comida", "Transporte", "Servicios", "Alquiler", "Compras", "Salud", "Educación", "Entretenimiento", "Ahorro", "Otro")
        }
        val incomeCategoryOptions = remember {
            listOf("Salario", "Freelance", "Inversiones", "Regalo", "Venta", "Otro")
        }
        val currentCategoryOptions = if (draftTxType == "INCOME") incomeCategoryOptions else expenseCategoryOptions

        val parsedAmount = remember(draftTxAmount) {
            val cleaned = draftTxAmount.trim().replace(" ", "").replace(",", ".")
            cleaned.toDoubleOrNull()?.takeIf { it > 0 }
        }
        val isFormValid = draftTxTitle.isNotBlank() && parsedAmount != null

        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
            containerColor = if (isDark) Color(0xFF15171E) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Text(
                    text = if (draftTxType == "INCOME") "Añadir Ingreso" else "Registrar Gasto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Type Selector: [ Gasto ] [ Ingreso ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6))
                        .padding(4.dp)
                ) {
                    val isExpenseSelected = draftTxType == "EXPENSE"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isExpenseSelected) (if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)) else Color.Transparent)
                            .clickable {
                                draftTxType = "EXPENSE"
                                if (!expenseCategoryOptions.contains(draftTxCategory)) {
                                    draftTxCategory = "Comida"
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Gasto",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isExpenseSelected) (if (isDark) Color(0xFF111827) else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val isIncomeSelected = draftTxType == "INCOME"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isIncomeSelected) (if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)) else Color.Transparent)
                            .clickable {
                                draftTxType = "INCOME"
                                if (!incomeCategoryOptions.contains(draftTxCategory)) {
                                    draftTxCategory = "Salario"
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ingreso",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isIncomeSelected) (if (isDark) Color(0xFF111827) else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = draftTxTitle,
                    onValueChange = { draftTxTitle = it },
                    label = { Text("Concepto (ej: Almuerzo, Uber, Sueldo)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = draftTxAmount,
                    onValueChange = { input ->
                        // Sanitize numeric input (digits and single decimal point/comma)
                        val sanitized = input.filter { it.isDigit() || it == '.' || it == ',' }
                        if (sanitized.count { it == '.' || it == ',' } <= 1) {
                            draftTxAmount = sanitized
                        }
                    },
                    label = { Text("Monto") },
                    prefix = { Text("S/ ", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date Selector: [ Hoy ] [ Ayer ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fecha:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Surface(
                        onClick = { draftTxDateOffset = 0 },
                        shape = RoundedCornerShape(100.dp),
                        color = if (draftTxDateOffset == 0) {
                            if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
                        } else if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)
                    ) {
                        Text(
                            text = "Hoy",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (draftTxDateOffset == 0) {
                                if (isDark) Color(0xFF111827) else Color.White
                            } else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        onClick = { draftTxDateOffset = 1 },
                        shape = RoundedCornerShape(100.dp),
                        color = if (draftTxDateOffset == 1) {
                            if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
                        } else if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)
                    ) {
                        Text(
                            text = "Ayer",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (draftTxDateOffset == 1) {
                                if (isDark) Color(0xFF111827) else Color.White
                            } else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Chips
                Text("Categoría", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currentCategoryOptions) { cat ->
                        val isSelected = draftTxCategory == cat
                        Surface(
                            onClick = { draftTxCategory = cat },
                            shape = RoundedCornerShape(100.dp),
                            color = if (isSelected) {
                                if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
                            } else if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(cat, draftTxType == "INCOME"),
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = if (isSelected) (if (isDark) Color(0xFF111827) else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        if (isDark) Color(0xFF111827) else Color.White
                                    } else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val amount = parsedAmount ?: return@Button
                        if (draftTxTitle.isNotBlank()) {
                            val timestamp = if (draftTxDateOffset == 1) {
                                System.currentTimeMillis() - 24L * 60 * 60 * 1000
                            } else {
                                System.currentTimeMillis()
                            }

                            viewModel.addTransaction(
                                title = draftTxTitle.trim(),
                                amount = amount,
                                type = draftTxType,
                                category = draftTxCategory,
                                timestamp = timestamp
                            )
                            draftTxTitle = ""
                            draftTxAmount = ""
                            showAddSheet = false
                            InAppNotificationBus.post("Transacción guardada")
                        }
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        contentColor = if (isDark) Color(0xFF111827) else Color.White,
                        disabledContainerColor = if (isDark) Color(0xFF1E212B) else Color(0xFFE5E7EB),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // ==========================================
    // 7. EDIT TRANSACTION BOTTOM SHEET
    // ==========================================
    transactionToEdit?.let { tx ->
        var editTitle by remember(tx.id) { mutableStateOf(tx.title) }
        var editAmount by remember(tx.id) { mutableStateOf(if (tx.amount % 1.0 == 0.0) tx.amount.toInt().toString() else tx.amount.toString()) }
        var editCategory by remember(tx.id) { mutableStateOf(tx.category) }
        var editType by remember(tx.id) { mutableStateOf(tx.type) }

        val categoryOptions = if (editType == "INCOME") {
            listOf("Salario", "Freelance", "Inversiones", "Regalo", "Venta", "Otro")
        } else {
            listOf("Comida", "Transporte", "Servicios", "Alquiler", "Compras", "Salud", "Educación", "Entretenimiento", "Ahorro", "Otro")
        }

        val parsedEditAmount = remember(editAmount) {
            val cleaned = editAmount.trim().replace(" ", "").replace(",", ".")
            cleaned.toDoubleOrNull()?.takeIf { it > 0 }
        }
        val isEditValid = editTitle.isNotBlank() && parsedEditAmount != null

        ModalBottomSheet(
            onDismissRequest = { transactionToEdit = null },
            sheetState = editSheetState,
            containerColor = if (isDark) Color(0xFF15171E) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Editar Movimiento",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            val toDelete = tx
                            transactionToEdit = null
                            showDeleteConfirmDialog = toDelete
                        }
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Type Selector (Gasto / Ingreso)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6))
                        .padding(4.dp)
                ) {
                    val isExpenseSelected = editType == "EXPENSE"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isExpenseSelected) (if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)) else Color.Transparent)
                            .clickable {
                                editType = "EXPENSE"
                                if (!categoryOptions.contains(editCategory)) {
                                    editCategory = "Comida"
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Gasto",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isExpenseSelected) (if (isDark) Color(0xFF111827) else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val isIncomeSelected = editType == "INCOME"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isIncomeSelected) (if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)) else Color.Transparent)
                            .clickable {
                                editType = "INCOME"
                                if (!categoryOptions.contains(editCategory)) {
                                    editCategory = "Salario"
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ingreso",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isIncomeSelected) (if (isDark) Color(0xFF111827) else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Concepto") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editAmount,
                    onValueChange = { input ->
                        val sanitized = input.filter { it.isDigit() || it == '.' || it == ',' }
                        if (sanitized.count { it == '.' || it == ',' } <= 1) {
                            editAmount = sanitized
                        }
                    },
                    label = { Text("Monto") },
                    prefix = { Text("S/ ", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Categoría", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categoryOptions) { cat ->
                        val isSelected = editCategory == cat
                        Surface(
                            onClick = { editCategory = cat },
                            shape = RoundedCornerShape(100.dp),
                            color = if (isSelected) {
                                if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
                            } else if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(cat, editType == "INCOME"),
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = if (isSelected) (if (isDark) Color(0xFF111827) else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        if (isDark) Color(0xFF111827) else Color.White
                                    } else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val amount = parsedEditAmount ?: return@Button
                        if (editTitle.isNotBlank()) {
                            viewModel.updateTransaction(
                                transaction = tx,
                                newTitle = editTitle.trim(),
                                newAmount = amount,
                                newCategory = editCategory,
                                newRecurrence = tx.recurrenceInterval,
                                newType = editType
                            )
                            transactionToEdit = null
                            InAppNotificationBus.post("Transacción actualizada")
                        }
                    },
                    enabled = isEditValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        contentColor = if (isDark) Color(0xFF111827) else Color.White,
                        disabledContainerColor = if (isDark) Color(0xFF1E212B) else Color(0xFFE5E7EB),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // ==========================================
    // 8. DELETE CONFIRMATION DIALOG
    // ==========================================
    showDeleteConfirmDialog?.let { txToDelete ->
        SmoothDialog(
            onDismissRequest = { showDeleteConfirmDialog = null }
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF15171E) else Color.White,
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "¿Eliminar movimiento?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Se eliminará '${txToDelete.title}' (${currencyFormatter.format(txToDelete.amount)}) de tus finanzas. Esta acción no se puede deshacer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showDeleteConfirmDialog = null }
                        ) {
                            Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.deleteTransaction(txToDelete.id)
                                showDeleteConfirmDialog = null
                                transactionToEdit = null
                                InAppNotificationBus.post("Transacción eliminada")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Eliminar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // 9. ADD SAVINGS GOAL BOTTOM SHEET
    // ==========================================
    if (showAddGoalSheet) {
        var goalTitle by remember { mutableStateOf("") }
        var goalTargetAmount by remember { mutableStateOf("") }
        var goalIcon by remember { mutableStateOf("SAVINGS") }

        val parsedGoalAmount = remember(goalTargetAmount) {
            val cleaned = goalTargetAmount.trim().replace(" ", "").replace(",", ".")
            cleaned.toDoubleOrNull()?.takeIf { it > 0 }
        }
        val isGoalValid = goalTitle.isNotBlank() && parsedGoalAmount != null

        ModalBottomSheet(
            onDismissRequest = { showAddGoalSheet = false },
            sheetState = addGoalSheetState,
            containerColor = if (isDark) Color(0xFF15171E) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Text(
                    text = "Nueva Meta de Ahorro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Clean Icon Picker
                Text("Icono de la Meta", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(goalIconOptions) { opt ->
                        val isSelected = goalIcon == opt.key
                        Surface(
                            onClick = { goalIcon = opt.key },
                            shape = RoundedCornerShape(100.dp),
                            color = if (isSelected) {
                                if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
                            } else {
                                if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)
                            },
                            modifier = Modifier.bounceClick()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = opt.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) (if (isDark) Color(0xFF111827) else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = opt.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) (if (isDark) Color(0xFF111827) else Color.White) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = goalTitle,
                    onValueChange = { goalTitle = it },
                    label = { Text("Nombre (ej: Vacaciones, Fondo de emergencia)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = goalTargetAmount,
                    onValueChange = { input ->
                        val sanitized = input.filter { it.isDigit() || it == '.' || it == ',' }
                        if (sanitized.count { it == '.' || it == ',' } <= 1) {
                            goalTargetAmount = sanitized
                        }
                    },
                    label = { Text("Monto Objetivo") },
                    prefix = { Text("S/ ", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val target = parsedGoalAmount ?: return@Button
                        viewModel.addSavingsGoal(
                            title = goalTitle.trim(),
                            targetAmount = target,
                            icon = goalIcon,
                            colorHex = 0x10B981L
                        )
                        showAddGoalSheet = false
                        InAppNotificationBus.post("Meta de ahorro creada")
                    },
                    enabled = isGoalValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        contentColor = if (isDark) Color(0xFF111827) else Color.White
                    )
                ) {
                    Text("Crear Meta", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // ==========================================
    // 10. DEPOSIT TO GOAL BOTTOM SHEET
    // ==========================================
    goalToDeposit?.let { goal ->
        var depositAmountInput by remember { mutableStateOf("") }
        val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)

        val parsedDeposit = remember(depositAmountInput) {
            val cleaned = depositAmountInput.trim().replace(" ", "").replace(",", ".")
            cleaned.toDoubleOrNull()?.takeIf { it > 0 }
        }

        ModalBottomSheet(
            onDismissRequest = { goalToDeposit = null },
            sheetState = depositSheetState,
            containerColor = if (isDark) Color(0xFF15171E) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getGoalIconVector(goal.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Aportar a: ${goal.title}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Faltan ${currencyFormatter.format(remaining)} para completar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick preset buttons: [+10] [+20] [+50] [+100]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10.0, 20.0, 50.0, 100.0).forEach { preset ->
                        Surface(
                            onClick = {
                                val current = parsedDeposit ?: 0.0
                                depositAmountInput = (current + preset).toInt().toString()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6),
                            modifier = Modifier
                                .weight(1f)
                                .bounceClick()
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "+S/ ${preset.toInt()}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = depositAmountInput,
                    onValueChange = { input ->
                        val sanitized = input.filter { it.isDigit() || it == '.' || it == ',' }
                        if (sanitized.count { it == '.' || it == ',' } <= 1) {
                            depositAmountInput = sanitized
                        }
                    },
                    label = { Text("Monto a depositar") },
                    prefix = { Text("S/ ", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val amount = parsedDeposit ?: return@Button
                        viewModel.depositToGoal(goal, amount)
                        goalToDeposit = null
                        InAppNotificationBus.post("Depósito realizado")
                    },
                    enabled = parsedDeposit != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        contentColor = if (isDark) Color(0xFF111827) else Color.White
                    )
                ) {
                    Text("Confirmar Aporte", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // ==========================================
    // 11. EDIT/DELETE SAVINGS GOAL BOTTOM SHEET
    // ==========================================
    goalToEdit?.let { goal ->
        var editTitle by remember(goal.id) { mutableStateOf(goal.title) }
        var editTarget by remember(goal.id) { mutableStateOf(if (goal.targetAmount % 1.0 == 0.0) goal.targetAmount.toInt().toString() else goal.targetAmount.toString()) }
        var editIcon by remember(goal.id) { mutableStateOf(goal.icon) }
        var showDeleteGoalConfirm by remember { mutableStateOf(false) }

        val parsedTarget = remember(editTarget) {
            val cleaned = editTarget.trim().replace(" ", "").replace(",", ".")
            cleaned.toDoubleOrNull()?.takeIf { it > 0 }
        }

        ModalBottomSheet(
            onDismissRequest = { goalToEdit = null },
            containerColor = if (isDark) Color(0xFF15171E) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Editar Meta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showDeleteGoalConfirm = true }
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Clean Icon Picker
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(goalIconOptions) { opt ->
                        val isSelected = editIcon == opt.key
                        Surface(
                            onClick = { editIcon = opt.key },
                            shape = RoundedCornerShape(100.dp),
                            color = if (isSelected) {
                                if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
                            } else {
                                if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)
                            },
                            modifier = Modifier.bounceClick()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = opt.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) (if (isDark) Color(0xFF111827) else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = opt.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) (if (isDark) Color(0xFF111827) else Color.White) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Nombre de la meta") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editTarget,
                    onValueChange = { input ->
                        val sanitized = input.filter { it.isDigit() || it == '.' || it == ',' }
                        if (sanitized.count { it == '.' || it == ',' } <= 1) {
                            editTarget = sanitized
                        }
                    },
                    label = { Text("Monto Objetivo") },
                    prefix = { Text("S/ ", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val target = parsedTarget ?: return@Button
                        if (editTitle.isNotBlank()) {
                            viewModel.updateSavingsGoal(
                                goal = goal,
                                newTitle = editTitle.trim(),
                                newTarget = target,
                                newIcon = editIcon,
                                newColor = 0x10B981L
                            )
                            goalToEdit = null
                            InAppNotificationBus.post("Meta actualizada")
                        }
                    },
                    enabled = editTitle.isNotBlank() && parsedTarget != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        contentColor = if (isDark) Color(0xFF111827) else Color.White
                    )
                ) {
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showDeleteGoalConfirm) {
            SmoothDialog(
                onDismissRequest = { showDeleteGoalConfirm = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF15171E) else Color.White,
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "¿Eliminar meta de ahorro?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Se eliminará '${goal.title}'. Los movimientos ya registrados en tu balance se conservarán.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showDeleteGoalConfirm = false }) {
                                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.deleteSavingsGoal(goal.id)
                                    showDeleteGoalConfirm = false
                                    goalToEdit = null
                                    InAppNotificationBus.post("Meta eliminada")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text("Eliminar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 12. DETERMINISTIC WEEKLY SPENDING BAR CHART
// ==========================================
@Composable
fun WeeklySpendingBarChart(
    transactions: List<Transaction>,
    isDark: Boolean,
    currencyFormatter: NumberFormat
) {
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    // 7 Days of the current week (Monday to Sunday) deterministically calculated
    val weekDaysData = remember(transactions) {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysFromMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
            add(Calendar.DAY_OF_YEAR, -daysFromMonday)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayNames = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        (0..6).map { idx ->
            val dayStart = cal.timeInMillis
            val nextDayCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            val dayEnd = nextDayCal.timeInMillis
            val daySpending = transactions.filter {
                it.type == "EXPENSE" && it.timestamp in dayStart until dayEnd
            }.sumOf { it.amount }
            cal.add(Calendar.DAY_OF_YEAR, 1)
            Triple(dayNames[idx], daySpending, dayStart)
        }
    }

    val maxDailySpending = remember(weekDaysData) {
        weekDaysData.maxOfOrNull { it.second } ?: 0.0
    }
    val totalWeeklyExpense = remember(weekDaysData) {
        weekDaysData.sumOf { it.second }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            weekDaysData.forEachIndexed { idx, (day, amount, _) ->
                val isSelected = selectedDayIndex == idx
                val barHeightFraction = if (maxDailySpending > 0) {
                    (amount / maxDailySpending).toFloat().coerceIn(0.08f, 1.0f)
                } else {
                    0.08f
                }
                val hasSpending = amount > 0

                val animatedBarHeight by animateFloatAsState(
                    targetValue = barHeightFraction,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 380f
                    ),
                    label = "bar_$day"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .bounceClick()
                        .clickable {
                            selectedDayIndex = if (selectedDayIndex == idx) null else idx
                        }
                ) {
                    // Bar
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height((76 * animatedBarHeight).dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    isSelected -> Color(0xFF06B6D4) // Highlight cyan
                                    hasSpending -> if (isDark) Color.White else Color(0xFF111827)
                                    else -> if (isDark) Color(0xFF262C38) else Color(0xFFE5E7EB)
                                }
                            )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Day Label
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isSelected -> Color(0xFF06B6D4)
                            hasSpending -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                        fontWeight = if (hasSpending || isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Day feedback or Total Weekly Expense
        if (selectedDayIndex != null) {
            val selected = weekDaysData[selectedDayIndex!!]
            Text(
                text = "${selected.first}: ${if (selected.second > 0) currencyFormatter.format(selected.second) else "Sin gastos"}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF06B6D4),
                fontSize = 12.sp
            )
        } else if (totalWeeklyExpense > 0) {
            Text(
                text = "Total gastado esta semana: ${currencyFormatter.format(totalWeeklyExpense)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        } else {
            Text(
                text = "Sin gastos registrados esta semana",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
    }
}

// ==========================================
// 13. SAVINGS GOAL CARD
// ==========================================
@Composable
fun SavingsGoalCard(
    goal: Goal,
    isDark: Boolean,
    currencyFormatter: NumberFormat,
    onDepositClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) {
        (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    } else 0f

    val isCompleted = goal.currentAmount >= goal.targetAmount && goal.targetAmount > 0

    Surface(
        onClick = onEditClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) Color(0xFF15171E) else Color.White,
        shadowElevation = if (isDark) 0.dp else 2.dp,
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
        modifier = Modifier
            .width(220.dp)
            .bounceClick()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getGoalIconVector(goal.icon),
                        contentDescription = null,
                        tint = if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (isCompleted) Color(0xFF10B981).copy(alpha = 0.15f) else (if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6))
                ) {
                    Text(
                        text = if (isCompleted) "¡Completada!" else "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = goal.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${currencyFormatter.format(goal.currentAmount)} de ${currencyFormatter.format(goal.targetAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(100.dp)),
                color = if (isCompleted) Color(0xFF10B981) else Color(0xFF06B6D4),
                trackColor = if (isDark) Color(0xFF1E212B) else Color(0xFFE5E7EB)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // "Aportar" button
            Button(
                onClick = onDepositClick,
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF1E212B) else Color(0xFFF3F4F6),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .bounceClick()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Aportar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 14. CATEGORY SUMMARY CARD
// ==========================================
@Composable
fun CategorySummaryCard(
    icon: ImageVector,
    title: String,
    amount: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF15171E) else Color.White,
        shadowElevation = if (isDark) 0.dp else 2.dp,
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
        modifier = modifier.height(105.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ==========================================
// 15. TRANSACTION ITEM ROW
// ==========================================
@Composable
fun TransactionItemRow(
    transaction: Transaction,
    isDark: Boolean,
    currencyFormatter: NumberFormat,
    onClick: () -> Unit
) {
    val isIncome = transaction.type == "INCOME"
    val timeFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale("es", "ES")) }
    val formattedTime = remember(transaction.timestamp) { timeFormat.format(Date(transaction.timestamp)) }

    val icon = remember(transaction.category, isIncome) {
        getCategoryIcon(transaction.category, isIncome)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF15171E) else Color.White,
        shadowElevation = if (isDark) 0.dp else 1.dp,
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFF0F2F6)),
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left circular icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isIncome) Color(0xFF10B981).copy(alpha = 0.15f)
                        else if (isDark) Color(0xFF1E212B)
                        else Color(0xFFF3F4F6)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isIncome) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.category} • $formattedTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Amount
            Text(
                text = "${if (isIncome) "+" else "-"}${currencyFormatter.format(transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = if (isIncome) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ==========================================
// 16. CATEGORY ICON HELPER
// ==========================================
private fun getCategoryIcon(category: String, isIncome: Boolean): ImageVector {
    if (isIncome) {
        return when {
            category.contains("Salario", ignoreCase = true) || category.contains("Sueldo", ignoreCase = true) -> Icons.Rounded.AccountBalance
            category.contains("Freelance", ignoreCase = true) -> Icons.Rounded.Laptop
            category.contains("Invers", ignoreCase = true) -> Icons.AutoMirrored.Rounded.TrendingUp
            category.contains("Regalo", ignoreCase = true) -> Icons.Rounded.CardGiftcard
            category.contains("Venta", ignoreCase = true) -> Icons.Rounded.Storefront
            else -> Icons.Rounded.ArrowDownward
        }
    }

    return when {
        category.contains("Comida", ignoreCase = true) || category.contains("Aliment", ignoreCase = true) -> Icons.Rounded.Restaurant
        category.contains("Transp", ignoreCase = true) || category.contains("Uber", ignoreCase = true) || category.contains("Taxi", ignoreCase = true) -> Icons.Rounded.DirectionsCar
        category.contains("Servicio", ignoreCase = true) || category.contains("Factura", ignoreCase = true) || category.contains("Luz", ignoreCase = true) || category.contains("Agua", ignoreCase = true) -> Icons.Rounded.Bolt
        category.contains("Alquiler", ignoreCase = true) || category.contains("Hogar", ignoreCase = true) || category.contains("Casa", ignoreCase = true) -> Icons.Rounded.Home
        category.contains("Compra", ignoreCase = true) -> Icons.Rounded.ShoppingBag
        category.contains("Salud", ignoreCase = true) || category.contains("Medic", ignoreCase = true) || category.contains("Farmacia", ignoreCase = true) -> Icons.Rounded.Favorite
        category.contains("Educaci", ignoreCase = true) || category.contains("Curso", ignoreCase = true) || category.contains("Libro", ignoreCase = true) -> Icons.Rounded.School
        category.contains("Entreten", ignoreCase = true) || category.contains("Cine", ignoreCase = true) || category.contains("Juego", ignoreCase = true) -> Icons.Rounded.SportsEsports
        category.contains("Ahorro", ignoreCase = true) || category.contains("Meta", ignoreCase = true) -> Icons.Rounded.Savings
        else -> Icons.AutoMirrored.Rounded.ReceiptLong
    }
}

// ==========================================
// 17. GOAL ICON HELPERS (CLEAN ZERO-EMOJI SYSTEM)
// ==========================================
data class GoalIconOption(val key: String, val label: String, val icon: ImageVector)

val goalIconOptions = listOf(
    GoalIconOption("SAVINGS", "Ahorro", Icons.Rounded.Savings),
    GoalIconOption("CAR", "Auto", Icons.Rounded.DirectionsCar),
    GoalIconOption("TRIP", "Viaje", Icons.Rounded.Flight),
    GoalIconOption("HOME", "Casa", Icons.Rounded.Home),
    GoalIconOption("TECH", "Tecnología", Icons.Rounded.Laptop),
    GoalIconOption("STUDY", "Estudio", Icons.Rounded.School),
    GoalIconOption("PHONE", "Celular", Icons.Rounded.Smartphone),
    GoalIconOption("SHOP", "Compras", Icons.Rounded.ShoppingBag),
    GoalIconOption("HEALTH", "Salud", Icons.Rounded.Favorite)
)

fun getGoalIconVector(iconKey: String): ImageVector {
    return when (iconKey) {
        "CAR", "🚗" -> Icons.Rounded.DirectionsCar
        "TRIP", "🏖️", "✈️" -> Icons.Rounded.Flight
        "HOME", "🏠" -> Icons.Rounded.Home
        "TECH", "💻" -> Icons.Rounded.Laptop
        "STUDY", "🎓" -> Icons.Rounded.School
        "PHONE", "📱" -> Icons.Rounded.Smartphone
        "SHOP", "🛍️", "💍" -> Icons.Rounded.ShoppingBag
        "HEALTH", "❤️" -> Icons.Rounded.Favorite
        else -> Icons.Rounded.Savings
    }
}

