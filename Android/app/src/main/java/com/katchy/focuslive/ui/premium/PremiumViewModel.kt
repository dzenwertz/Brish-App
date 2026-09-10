package com.katchy.focuslive.ui.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.katchy.focuslive.data.repository.BillingRepository
import com.katchy.focuslive.data.repository.PurchaseState
import com.katchy.focuslive.data.repository.UserStatsRepository
import com.katchy.focuslive.data.repository.MascotRepository
import com.katchy.focuslive.data.model.UserStats
import com.katchy.focuslive.data.model.MascotType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val userStatsRepository: UserStatsRepository,
    private val mascotRepository: MascotRepository
) : ViewModel() {

    val selectedMascot: StateFlow<MascotType> = mascotRepository.selectedMascot
    
    val userStats: StateFlow<UserStats> = userStatsRepository.userStats

    val isPremium: StateFlow<Boolean> = userStatsRepository.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val purchaseState: StateFlow<PurchaseState> = billingRepository.purchaseState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PurchaseState.Idle)

    val premiumPlans: StateFlow<List<ProductDetails>> = billingRepository.premiumPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        billingRepository.launchPurchaseFlow(activity, productDetails)
    }

    fun simulatePremiumPurchase(success: Boolean, onComplete: (Boolean) -> Unit = {}) {
        if (!com.katchy.focuslive.BuildConfig.DEBUG) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            billingRepository.simulatePremiumPurchase(success) { result ->
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(result)
                }
            }
        }
    }
}
