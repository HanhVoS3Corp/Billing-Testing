package com.alanvo.test.googlebilling.features.billingClient

import com.android.billingclient.api.BillingClient

fun Int.toBillingMsg(): String =
    when (this) {
        BillingClient.BillingResponseCode.OK -> "OK"
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "ITEM_ALREADY_OWNED"
        BillingClient.BillingResponseCode.USER_CANCELED -> "USER_CANCELED"
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "BILLING_UNAVAILABLE"
        BillingClient.BillingResponseCode.ERROR -> "ERROR"
        BillingClient.BillingResponseCode.NETWORK_ERROR -> "NETWORK_ERROR"
        BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "DEVELOPER_ERROR"
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> "FEATURE_NOT_SUPPORTED"
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "FEATURE_NOT_SUPPORTED"
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> "FEATURE_NOT_SUPPORTED"
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "FEATURE_NOT_SUPPORTED"
        else -> "UNIDENTIFIED"
    }
