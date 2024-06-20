package com.alanvo.test.googlebilling.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alanvo.test.googlebilling.features.billingClient.BillingHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    val billingReadyState = BillingHelper.instance.readyState
        .shareIn(
            scope = viewModelScope,
            replay = 0,
            started = SharingStarted.WhileSubscribed()
        )

}
