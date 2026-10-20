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

    @Test fun home_displaysAgendaDeliveries_andFocusAction() {
        var openedFocus = false
        composeTestRule.setContent {
            MaterialTheme {
                HomeContent(state = HomeViewModel.HomeState(), onNavigateToFocus = { openedFocus = true })
            }
        }
        composeTestRule.onNodeWithText("Agenda de hoy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Entregas pendientes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Iniciar enfoque").performClick()
        assertTrue(openedFocus)
    }

    @Test fun task_supportsCompletion() {
        var toggled = false
        composeTestRule.setContent {
            MaterialTheme { HomeTaskItem(Task(title = "Leer capítulo"), onToggle = { toggled = true }) }
        }
        composeTestRule.onNodeWithText("Leer capítulo").assertIsDisplayed().performClick()
        assertTrue(toggled)
    }

    @Test fun quickAdd_closesOnlyAfterPersistenceCallback() {
        var saved: (() -> Unit)? = null
        composeTestRule.setContent {
            MaterialTheme {
                HomeContent(state = HomeViewModel.HomeState(), onAddTask = { _, success -> saved = success })
            }
        }
        composeTestRule.onNodeWithText("Añadir tarea").performClick()
        composeTestRule.onNodeWithText("Título").performTextInput("Leer capítulo")
        composeTestRule.onNodeWithText("Guardar").performClick()
        composeTestRule.onNodeWithText("Tarea para hoy").assertIsDisplayed()
        composeTestRule.runOnIdle { saved?.invoke() }
        composeTestRule.onNodeWithText("Tarea para hoy").assertDoesNotExist()
    }

    @Test fun todayTask_canBecomeFocusSource() {
        var focused = false
        composeTestRule.setContent {
            MaterialTheme { HomeTaskItem(Task(title = "Leer capítulo"), onToggle = {}, onFocus = { focused = true }) }
        }
        composeTestRule.onNodeWithContentDescription("Enfocar Leer capítulo").performClick()
        assertTrue(focused)
    }

    @Test fun overdueDelivery_isLabeledAndCanBecomeFocusSource() {
        val delivery = Task(id = "delivery", title = "Entregar ensayo", courseId = "course", dueDate = System.currentTimeMillis() - 86_400_000L)
        var focused = false
        composeTestRule.setContent {
            MaterialTheme {
                HomeContent(
                    state = HomeViewModel.HomeState(upcomingDeliveries = listOf(HomeViewModel.UpcomingDelivery(delivery, "Literatura"))),
                    onFocusTask = { focused = it.id == delivery.id }
                )
            }
        }
        composeTestRule.onNodeWithText("Vencida ·", substring = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Enfocar Entregar ensayo").performClick()
        assertTrue(focused)
    }
}
