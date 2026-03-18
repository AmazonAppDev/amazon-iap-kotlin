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
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserDataResponse

/**
 * BridgeContext represents the external runtime context (e.g. an Aster/Python layer)
 * that receives lifecycle reports from the coherence bridge.
 *
 * Implement this interface to integrate the coherence bridge with any host runtime.
 */
interface BridgeContext {
    /** Called during the Prepare phase to supply the current user ID. */
    fun getUserId(): String

    /** Called during the Report phase to receive the result of an IAP operation. */
    fun report(result: Any)

    /** Called during the Cleanup phase to release resources held by the host runtime. */
    fun cleanup()
}

/**
 * CoherenceBridge: Ties an external runtime context (Aster/Python) to Amazon IAP (Kotlin/JVM).
 *
 * Every operation follows the same five-phase rhythm:
 *   Init → Prepare → Execute → Report → Cleanup
 *
 * This keeps the bridge substrate-agnostic: the host runtime only needs to implement
 * [BridgeContext]; it does not need to know about Amazon IAP internals.
 */
class CoherenceBridge(private val context: Context) {

    private var bridgeContext: BridgeContext? = null
    private var purchasingListener: PurchasingListener? = null

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 1: Init — establish context
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Initialize the bridge with the provided [BridgeContext].
     * Must be called before any other phase.
     */
    fun initPhase(ctx: BridgeContext): CoherenceBridge {
        bridgeContext = ctx
        return this
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 2: Prepare — ready resources
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Register the [PurchasingListener] with the Amazon Appstore and fetch user data.
     * Returns the [RequestId] produced by [PurchasingService.getUserData].
     */
    fun preparePhase(listener: PurchasingListener): RequestId {
        purchasingListener = listener
        PurchasingService.registerListener(context, listener)
        return PurchasingService.getUserData()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 3: Execute — do work
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Query product data for the given [skus].
     * Returns the [RequestId] produced by [PurchasingService.getProductData].
     */
    fun executeProductDataQuery(skus: Set<String>): RequestId {
        return PurchasingService.getProductData(skus)
    }

    /**
     * Initiate a purchase for the given [sku].
     * Returns the [RequestId] produced by [PurchasingService.purchase].
     */
    fun executePurchasePhase(sku: String): RequestId {
        return PurchasingService.purchase(sku)
    }

    /**
     * Notify the Appstore that a purchase has been fulfilled.
     */
    fun executeFulfillmentPhase(receiptId: String, result: FulfillmentResult) {
        PurchasingService.notifyFulfillment(receiptId, result)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 4: Report — communicate state back to the host runtime
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Forward [result] to the registered [BridgeContext].
     */
    fun reportPhase(result: Any) {
        bridgeContext?.report(result)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 5: Cleanup — manage entropy
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Release all resources and notify the [BridgeContext] to perform its own cleanup.
     */
    fun cleanupPhase() {
        purchasingListener = null
        bridgeContext?.cleanup()
        bridgeContext = null
    }
}
