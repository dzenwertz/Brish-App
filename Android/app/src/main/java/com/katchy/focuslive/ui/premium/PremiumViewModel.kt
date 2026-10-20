package com.katchy.focuslive.ui.premium

import androidx.lifecycle.ViewModel
import com.katchy.focuslive.data.repository.BillingRepository
import com.katchy.focuslive.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    subscriptionRepository: SubscriptionRepository
) : ViewModel() {
    val isPremium = subscriptionRepository.isPremium
    val purchaseState = billingRepository.purchaseState

    fun restoreActivePurchases() {
        billingRepository.restoreActivePurchases()
    }
}
