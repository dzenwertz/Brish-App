package com.katchy.focuslive

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = "com.brishwlaz", // Per build.gradle.kts
            includeInStartupProfile = true
        ) {
            // Start the app and trigger critical user flows
            pressHome()
            startActivityAndWait()
            
            // Scroll Home
            // device.findObject(By.res("task_list")).scroll(Direction.DOWN, 1.0f)
            
            // Open Timer
            // ...
        }
    }
}
