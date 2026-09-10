package com.katchy.focuslive.ui.main

import androidx.lifecycle.ViewModel
import com.katchy.focuslive.data.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {
    val isPlannerEnabled = appPreferencesRepository.isPlannerEnabled
    val isNotesEnabled = appPreferencesRepository.isNotesEnabled
    val isFinanceEnabled = appPreferencesRepository.isFinanceEnabled
    val isHabitsEnabled = appPreferencesRepository.isHabitsEnabled
}
