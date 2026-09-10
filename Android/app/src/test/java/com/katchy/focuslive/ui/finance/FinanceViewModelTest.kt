package com.katchy.focuslive.ui.finance

import com.katchy.focuslive.data.repository.AppPreferencesRepository
import com.katchy.focuslive.data.repository.FinanceRepository
import com.katchy.focuslive.data.repository.GoalRepository
import com.katchy.focuslive.data.repository.MascotRepository
import com.katchy.focuslive.data.repository.UserStatsRepository
import com.katchy.focuslive.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FinanceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: FinanceViewModel
    private val financeRepository: FinanceRepository = mockk(relaxed = true)
    private val goalRepository: GoalRepository = mockk(relaxed = true)
    private val mascotRepository: MascotRepository = mockk(relaxed = true)
    private val appPreferencesRepository: AppPreferencesRepository = mockk(relaxed = true)
    private val userStatsRepository: UserStatsRepository = mockk(relaxed = true)

    @Before
    fun setup() {
        viewModel = FinanceViewModel(
            financeRepository,
            goalRepository,
            mascotRepository,
            appPreferencesRepository,
            userStatsRepository
        )
    }

    @Test
    fun addTransaction_withNegativeAmount_doesNotCallRepository() = runTest {
        // Arrange
        val title = "Invalid Expense"
        val amount = -50.0
        val type = "EXPENSE"
        val category = "Food"

        // Act
        viewModel.addTransaction(title, amount, type, category)

        // Assert
        coVerify(exactly = 0) { financeRepository.addTransaction(any()) }
    }

    @Test
    fun addTransaction_withZeroAmount_doesNotCallRepository() = runTest {
        // Arrange
        val title = "Zero Expense"
        val amount = 0.0
        val type = "EXPENSE"
        val category = "Food"

        // Act
        viewModel.addTransaction(title, amount, type, category)

        // Assert
        coVerify(exactly = 0) { financeRepository.addTransaction(any()) }
    }

    @Test
    fun addTransaction_withValidAmount_callsRepository() = runTest {
        // Arrange
        val title = "Valid Expense"
        val amount = 50.0
        val type = "EXPENSE"
        val category = "Food"

        // Act
        viewModel.addTransaction(title, amount, type, category)

        // Assert
        coVerify(exactly = 1) { financeRepository.addTransaction(any()) }
    }
}
