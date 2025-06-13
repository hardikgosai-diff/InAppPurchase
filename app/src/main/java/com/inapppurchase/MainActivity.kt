package com.inapppurchase

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.wdtheprovider.inapppurchase.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var billingClient: BillingClient

    private val productId = "fb_remove_ads_lifetime"
    //private val productId = "com.app.veganbowls.monthly"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()
        //setupBillingClient()
    }

    private fun initUI() {
        binding.bt.setOnClickListener {
            setupBillingClient()
        }
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases()
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    handlePurchaseUpdates(purchases)
                } else {
                    Toast.makeText(this@MainActivity, "Else2", Toast.LENGTH_SHORT).show()
                }
            }
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                } else {
                    println("${billingResult.responseCode}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Toast.makeText(this@MainActivity, "Billing disconnected", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                launchPurchaseFlow(productDetails)
            } else {
                println("Product not found")
            }
        }
    }

    private fun launchPurchaseFlow(productDetails: ProductDetails) {

        val offerToken =
            productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""

        val productDetailsParams = if (offerToken != "") {
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        } else {
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(this, billingFlowParams)
    }

    private fun handlePurchaseUpdates(purchases: List<Purchase>?) {
        purchases?.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Toast.makeText(this, "Purchase successful!", Toast.LENGTH_SHORT).show()
                // Unlock your premium feature here
            }
        }
    }

    override fun onDestroy() {
        if (::billingClient.isInitialized && billingClient.isReady) {
            billingClient.endConnection()
        }
        super.onDestroy()
    }

}