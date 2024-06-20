package com.alanvo.test.googlebilling.features.billingClient

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingHelper : PurchasesUpdatedListener, BillingClientStateListener {
    private val scope = CoroutineScope(Job())
    private val _readyState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val readyState: StateFlow<Boolean> = _readyState.asStateFlow()

    private var billingClient: BillingClient? = null
    private var billingCallback: BillingCallback? = null

    fun initialize(context: Context) {
        try {
            billingClient =
                BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build()

            billingClient?.startConnection(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBillingServiceDisconnected() {
        _readyState.update { false }
        billingClient?.startConnection(this)
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        when (val responseCode = result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                scope.launch {
                    queryProducts(
                        productType = BillingClient.ProductType.INAPP,
                        productId = SINGLE_PROGRAM_ID,
                    )
                    queryPurchases(
                        productType = BillingClient.ProductType.INAPP,
                    )
                    _readyState.update { true }
                }
            }

            else -> {
                Log.d("TestAlan", "onBillingSetupFinished - responseCode: $responseCode - failed case")
                _readyState.update { false }
            }
        }
    }

    private suspend fun queryProducts(productType: String, productId: String) {
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(productType)
                        .build()
                )
            )
            .build()

        val productDetailsResult: ProductDetailsResult? = withContext(Dispatchers.IO) {
            billingClient?.queryProductDetails(queryProductDetailsParams)
        }

        val productDetailsList: List<ProductDetails>? = productDetailsResult?.productDetailsList

        when (val responseCode = productDetailsResult?.billingResult?.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.d("TestAlan", "queryProducts - result ok")
                if (productDetailsList?.isNotEmpty() == true) {
                    productDetailsList.forEach { productDetails ->
                        printProductDetails(productDetails)
                    }


                }
            }

            else -> {
                Log.d("TestAlan", "queryProducts - response is not ok ${responseCode?.toBillingMsg()}")
                Log.d("TestAlan", "queryProducts - billingResult?.debugMessage ${productDetailsResult?.billingResult?.debugMessage}")
            }
        }
    }

    private fun printProductDetails(productDetails: ProductDetails) {
        Log.d("TestAlan", "queryProducts - productDetails - productId: ${productDetails.productId} - productType: ${productDetails.productType} - title: ${productDetails.title} " +
                "- description: $${productDetails.description} - oneTimePurchaseOfferDetails: ${productDetails.oneTimePurchaseOfferDetails}")
    }

    private suspend fun queryPurchases(productType: String) {
        if (billingClient?.isReady == false) {
            Log.d("TestAlan", "queryPurchases - billingClient is not ready")
            return
        }

        val queryPurchaseParams = QueryPurchasesParams.newBuilder()
            .setProductType(productType)
            .build()

        val result: PurchasesResult? = withContext(Dispatchers.IO) {
            billingClient?.queryPurchasesAsync(queryPurchaseParams)
        }

        when (val responseCode = result?.billingResult?.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.d("TestAlan", "queryPurchases - result ok")
            }

            else -> {
                Log.d("TestAlan", "queryPurchases - response is not ok ${responseCode?.toBillingMsg()}")
                Log.d("TestAlan", "queryPurchases - billingResult?.debugMessage ${result?.billingResult?.debugMessage}")
            }
        }
    }

    fun purchase(productType: String, productId: String, activity: Activity, callback: BillingCallback) {
        scope.launch {
            billingCallback = callback
            val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(productType)
                            .build()
                    )
                )
                .build()

            val productDetailsResult: ProductDetailsResult? = withContext(Dispatchers.IO) {
                billingClient?.queryProductDetails(queryProductDetailsParams)
            }

            val productDetailsList: List<ProductDetails>? = productDetailsResult?.productDetailsList

            when (val responseCode = productDetailsResult?.billingResult?.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    Log.d("TestAlan", "queryProducts - result ok")
                    if (productDetailsList?.isNotEmpty() == true) {
                        val productDetailsParamsList = mutableListOf<BillingFlowParams.ProductDetailsParams>()
                        productDetailsList.forEach { productDetails ->
                            printProductDetails(productDetails)

                            productDetailsParamsList.add(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                                    .setProductDetails(productDetails)
                                    .build()
                            )
                        }

                        val billingFlowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(productDetailsParamsList)
                            .build()

                        billingClient?.launchBillingFlow(activity, billingFlowParams)
                    } else {
                        Toast.makeText(activity, "Can not get information of this purchase", Toast.LENGTH_LONG).show()
                    }
                }

                else -> {
                    Log.d("TestAlan", "queryProducts - response is not ok ${responseCode?.toBillingMsg()}")
                    Log.d("TestAlan", "queryProducts - billingResult?.debugMessage ${productDetailsResult?.billingResult?.debugMessage}")
                    Toast.makeText(activity, "Purchase failed. Please contact support for help", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun restore(skuType: String, callback: BillingCallback) {
        billingClient?.let { billingClient ->
            billingCallback = callback
            billingClient.queryPurchasesAsync(skuType) { _, purchasesList ->
                if (purchasesList.isEmpty()) billingCallback?.onFailure("You do not have any purchases")
                for (purchase in purchasesList) {
                    if (checkPurchaseStatusIsOk(purchase)) {
                        checkAcknowledgeStatus(purchase, billingClient)
                    }
                }
            }
        }
    }

    private fun checkAcknowledgeStatus(purchase: Purchase, billingClient: BillingClient) {
        if (purchase.isAcknowledged) billingCallback?.onSuccess(purchase)
        else getPurchaseAcknowledged(purchase, billingClient)
    }

    private fun getPurchaseAcknowledged(purchase: Purchase, billingClient: BillingClient) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken).build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingCallback?.onSuccess(purchase) }
    }

    private fun checkPurchaseStatusIsOk(purchase: Purchase): Boolean {
        return if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            || purchase.purchaseState == Purchase.PurchaseState.PENDING
        ) {
            true
        } else {
            billingCallback?.onFailure("Restore failed. Please contact support for help")
            false
        }
    }

    // acknowledge: https://stackoverflow.com/questions/56289258/how-does-acknowledgepurchase-really-work-on-android
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if ((billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    || billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)
            && !purchases.isNullOrEmpty()
        ) {
            for (purchase: Purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED || purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                    if (!purchase.isAcknowledged) {
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken).build()

                        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { acknowledgeResult: BillingResult ->
                            when (acknowledgeResult.responseCode) {
                                BillingClient.BillingResponseCode.OK -> {
                                    billingCallback?.onSuccess(purchase)
                                }
                                else -> {
                                    // TODO: should try to acknowledge the purchase again
//                                    billingCallback?.onFailure("User canceled purchase")
                                }
                            }
                        }
                    } else {
                        billingCallback?.onSuccess(purchase)
                    }
//                    logPurchaseEvent()
                } else {
                    billingCallback?.onFailure("Purchase failed. Please contact support for help")
                }
            }
        } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                billingCallback?.onFailure("You're already subscribed. Please try Restore Purchases or contact support for help")
            } else {
                billingCallback?.onFailure("Purchase failed. Please contact support for help")
            }
        }
    }

    fun dispose() {
        billingClient?.endConnection()
    }

    interface BillingCallback {
        fun onSuccess(purchase: Purchase)
        fun onFailure(message: String?)
    }

    companion object {
        val instance: BillingHelper by lazy { BillingHelper() }

        const val SINGLE_PROGRAM_ID = "com.vnhanh.testing.program.single"
    }
}
