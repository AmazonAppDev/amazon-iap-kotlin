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
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.RequestId

/**
 * RealIapService: Production implementation of [IapService] that delegates every call
 * to the static [PurchasingService] API from the Amazon Appstore SDK.
 *
 * Swap this for a mock or fake in tests.
 */
class RealIapService : IapService {

    override fun registerListener(context: Context, listener: PurchasingListener) {
        PurchasingService.registerListener(context, listener)
    }

    override fun getUserData(): RequestId =
        PurchasingService.getUserData()

    override fun getProductData(skus: Set<String>): RequestId =
        PurchasingService.getProductData(skus)

    override fun purchase(sku: String): RequestId =
        PurchasingService.purchase(sku)

    override fun getPurchaseUpdates(reset: Boolean): RequestId =
        PurchasingService.getPurchaseUpdates(reset)

    override fun notifyFulfillment(receiptId: String, result: FulfillmentResult) {
        PurchasingService.notifyFulfillment(receiptId, result)
    }
}
