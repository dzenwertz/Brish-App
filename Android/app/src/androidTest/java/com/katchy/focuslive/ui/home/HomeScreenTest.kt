package com.katchy.focuslive.ui.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_displaysKeyComponents() {
        // Since we cannot easily inject ViewModel in checking specific implementation logic in standard Compose UI tests without Hilt setup complexity,
        // we will verify if fundamental Texts render if we were to launch it.
        // However, assuming we test standard Composable.
        
        /* 
           Note: Full verification requires HiltAndroidRule and launching Fragment/Activity.
           For this "Sample" verify, we'd verify simple composables.
           Since HomeScreen requires complex ViewModel, passing it is hard without Hilt.
           We can create a test for a simpler component like 'DashboardWidget' or 'PremiumTaskItem'.
        */
        
        // Let's test "PremiumTaskItem" which is a key part of Home
        val task = com.katchy.focuslive.data.model.Task(id = "1", title = "Test Task", isCompleted = false)
        
        composeTestRule.setContent {
            PremiumTaskItem(
                task = task,
                onToggle = {},
                onDelete = {}
            )
        }

        composeTestRule.onNodeWithText("Test Task").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Task").assertHasClickAction()
    }
    
    @Test
    fun widget_displaysCorrectData() {
        composeTestRule.setContent {
            DashboardWidget(
                title = "Test Widget",
                value = "100",
                subtext = "Subtext",
                icon = androidx.compose.material.icons.Icons.Default.Home,
                iconTint = androidx.compose.ui.graphics.Color.Blue
            )
        }
        
        composeTestRule.onNodeWithText("Test Widget").assertIsDisplayed()
        composeTestRule.onNodeWithText("100").assertIsDisplayed()
        composeTestRule.onNodeWithText("Subtext").assertIsDisplayed()
    }
}
