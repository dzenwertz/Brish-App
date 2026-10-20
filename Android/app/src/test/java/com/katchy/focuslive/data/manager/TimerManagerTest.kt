package com.katchy.focuslive.data.manager

import com.katchy.focuslive.data.repository.AppPreferencesRepository
import com.katchy.focuslive.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerManagerTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val preferences = mockk<AppPreferencesRepository>(relaxed = true)
    private val store = mockk<TimerStateStore>(relaxed = true)
    private val runtime = mockk<TimerRuntime>(relaxed = true)
    private var persisted: TimerSnapshot? = null
    private var now = 10_000L
    private var boot = 7
    private var owner = "user:A"
    private val owners = MutableStateFlow("user:A")
    private val managers = mutableListOf<TimerManager>()

    @Before fun setup() {
        every { preferences.workDuration } returns MutableStateFlow(25L)
        every { preferences.breakDuration } returns MutableStateFlow(5L)
        every { preferences.isPomodoroSoundEnabled } returns MutableStateFlow(false)
        every { runtime.owner() } answers { owner }
        every { runtime.elapsedMs() } answers { now }
        every { runtime.bootCount() } answers { boot }
        every { runtime.ownerChanges } returns owners
        every { store.load() } answers { persisted }
        every { store.save(any()) } answers { persisted = firstArg(); Unit }
        every { store.clear() } answers { persisted = null; Unit }
    }
    private fun manager() = TimerManager(preferences, store, runtime).also { managers += it }
    @After fun cleanup() { managers.forEach { it.dispose() } }
    private fun timerTest(block: suspend TestScope.() -> Unit) = runTest {
        try { block() } finally { managers.forEach { it.dispose() } }
    }

    @Test fun runningRestartUsesElapsedDeadline_andStartsForegroundService() = timerTest {
        val first = manager()
        first.setActiveTask("delivery")
        first.startTimer()
        first.dispose()
        now += 61_000
        val restored = manager()
        assertTrue(restored.isTimerRunning.value)
        assertEquals(1439L, restored.timeLeft.value)
        assertEquals("delivery", restored.activeTaskId.value)
        verify(atLeast = 2) { runtime.startForegroundService() }
    }

    @Test fun pausedRestartPreservesRemaining_evenAfterDeviceReboot() = timerTest {
        val first = manager()
        first.startTimer()
        now += 12_300
        first.pauseTimer()
        first.dispose()
        boot++
        now = 1_000
        val restored = manager()
        assertFalse(restored.isTimerRunning.value)
        assertEquals(1488L, restored.timeLeft.value)
    }

    @Test fun rebootRestoresRunningCheckpointPaused_notUsingOldDeadline() = timerTest {
        val first = manager()
        first.startTimer()
        now += 30_000
        advanceTimeBy(500)
        runCurrent()
        assertEquals(1470L, persisted!!.remainingSeconds)
        first.dispose()
        boot++
        now = 2_000
        val restored = manager()
        assertFalse(restored.isTimerRunning.value)
        assertEquals(1470L, restored.timeLeft.value)
    }

    @Test fun changedBootCountIsDetectedEvenIfNewUptimeExceedsOldUptime() = timerTest {
        val first = manager()
        first.startTimer()
        first.dispose()
        boot++
        now += 500_000
        val restored = manager()
        assertFalse(restored.isTimerRunning.value)
        assertEquals(1500L, restored.timeLeft.value)
    }

    @Test fun accountSwitchStopsResetsAndDiscardsOldTask_withoutCompletionCallback() = timerTest {
        val timer = manager()
        val finished = mockk<(Boolean) -> Unit>(relaxed = true)
        timer.onTimerFinished = finished
        timer.setActiveTask("A-private-task")
        timer.setAmbientSound(TimerManager.AmbientSound.RAIN)
        timer.startTimer()
        owner = "user:B"
        owners.value = owner
        runCurrent()
        assertFalse(timer.isTimerRunning.value)
        assertEquals(1500L, timer.timeLeft.value)
        assertNull(timer.activeTaskId.value)
        assertEquals(TimerManager.AmbientSound.NONE, timer.ambientSound.value)
        assertEquals("user:B", persisted!!.owner)
        verify { runtime.stopForegroundService() }
        verify(exactly = 0) { finished.invoke(any()) }
        owner = "user:A"
        owners.value = owner
        runCurrent()
        assertNull(timer.activeTaskId.value)
        assertFalse(timer.isTimerRunning.value)
    }

    @Test fun restartUnderAnotherOwnerNeverRestoresPreviousSnapshot() = timerTest {
        val first = manager()
        first.setActiveTask("A-private-task")
        first.startTimer()
        first.dispose()
        owner = "user:B"
        owners.value = owner
        val restored = manager()
        assertFalse(restored.isTimerRunning.value)
        assertNull(restored.activeTaskId.value)
        assertEquals("user:B", persisted!!.owner)
    }

    @Test fun resetAndSkipPersist_withoutCompletionEvents() = timerTest {
        val timer = manager()
        val finished = mockk<(Boolean) -> Unit>(relaxed = true)
        timer.onTimerFinished = finished
        timer.startTimer()
        now += 20_000
        timer.resetTimer()
        assertEquals(1500L, persisted!!.remainingSeconds)
        assertFalse(persisted!!.running)
        timer.skipSession()
        assertEquals("BREAK", persisted!!.state)
        assertEquals(300L, persisted!!.remainingSeconds)
        verify(exactly = 0) { finished.invoke(any()) }
    }

    @Test fun expiredRestartAdvancesOnceToPausedBreak() = timerTest {
        val first = manager()
        first.startTimer()
        first.dispose()
        now += 1500_001
        val restored = manager()
        assertFalse(restored.isTimerRunning.value)
        assertEquals(TimerManager.TimerState.BREAK, restored.timerState.value)
        assertEquals(300L, restored.timeLeft.value)
        restored.dispose()
        assertEquals(TimerManager.TimerState.BREAK, manager().timerState.value)
    }

    @Test fun durationsAndCorruptSnapshotsAreBounded() = timerTest {
        persisted = TimerSnapshot(owner, "WORK", Long.MAX_VALUE, true, Long.MAX_VALUE, now, boot)
        val timer = manager()
        assertFalse(timer.isTimerRunning.value)
        assertEquals(1500L, timer.timeLeft.value)
        timer.updateDurations(Long.MAX_VALUE, -100)
        assertEquals(MAX_TIMER_SECONDS, timer.timeLeft.value)
        timer.skipSession()
        assertEquals(60L, timer.timeLeft.value)
    }

    @Test fun writesAreCheckpointed_notWrittenOnEveryTick() = timerTest {
        val timer = manager()
        timer.startTimer()
        val checkpoint = persisted
        now += 10_000
        advanceTimeBy(500)
        runCurrent()
        assertEquals(1490L, timer.timeLeft.value)
        assertEquals(checkpoint, persisted)
        now += 20_000
        advanceTimeBy(500)
        runCurrent()
        assertEquals(1470L, persisted!!.remainingSeconds)
    }

    @Test fun unavailableBootIdentityOrElapsedRollbackRestoresPaused() {
        val snapshot = TimerSnapshot(owner, "WORK", 1500, true, now + 1500_000, now, boot)
        assertFalse(restoreTimerSnapshot(snapshot, owner, now + 1000, null)!!.running)
        assertFalse(restoreTimerSnapshot(snapshot, owner, now - 1000, boot)!!.running)
    }

    @Test fun liveCompletionAdvancesAndNotifiesExactlyOnce() = timerTest {
        val timer = manager()
        val finished = mockk<(Boolean) -> Unit>(relaxed = true)
        timer.onTimerFinished = finished
        timer.startTimer()
        now += 1500_000
        advanceTimeBy(500)
        runCurrent()
        assertEquals(TimerManager.TimerState.BREAK, timer.timerState.value)
        assertFalse(timer.isTimerRunning.value)
        assertEquals("BREAK", persisted!!.state)
        advanceTimeBy(1000)
        runCurrent()
        verify(exactly = 1) { finished.invoke(true) }
        verify(exactly = 1) { runtime.notifyCompletion(true) }
    }

    @Test fun addMinutesExtendsRunningSessionDeadline() = timerTest {
        val timer = manager()
        timer.startTimer()
        now += 60_000
        timer.addMinutes(5)
        assertEquals(1740L, timer.timeLeft.value)
        now += 1_500_000 // 25 min: sin el +5 ya habría terminado
        advanceTimeBy(500)
        runCurrent()
        assertTrue(timer.isTimerRunning.value)
        assertEquals(240L, timer.timeLeft.value)
        now += 240_000
        advanceTimeBy(500)
        runCurrent()
        assertFalse(timer.isTimerRunning.value)
        assertEquals(TimerManager.TimerState.BREAK, timer.timerState.value)
    }

    @Test fun addMinutesWhilePausedAddsToRemaining() = timerTest {
        val timer = manager()
        timer.addMinutes(5)
        assertEquals(1800L, timer.timeLeft.value)
        assertFalse(timer.isTimerRunning.value)
    }
}
