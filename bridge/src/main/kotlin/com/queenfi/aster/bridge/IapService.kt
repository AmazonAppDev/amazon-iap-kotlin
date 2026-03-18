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

package com.queenfi.aster.bridge

import android.content.Context
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.RequestId

/**
 * IapService: Abstracts the static [com.amazon.device.iap.PurchasingService] calls behind
 * an interface so that purchase flows and user-data retrieval can be unit-tested with a mock.
 *
 * Production code uses [RealIapService]; tests substitute a mock or fake implementation.
 */
interface IapService {

    /**
     * Register a [PurchasingListener] with the Amazon Appstore SDK.
     * Maps to [com.amazon.device.iap.PurchasingService.registerListener].
     */
    fun registerListener(context: Context, listener: PurchasingListener)

    /**
     * Request current user data (userId + marketplace) from the Appstore.
     * Maps to [com.amazon.device.iap.PurchasingService.getUserData].
     */
    fun getUserData(): RequestId

    /**
     * Validate the given set of [skus] with the Amazon Appstore.
     * Maps to [com.amazon.device.iap.PurchasingService.getProductData].
     */
    fun getProductData(skus: Set<String>): RequestId

    /**
     * Initiate a purchase for the given [sku].
     * Maps to [com.amazon.device.iap.PurchasingService.purchase].
     */
    fun purchase(sku: String): RequestId

    /**
     * Query previous purchases.  Pass [reset] = true to re-process all records.
     * Maps to [com.amazon.device.iap.PurchasingService.getPurchaseUpdates].
     */
    fun getPurchaseUpdates(reset: Boolean): RequestId

    /**
     * Notify the Appstore that a purchase has been fulfilled or unavailable.
     * Maps to [com.amazon.device.iap.PurchasingService.notifyFulfillment].
     */
    fun notifyFulfillment(receiptId: String, result: FulfillmentResult)
}
