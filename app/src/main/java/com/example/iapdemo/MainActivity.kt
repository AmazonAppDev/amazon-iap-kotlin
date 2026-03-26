/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.example.iapdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.SharedPreferences
import android.graphics.Color
import com.amazon.device.iap.PurchasingService
import android.util.Log
import android.widget.Toast
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.UserDataResponse
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.iapdemo.databinding.ActivityMainBinding
import java.util.*

const val parentSKU = "techsubscription"

private const val TAG = "KOTLIN_INTEGRATION"
private const val PREFS_NAME = "iap_cache"
private const val PREFS_KEY_PRODUCTS = "cached_product_skus"
private const val PREFS_KEY_SUBSCRIPTION = "subscription_active"

class MainActivity : AppCompatActivity() {

    private lateinit var currentUserId: String
    private lateinit var currentMarketplace: String
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        binding.productsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Restore cached subscription state immediately so the UI is useful
        // even before the Appstore responds (offline / outside-sandbox fallback).
        restoreCachedSubscriptionState()

        PurchasingService.registerListener(this, purchasingListener)
        Log.v(TAG, "Registering PurchasingListener")

        binding.subscriptionButton.setOnClickListener { PurchasingService.purchase(parentSKU) }

    }

    override fun onResume() {
        super.onResume()

        //getUserData() will query the Appstore for the Users information
        PurchasingService.getUserData()

        //getPurchaseUpdates() will query the Appstore for any previous purchase
        PurchasingService.getPurchaseUpdates(true)

        //getProductData will validate the SKUs with Amazon Appstore
        val productSkus = hashSetOf("techquarterly","techmonthly")

        PurchasingService.getProductData(productSkus)
        Log.v(TAG, "Validating SKUs with Amazon")
    }

    // ── Local cache helpers ──────────────────────────────────────────────────

    /** Persist the set of available product SKUs to SharedPreferences. */
    private fun cacheProductSkus(skus: Set<String>) {
        prefs.edit().putStringSet(PREFS_KEY_PRODUCTS, skus).apply()
        Log.v(TAG, "Cached ${skus.size} product SKU(s)")
    }

    /** Persist whether the user has an active subscription. */
    private fun cacheSubscriptionActive(active: Boolean) {
        prefs.edit().putBoolean(PREFS_KEY_SUBSCRIPTION, active).apply()
        Log.v(TAG, "Cached subscription active = $active")
    }

    /**
     * Update the subscription status UI. Call once after determining whether
     * the user has an active subscription (from live Appstore or from cache).
     */
    private fun updateSubscriptionUI(isActive: Boolean) {
        if (isActive) {
            binding.textView.apply {
                text = "SUBSCRIBED"
                setTextColor(Color.RED)
            }
        }
    }

    /**
     * Restore the previously-cached subscription state so the UI reflects a
     * known good state while waiting for the Appstore (or when offline /
     * outside the Amazon sandbox agent).
     */
    private fun restoreCachedSubscriptionState() {
        val active = prefs.getBoolean(PREFS_KEY_SUBSCRIPTION, false)
        if (active) {
            updateSubscriptionUI(true)
            Log.v(TAG, "Restored subscription state from cache")
        }
    }

    // ── PurchasingListener ───────────────────────────────────────────────────

    private var purchasingListener: PurchasingListener = object : PurchasingListener {
        override fun onUserDataResponse(response: UserDataResponse) {
            Log.v(TAG, "onUserDataResponse")
            when (response.requestStatus) {
                UserDataResponse.RequestStatus.SUCCESSFUL -> {
                    currentUserId = response.userData.userId
                    currentMarketplace = response.userData.marketplace
                    Log.v(TAG, response.userData.toString())
                }
                UserDataResponse.RequestStatus.NOT_SUPPORTED -> {
                    // Running outside the Amazon Appstore / sandbox agent.
                    Log.w(TAG, "Amazon Appstore not available on this device; IAP is unavailable.")
                    Toast.makeText(
                        this@MainActivity,
                        "Amazon Appstore is not available on this device.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                UserDataResponse.RequestStatus.FAILED, null -> {
                    Log.e(TAG, "getUserData failed; falling back to cached state")
                    restoreCachedSubscriptionState()
                }
            }
        }

        override fun onProductDataResponse(productDataResponse: ProductDataResponse) {
            Log.v(TAG, "onProductDataResponse")
            when (productDataResponse.requestStatus) {
                ProductDataResponse.RequestStatus.SUCCESSFUL -> {
                    Log.v(TAG, "ProductDataResponse.RequestStatus SUCCESSFUL")
                    val products = productDataResponse.productData
                    for (key in products.keys) {
                        val product = products[key]
                        Log.v(
                            TAG,
                            "Product: ${product!!.title} \n Type: ${product.productType}\n SKU: ${product.sku}\n Price: ${product.price}\n Description: ${product.description}\n"
                        )
                    }
                    binding.productsRecyclerView.adapter = ProductAdapter(products.values.toList())
                    // Cache the available SKUs for offline / outside-sandbox use.
                    cacheProductSkus(products.keys)
                    for (s in productDataResponse.unavailableSkus) {
                        Log.v(TAG, "Unavailable SKU:$s")
                    }
                }
                ProductDataResponse.RequestStatus.FAILED -> {
                    Log.v(TAG, "ProductDataResponse.RequestStatus FAILED; using cached SKUs")
                    val cachedSkus = prefs.getStringSet(PREFS_KEY_PRODUCTS, emptySet()) ?: emptySet()
                    Log.v(TAG, "Cached SKUs: $cachedSkus")
                }
                else -> {
                    Log.e(TAG, "Not supported")
                }
            }
        }

        override fun onPurchaseResponse(purchaseResponse: PurchaseResponse) {
            Log.v(TAG, "onPurchaseResponse")
            when (purchaseResponse.requestStatus) {
                PurchaseResponse.RequestStatus.SUCCESSFUL -> {
                    Log.v(TAG, "PurchaseResponse.RequestStatus SUCCESSFUL")
                    Log.v(TAG, purchaseResponse.receipt.toString())
                    PurchasingService.notifyFulfillment(
                        purchaseResponse.receipt.receiptId,
                        FulfillmentResult.FULFILLED
                    )
                }
                PurchaseResponse.RequestStatus.FAILED -> {
                    Log.v(TAG, "PurchaseResponse.RequestStatus FAILED")
                }
                else -> {
                    Log.e(TAG, "Not supported")
                }
            }
        }

        override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
            Log.v(TAG, "onPurchaseUpdatesResponse")
            when (response.requestStatus) {
                PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL -> {
                    Log.v(TAG, "PurchaseUpdatesResponse.RequestStatus SUCCESSFUL")
                    var hasActiveSubscription = false
                    for (receipt in response.receipts) {
                        Log.v(TAG, receipt.toString())
                        if (!receipt.isCanceled) {
                            hasActiveSubscription = true
                        }
                    }
                    // Update the UI once after inspecting all receipts.
                    updateSubscriptionUI(hasActiveSubscription)
                    // Persist the subscription state so it survives outside-sandbox runs.
                    cacheSubscriptionActive(hasActiveSubscription)
                    if (response.hasMore()) {
                        PurchasingService.getPurchaseUpdates(true)
                    }
                }
                PurchaseUpdatesResponse.RequestStatus.FAILED -> {
                    Log.v(TAG, "PurchaseUpdatesResponse.RequestStatus FAILED; using cached state")
                    restoreCachedSubscriptionState()
                }
                else -> {
                    Log.e(TAG, "Not supported")
                }
            }
        }
    }
}
