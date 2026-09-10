package com.katchy.focuslive.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.model.MascotType
import com.katchy.focuslive.data.repository.MascotRepository
import com.katchy.focuslive.data.repository.UserStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MascotSelectionViewModel @Inject constructor(
    private val mascotRepository: MascotRepository,
    private val userStatsRepository: UserStatsRepository
) : ViewModel() {

    val selectedMascot: StateFlow<MascotType> = mascotRepository.selectedMascot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MascotType.POPPIN)

    val unlockedMascotIds: StateFlow<Set<String>> = mascotRepository.unlockedMascotIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf(MascotType.POPPIN.id, MascotType.LLAMA.id))

    val mascotXpMap: StateFlow<Map<String, Int>> = mascotRepository.mascotXpMap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun getMascotLevel(xp: Int): Int {
        return mascotRepository.getMascotLevel(xp)
    }

    fun getMascotProgress(xp: Int): Float {
        return (xp % 100) / 100f
    }

    val coins: StateFlow<Int> = userStatsRepository.userStats
        .map { it.coins }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val streakFreezes: StateFlow<Int> = userStatsRepository.userStats
        .map { it.streakFreezes }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isPremium: StateFlow<Boolean> = userStatsRepository.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun buyStreakFreeze(): Boolean {
        return userStatsRepository.buyStreakFreeze(50)
    }

    fun setMascot(mascot: MascotType) {
        mascotRepository.setMascot(mascot)
    }

    fun unlockMascot(mascot: MascotType): Boolean {
        if (userStatsRepository.spendCoins(mascot.coinCost)) {
            mascotRepository.unlockMascot(mascot.id)
            return true
        }
        return false
    }
}
