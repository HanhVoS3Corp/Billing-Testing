package com.alanvo.test.googlebilling.features.billingClient

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillingHelper : PurchasesUpdatedListener, BillingClientStateListener {
    val scope = CoroutineScope(Job())

    var billingClient: BillingClient? = null
    var billingCallback: BillingCallback? = null
    var lastSkuDetails: SkuDetails? = null

    fun initialize(context: Context) {
        try {
            val pendingParams = PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            billingClient =
                BillingClient.newBuilder(context).enablePendingPurchases(pendingParams).setListener(this).build()

            billingClient?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    scope.launch {
                        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
                        billingClient?.queryPurchasesAsync(params) { a, b ->

                        }
                    }
                }

                override fun onBillingServiceDisconnected() {

                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBillingServiceDisconnected() {

    }

    override fun onBillingSetupFinished(p0: BillingResult) {

    }

    fun purchase(sku: String, activity: FragmentActivity, callback: BillingCallback) {
        billingCallback = callback

        val skuList = ArrayList<String>()
        skuList.add(sku)
        val params = SkuDetailsParams.newBuilder()
        var type = BillingClient.SkuType.SUBS
        if (sku == SKU_UNLIMITED || sku == SKU_UNLIMITED_SALE
            || sku == SKU_ANNUAL_TO_UNLIMITED || sku == SKU_MONTHLY_TO_UNLIMITED
        ) {
            type = BillingClient.SkuType.INAPP
        }
        params.setSkusList(skuList).setType(type)
        billingClient?.querySkuDetailsAsync(
            params.build()
        ) { billingResult, detailsList ->
            Log.d("BillingClient", billingResult.responseCode.toString())
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !detailsList.isNullOrEmpty()) {
                val flowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(detailsList.first())
                    .build()
                billingClient?.launchBillingFlow(activity, flowParams)
                lastSkuDetails = detailsList.firstOrNull()
            } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
                billingCallback?.onFailure("Purchase failed. Please contact support for help")
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

    fun abc() {
        billingClient?.let {
            val product1 =
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(SKU_ANNUAL)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            val product2 =
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(SKU_SIX_MONTHLY)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            val queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        arrayListOf(product1, product2)
                    )
                    .build()

            it.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList: MutableList<ProductDetails> ->
                // check billingResult
                // process returned productDetailsList
                Log.d("TestAlan", "billingResult $billingResult")
                productDetailsList.forEach { details ->
                    details.subscriptionOfferDetails
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
                        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { _result: BillingResult ->
                            if (_result.responseCode == BillingClient.BillingResponseCode.OK) {
                                billingCallback?.onSuccess(purchase)
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

    interface BillingCallback {
        fun onSuccess(purchase: Purchase)
        fun onFailure(message: String?)
    }

    companion object {
        val instance: BillingHelper by lazy { BillingHelper() }

        const val SKU_SUB_SINGLE_PLAN = "com.ganbaru.method.3.month.subscription"
        const val SKU_SUB_ALL_PROGRAMS = "com.ganbaru.method.3.yearly"
        const val SKU_SUB_ALL_PROGRAMS_SALE = "com.ganbaru.method.2.subscription.all.programs.sale"
        const val SKU_UNLIMITED_SALE = "com.ganbaru.method.full.access.sale"
        const val SKU_UNLIMITED = "com.ganbaru.method.unlimited"
        const val SKU_ANNUAL_TO_UNLIMITED = "com.ganbaru.method.2.upgrade.annual.to.lifetime"
        const val SKU_MONTHLY_TO_UNLIMITED = "com.ganbaru.method.2.upgrade.monthly.to.lifetime"
        const val SKU_MONTHLY_TO_ANNUAL = "com.ganbaru.method.2.upgrade.monthly.to.annual"
        const val SKU_SUB_MONTH_PLAN = "com.ganbaru.method.3.month.subscription"
        const val SKU_SUB_3MONTH_1W_FREE_PLAN = "com.ganbaru.method.3months.subscription"
        const val SKU_SUB_TESTING = "com.ganbaru.method.internal.testing"

        //the above is confusing, keep new separate, this is yearly
        const val SKU_ANNUAL = "com.ganbaru.method.3.yearly"

        //6 monthly
        const val SKU_SIX_MONTHLY = "com.ganbaru.method.3.six.month.sub"

        //this is monthly
        const val SKU_MONTHLY = "com.ganbaru.method.3.month.subscription"

        fun isSpringSale(): Boolean {
            val startDateStr = "26/11/2021" // "26/04/2021"
            val endDateStr = "30/11/2021" // midnight 2nd may "03/05/2021"
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val minDate = sdf.parse(startDateStr)
            val maxDate = sdf.parse(endDateStr)
            val now = Date()
            return (now > minDate && now < maxDate)
        }

        fun getAllProgramsSku(): String {
            if (isSpringSale()) {
                return SKU_SUB_ALL_PROGRAMS_SALE
            }
            return SKU_SUB_ALL_PROGRAMS
        }

        fun getUnlimitedSku(): String {
            return SKU_UNLIMITED
        }
    }
}
