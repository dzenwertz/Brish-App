package com.katchy.focuslive.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.katchy.focuslive.data.model.Task
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    private val web = Task(id = "web", title = "Proyecto web", time = "15:00", endTime = "17:00", category = "Trabajo")

    @Test fun currentBlock_offersFocus() {
        var focused = false
        composeTestRule.setContent {
            MaterialTheme {
                HomeContent(plan = planToday(listOf(web), nowMinutes = 16 * 60), onFocusTask = { focused = it.id == "web" })
            }
        }
        composeTestRule.onNodeWithText("Proyecto web").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enfocar").performClick()
        assertTrue(focused)
    }

    @Test fun pendingTask_togglesOnTap() {
        var toggled = false
        composeTestRule.setContent {
            MaterialTheme {
                HomeContent(plan = planToday(listOf(Task(id = "t", title = "Pagar la luz")), nowMinutes = 600), onToggleTask = { toggled = true })
            }
        }
        composeTestRule.onNodeWithText("Pagar la luz").performScrollTo().performClick()
        assertTrue(toggled)
    }

    @Test fun emptyDay_invitesToCapture() {
        var captured = false
        composeTestRule.setContent {
            MaterialTheme { HomeContent(plan = TodayPlan(), onCapture = { captured = true }) }
        }
        composeTestRule.onNodeWithText("Tu día está libre").assertIsDisplayed()
        composeTestRule.onNodeWithText("Capturar algo").performClick()
        assertTrue(captured)
    }
}
