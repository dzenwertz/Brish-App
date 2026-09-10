package com.katchy.focuslive.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.model.Transaction
import com.katchy.focuslive.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val goalRepository: com.katchy.focuslive.data.repository.GoalRepository,
    private val mascotRepository: com.katchy.focuslive.data.repository.MascotRepository,
    private val appPreferencesRepository: com.katchy.focuslive.data.repository.AppPreferencesRepository,
    private val userStatsRepository: com.katchy.focuslive.data.repository.UserStatsRepository
) : ViewModel() {

    val selectedMascot = mascotRepository.selectedMascot


    private val _filterType = kotlinx.coroutines.flow.MutableStateFlow("ALL") // "ALL", "EXPENSE", "INCOME"
    val filterType: StateFlow<String> = _filterType

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    val transactions: StateFlow<List<Transaction>> = financeRepository.getTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTransaction(
        title: String, 
        amount: Double, 
        type: String, 
        category: String, 
        isRecurring: Boolean = false, 
        recurrenceInterval: String = "NONE",
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (title.isBlank() || amount <= 0) return
        viewModelScope.launch {
            val transaction = Transaction(
                title = title.trim(),
                amount = amount,
                type = type,
                category = category,
                isRecurring = isRecurring,
                recurrenceInterval = recurrenceInterval,
                timestamp = timestamp
            )
            financeRepository.addTransaction(transaction)
            
            // Gamification feedback
            try {
                userStatsRepository.addXp(5)
                mascotRepository.addXpToSelectedMascot(5)
                com.katchy.focuslive.ui.util.MascotEventBus.emit(com.katchy.focuslive.ui.util.MascotEvent.TASK_DONE)
            } catch (e: Exception) {
                // Ignore gamification failures
            }
            
            checkFinancierBadge()
        }
    }

    private suspend fun checkFinancierBadge() {
        if (financeRepository.getTransactionCount() >= 30) {
            userStatsRepository.unlockBadge(com.katchy.focuslive.util.GamificationLogic.BADGE_FINANCIER)
        }
    }

    fun updateTransaction(
        transaction: Transaction,
        newTitle: String,
        newAmount: Double,
        newCategory: String,
        newRecurrence: String,
        newType: String = transaction.type
    ) {
        viewModelScope.launch {
            val updatedTransaction = transaction.copy(
                title = newTitle, 
                amount = newAmount, 
                category = newCategory,
                type = newType,
                isRecurring = newRecurrence != "NONE",
                recurrenceInterval = newRecurrence
            )
            financeRepository.updateTransaction(updatedTransaction)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            financeRepository.deleteTransaction(id)
        }
    }

    // --- Savings Goals (Persistent via GoalRepository) ---
    
    val savingsGoals: StateFlow<List<com.katchy.focuslive.data.model.Goal>> = goalRepository.getGoals()
        .map { goals -> goals.filter { it.type == "SAVINGS" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSavingsGoal(title: String, targetAmount: Double, icon: String, colorHex: Long) {
        if (title.isBlank() || targetAmount <= 0) return
        viewModelScope.launch {
            val goal = com.katchy.focuslive.data.model.Goal(
                title = title,
                targetAmount = targetAmount,
                currentAmount = 0.0,
                type = "SAVINGS",
                icon = icon,
                colorHex = String.format(java.util.Locale.US, "#%06X", (0xFFFFFF and colorHex.toInt()))
            )
            goalRepository.addGoal(goal)
        }
    }

    fun depositToGoal(goal: com.katchy.focuslive.data.model.Goal, amount: Double) {
        if (amount <= 0) return
        viewModelScope.launch {
            val updatedAmount = (goal.currentAmount + amount).coerceAtMost(goal.targetAmount)
            goalRepository.updateGoal(goal.copy(currentAmount = updatedAmount))

            val transaction = Transaction(
                title = "Depósito a meta: ${goal.title}",
                amount = amount,
                type = "EXPENSE",
                category = "Ahorro",
                timestamp = System.currentTimeMillis()
            )
            financeRepository.addTransaction(transaction)
            
            if (updatedAmount >= goal.targetAmount && goal.currentAmount < goal.targetAmount) {
                // Goal reached celebration!
                try {
                    userStatsRepository.addXp(25)
                    mascotRepository.addXpToSelectedMascot(25)
                    com.katchy.focuslive.ui.util.MascotEventBus.emit(com.katchy.focuslive.ui.util.MascotEvent.BADGE_UNLOCKED)
                } catch (e: Exception) {
                    // Ignore gamification failures
                }
            } else {
                try {
                    userStatsRepository.addXp(5)
                    mascotRepository.addXpToSelectedMascot(5)
                    com.katchy.focuslive.ui.util.MascotEventBus.emit(com.katchy.focuslive.ui.util.MascotEvent.TASK_DONE)
                } catch (e: Exception) {
                    // Ignore gamification failures
                }
            }
            
            checkFinancierBadge()
        }
    }

    fun updateSavingsGoal(goal: com.katchy.focuslive.data.model.Goal, newTitle: String, newTarget: Double, newIcon: String, newColor: Long) {
        viewModelScope.launch {
            val updatedGoal = goal.copy(
                title = newTitle,
                targetAmount = newTarget,
                icon = newIcon,
                colorHex = String.format(java.util.Locale.US, "#%06X", (0xFFFFFF and newColor.toInt()))
            )
            goalRepository.updateGoal(updatedGoal)
        }
    }

    fun deleteSavingsGoal(id: String) {
        viewModelScope.launch {
            goalRepository.deleteGoal(id)
        }
    }
}
