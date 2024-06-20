package com.alanvo.test.googlebilling.ui.home

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alanvo.test.googlebilling.common.theme.TestBillingTheme
import com.alanvo.test.googlebilling.features.billingClient.BillingHelper
import com.alanvo.test.googlebilling.features.billingClient.BillingHelper.Companion.SINGLE_PROGRAM_ID
import com.alanvo.test.googlebilling.features.billingClient.BillingHelper.Companion.SUBSCRIPTION_TYPE_01
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()

    private val billingCallback = object : BillingHelper.BillingCallback {
        override fun onSuccess(purchase: Purchase) {
            Log.d("TestAlan", "home screen - paid success")
        }

        override fun onFailure(message: String?) {
            Log.e("TestAlan", "home screen - paid failed")
            Toast.makeText(this@MainActivity, "Paid failed", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        BillingHelper.instance.initialize(this)

        setContent {
            TestBillingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        BillingHelper.instance.purchase(
                            productType = BillingClient.ProductType.INAPP,
                            productId = SINGLE_PROGRAM_ID,
                            callback = billingCallback,
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        BillingHelper.instance.dispose()
        super.onDestroy()
    }

    @Composable
    internal fun Greeting(
        name: String,
        modifier: Modifier = Modifier,
        onClicked: () -> Unit
    ) {
        val isReady by viewModel.billingReadyState.collectAsStateWithLifecycle(initialValue = false)

        SideEffect {
            Log.d("TestAlan", "home screen - isReady $isReady")
        }

        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Hello $name!",
                style = TextStyle(color = if (isReady) Color.White else Color.Black),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(color = if (isReady) Color.Blue else Color.Gray)
                    .clickable(
                        enabled = isReady
                    ) {
                        onClicked()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

}
