package com.katchy.focuslive.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefsRepository: AppPreferencesRepository
) : ViewModel() {

    fun saveUserName(name: String) {
        viewModelScope.launch {
            prefsRepository.setUserName(name)
        }
    }
}
