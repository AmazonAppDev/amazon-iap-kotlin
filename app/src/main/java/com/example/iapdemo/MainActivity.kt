package com.example.iapdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.graphics.Color
import com.amazon.device.iap.PurchasingService
import android.util.Log
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.UserDataResponse
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.example.iapdemo.databinding.ActivityMainBinding
import java.util.*


const val parentSKU = "com.amazon.sample.iap.subscription.mymagazine"

class MainActivity : AppCompatActivity() {

    private lateinit var currentUserId: String
    private lateinit var currentMarketplace: String
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        PurchasingService.registerListener(this, purchasingListener)

        binding.subscriptionButton.setOnClickListener { PurchasingService.purchase(parentSKU) }

    }

    override fun onResume() {
        super.onResume()

        //getUserData() will query the Appstore for the Users information
        PurchasingService.getUserData()

        //getPurchaseUpdates() will query the Appstore for any previous purchase
        PurchasingService.getPurchaseUpdates(true)

        //getProductData will validate the SKUs with Amazon Appstore
        val productSkus = hashSetOf(parentSKU)

        PurchasingService.getProductData(productSkus)
        Log.v("Validating SKUs", "Validating SKUs with Amazon")
    }

    private var purchasingListener: PurchasingListener = object : PurchasingListener {
        override fun onUserDataResponse(response: UserDataResponse) {
            when (response.requestStatus) {
                UserDataResponse.RequestStatus.SUCCESSFUL -> {
                    currentUserId = response.userData.userId
                    currentMarketplace = response.userData.marketplace
                }
                UserDataResponse.RequestStatus.FAILED, UserDataResponse.RequestStatus.NOT_SUPPORTED, null -> {
                    Log.e("Request", "Request error")
                }
            }
        }

        override fun onProductDataResponse(productDataResponse: ProductDataResponse) {
            when (productDataResponse.requestStatus) {
                ProductDataResponse.RequestStatus.SUCCESSFUL -> {
                    val products = productDataResponse.productData
                    for (key in products.keys) {
                        val product = products[key]
                        Log.v(
                            "Product:",
                            "Product: ${product!!.title} \n Type: ${product.productType}\n SKU: ${product.sku}\n Price: ${product.price}\n Description: ${product.description}\n"
                        )
                    }
                    for (s in productDataResponse.unavailableSkus) {
                        Log.v("Unavailable SKU:$s", "Unavailable SKU:$s")
                    }
                }
                ProductDataResponse.RequestStatus.FAILED -> Log.v("FAILED", "FAILED")

                else -> {
                    Log.e("Product", "Not supported")
                }
            }
        }

        override fun onPurchaseResponse(purchaseResponse: PurchaseResponse) {
            when (purchaseResponse.requestStatus) {
                PurchaseResponse.RequestStatus.SUCCESSFUL -> PurchasingService.notifyFulfillment(
                    purchaseResponse.receipt.receiptId,
                    FulfillmentResult.FULFILLED
                )
                PurchaseResponse.RequestStatus.FAILED -> {
                }
                else -> {
                    Log.e("Product", "Not supported")
                }
            }
        }

        override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
            when (response.requestStatus) {
                PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL -> {
                    for (receipt in response.receipts) {
                        if (!receipt.isCanceled) {
                            binding.textView.apply {
                                text = "SUBSCRIBED"
                                setTextColor(Color.RED)
                            }
                        }
                    }
                    if (response.hasMore()) {
                        PurchasingService.getPurchaseUpdates(true)
                    }
                }
                PurchaseUpdatesResponse.RequestStatus.FAILED -> Log.d("FAILED", "FAILED")
                else -> {
                    Log.e("Product", "Not supported")
                }
            }
        }
    }
}