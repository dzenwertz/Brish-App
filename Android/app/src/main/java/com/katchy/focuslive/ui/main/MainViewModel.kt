package com.katchy.focuslive.ui.main

import androidx.lifecycle.ViewModel
import com.katchy.focuslive.data.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val prefs: AppPreferencesRepository
) : ViewModel() {
    val guideShown = prefs.guideShown

    fun finishGuide() = prefs.setGuideShown(true)
}
