package com.katchy.focuslive.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface BillingRepository {
    val isBillingSupported: StateFlow<Boolean>
    val premiumPlans: StateFlow<List<ProductDetails>>
    val purchaseState: StateFlow<PurchaseState>
    fun startBillingConnection()
    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails)
    fun simulatePremiumPurchase(success: Boolean, onComplete: (Boolean) -> Unit)
    fun restoreActivePurchases()
}

sealed class PurchaseState {
    object Idle : PurchaseState()
    object Loading : PurchaseState()
    data class Success(val purchase: Purchase) : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}

@Singleton
class BillingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userStatsRepository: UserStatsRepository
) : BillingRepository, PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null

    private val _isBillingSupported = MutableStateFlow(false)
    override val isBillingSupported: StateFlow<Boolean> = _isBillingSupported.asStateFlow()

    private val _premiumPlans = MutableStateFlow<List<ProductDetails>>(emptyList())
    override val premiumPlans: StateFlow<List<ProductDetails>> = _premiumPlans.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    override val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        startBillingConnection()
    }

    override fun startBillingConnection() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .enablePrepaidPlans()
                    .build()
            )
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isBillingSupported.value = true
                    queryPremiumProducts()
                    restoreActivePurchases()
                } else {
                    _isBillingSupported.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                _isBillingSupported.value = false
                // Auto-reconnect after a brief delay to recover from transient Play Services disconnects
                coroutineScope.launch {
                    kotlinx.coroutines.delay(3000)
                    startBillingConnection()
                }
            }
        })
    }

    private fun queryPremiumProducts() {
        val client = billingClient ?: return
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("brish_premium_monthly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("brish_premium_yearly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _premiumPlans.value = productDetailsList
            }
        }
    }

    override fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        val client = billingClient ?: return
        _purchaseState.value = PurchaseState.Loading

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        client.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _purchaseState.value = PurchaseState.Idle
        } else {
            _purchaseState.value = PurchaseState.Error("Error en la compra: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        coroutineScope.launch {
                            val success = syncPremiumToFirestore(true)
                            if (success) {
                                _purchaseState.value = PurchaseState.Success(purchase)
                            } else {
                                _purchaseState.value = PurchaseState.Error("Error al sincronizar con el servidor.")
                            }
                        }
                    }
                }
            } else {
                coroutineScope.launch {
                    syncPremiumToFirestore(true)
                    _purchaseState.value = PurchaseState.Success(purchase)
                }
            }
        }
    }

    private suspend fun syncPremiumToFirestore(isPremium: Boolean): Boolean {
        val currentUser = firebaseAuth.currentUser ?: return false
        return try {
            // Update Firestore
            firestore.collection("users").document(currentUser.uid)
                .set(mapOf("isPremium" to isPremium), com.google.firebase.firestore.SetOptions.merge())
                .await()
            // Update local repository cache
            userStatsRepository.setPremium(isPremium)
            true
        } catch (e: Exception) {
            android.util.Log.e("BillingRepository", "Error syncing premium to Firestore", e)
            false
        }
    }

    override fun simulatePremiumPurchase(success: Boolean, onComplete: (Boolean) -> Unit) {
        if (!com.katchy.focuslive.BuildConfig.DEBUG) {
            onComplete(false)
            return
        }
        coroutineScope.launch {
            val syncSuccess = syncPremiumToFirestore(success)
            onComplete(syncSuccess)
        }
    }

    override fun restoreActivePurchases() {
        val client = billingClient ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        client.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var hasActivePremium = false
                for (purchase in purchasesList) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        for (productId in purchase.products) {
                            if (productId == "brish_premium_monthly" || productId == "brish_premium_yearly") {
                                hasActivePremium = true
                            }
                        }
                    }
                }
                if (hasActivePremium) {
                    coroutineScope.launch {
                        syncPremiumToFirestore(true)
                    }
                }
            } else {
                android.util.Log.e("BillingRepository", "Failed to query purchases: ${billingResult.debugMessage}")
            }
        }
    }
}
