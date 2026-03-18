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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CoherenceBridge].
 *
 * [IapService] is mocked so that every test runs on the JVM without a real
 * Amazon Appstore connection.  Covers:
 *   - Full five-phase lifecycle (Init → Prepare → Execute → Report → Cleanup)
 *   - User-data retrieval flow
 *   - Purchase flow (purchase → fulfillment)
 *   - Purchase-updates query
 *   - Product-data query
 *   - Error / edge paths (missing init, failed statuses)
 */
class CoherenceBridgeTest {

    // ── Mocks ────────────────────────────────────────────────────────────────

    private val mockContext: Context = mockk(relaxed = true)
    private val mockIapService: IapService = mockk(relaxed = true)
    private val mockBridgeContext: BridgeContext = mockk(relaxed = true)
    private val mockListener: PurchasingListener = mockk(relaxed = true)

    private lateinit var bridge: CoherenceBridge

    @Before
    fun setUp() {
        bridge = CoherenceBridge(mockContext, mockIapService)
    }

    // ── Phase 1: Init ────────────────────────────────────────────────────────

    @Test
    fun `initPhase stores bridge context and returns the bridge for chaining`() {
        val result = bridge.initPhase(mockBridgeContext)
        assertSame(bridge, result)
    }

    // ── Phase 2: Prepare — user-data retrieval ───────────────────────────────

    @Test
    fun `preparePhase registers listener then calls getUserData`() {
        val expectedRequestId: RequestId = mockk()
        every { mockIapService.getUserData() } returns expectedRequestId

        bridge.initPhase(mockBridgeContext)
        val actualRequestId = bridge.preparePhase(mockListener)

        // Listener must be registered before user data is requested
        verifyOrder {
            mockIapService.registerListener(mockContext, mockListener)
            mockIapService.getUserData()
        }
        assertEquals(expectedRequestId, actualRequestId)
    }

    @Test
    fun `preparePhase returns the RequestId from getUserData`() {
        val requestId: RequestId = mockk()
        every { mockIapService.getUserData() } returns requestId

        val result = bridge.preparePhase(mockListener)

        assertEquals(requestId, result)
    }

    @Test
    fun `preparePhase passes the correct context to registerListener`() {
        bridge.preparePhase(mockListener)
        verify { mockIapService.registerListener(mockContext, mockListener) }
    }

    // ── Phase 3a: Execute — product-data query ───────────────────────────────

    @Test
    fun `executeProductDataQuery calls getProductData with the supplied SKUs`() {
        val skus = setOf("techmonthly", "techquarterly")
        val requestId: RequestId = mockk()
        every { mockIapService.getProductData(skus) } returns requestId

        val result = bridge.executeProductDataQuery(skus)

        verify(exactly = 1) { mockIapService.getProductData(skus) }
        assertEquals(requestId, result)
    }

    @Test
    fun `executeProductDataQuery works with a single SKU`() {
        val skus = setOf("techsubscription")
        val requestId: RequestId = mockk()
        every { mockIapService.getProductData(skus) } returns requestId

        val result = bridge.executeProductDataQuery(skus)

        verify { mockIapService.getProductData(skus) }
        assertEquals(requestId, result)
    }

    // ── Phase 3b: Execute — purchase flow ────────────────────────────────────

    @Test
    fun `executePurchasePhase calls purchase with the correct SKU`() {
        val sku = "techsubscription"
        val requestId: RequestId = mockk()
        every { mockIapService.purchase(sku) } returns requestId

        val result = bridge.executePurchasePhase(sku)

        verify(exactly = 1) { mockIapService.purchase(sku) }
        assertEquals(requestId, result)
    }

    @Test
    fun `executePurchasePhase returns the RequestId from purchase`() {
        val requestId: RequestId = mockk()
        every { mockIapService.purchase(any()) } returns requestId

        assertEquals(requestId, bridge.executePurchasePhase("any-sku"))
    }

    @Test
    fun `executeFulfillmentPhase calls notifyFulfillment with FULFILLED`() {
        val receiptId = "receipt-abc-123"

        bridge.executeFulfillmentPhase(receiptId, FulfillmentResult.FULFILLED)

        verify(exactly = 1) {
            mockIapService.notifyFulfillment(receiptId, FulfillmentResult.FULFILLED)
        }
    }

    @Test
    fun `executeFulfillmentPhase calls notifyFulfillment with UNAVAILABLE`() {
        val receiptId = "receipt-xyz-456"

        bridge.executeFulfillmentPhase(receiptId, FulfillmentResult.UNAVAILABLE)

        verify(exactly = 1) {
            mockIapService.notifyFulfillment(receiptId, FulfillmentResult.UNAVAILABLE)
        }
    }

    // ── Phase 3c: Purchase-updates query ─────────────────────────────────────

    @Test
    fun `getPurchaseUpdates can be called via IapService with reset true`() {
        val requestId: RequestId = mockk()
        every { mockIapService.getPurchaseUpdates(true) } returns requestId

        val result = mockIapService.getPurchaseUpdates(true)

        verify { mockIapService.getPurchaseUpdates(true) }
        assertEquals(requestId, result)
    }

    @Test
    fun `getPurchaseUpdates can be called via IapService with reset false`() {
        val requestId: RequestId = mockk()
        every { mockIapService.getPurchaseUpdates(false) } returns requestId

        val result = mockIapService.getPurchaseUpdates(false)

        verify { mockIapService.getPurchaseUpdates(false) }
        assertEquals(requestId, result)
    }

    // ── Phase 4: Report ──────────────────────────────────────────────────────

    @Test
    fun `reportPhase forwards result to bridgeContext`() {
        bridge.initPhase(mockBridgeContext)

        val payload = "purchase-result-payload"
        bridge.reportPhase(payload)

        verify(exactly = 1) { mockBridgeContext.report(payload) }
    }

    @Test
    fun `reportPhase accepts any object type`() {
        bridge.initPhase(mockBridgeContext)

        val payload = mapOf("status" to "SUCCESSFUL", "receiptId" to "r-001")
        bridge.reportPhase(payload)

        verify { mockBridgeContext.report(payload) }
    }

    @Test
    fun `reportPhase does nothing and does not throw when initPhase was not called`() {
        // Should be safe — no NPE or other exception
        bridge.reportPhase("orphan-result")
    }

    // ── Phase 5: Cleanup ─────────────────────────────────────────────────────

    @Test
    fun `cleanupPhase calls bridgeContext cleanup`() {
        bridge.initPhase(mockBridgeContext)
        bridge.cleanupPhase()

        verify(exactly = 1) { mockBridgeContext.cleanup() }
    }

    @Test
    fun `cleanupPhase is safe to call without a prior initPhase`() {
        // Must not throw
        bridge.cleanupPhase()
    }

    @Test
    fun `cleanupPhase clears bridge context so subsequent reportPhase is a no-op`() {
        bridge.initPhase(mockBridgeContext)
        bridge.cleanupPhase()

        // After cleanup no report should reach the context
        bridge.reportPhase("should-not-arrive")

        verify(exactly = 0) { mockBridgeContext.report(any()) }
    }

    // ── Full lifecycle integration ────────────────────────────────────────────

    @Test
    fun `full purchase lifecycle - init prepare purchase fulfillment report cleanup`() {
        val sku = "techmonthly"
        val receiptId = "receipt-full-cycle"
        val userRequestId: RequestId = mockk()
        val purchaseRequestId: RequestId = mockk()

        every { mockIapService.getUserData() } returns userRequestId
        every { mockIapService.purchase(sku) } returns purchaseRequestId
        every { mockBridgeContext.getUserId() } returns "user-001"

        // Phase 1 – Init
        bridge.initPhase(mockBridgeContext)

        // Phase 2 – Prepare (user-data retrieval)
        val prepareId = bridge.preparePhase(mockListener)
        assertEquals(userRequestId, prepareId)

        // Phase 3 – Purchase
        val purchaseId = bridge.executePurchasePhase(sku)
        assertEquals(purchaseRequestId, purchaseId)

        // Phase 3 – Fulfillment
        bridge.executeFulfillmentPhase(receiptId, FulfillmentResult.FULFILLED)

        // Phase 4 – Report
        bridge.reportPhase("FULFILLED:$receiptId")

        // Phase 5 – Cleanup
        bridge.cleanupPhase()

        // Verify all IAP service interactions occurred in the correct order
        verifyOrder {
            mockIapService.registerListener(mockContext, mockListener)
            mockIapService.getUserData()
            mockIapService.purchase(sku)
            mockIapService.notifyFulfillment(receiptId, FulfillmentResult.FULFILLED)
        }
        verify { mockBridgeContext.report("FULFILLED:$receiptId") }
        verify { mockBridgeContext.cleanup() }
    }

    @Test
    fun `full user-data retrieval flow - init prepare report cleanup`() {
        val userRequestId: RequestId = mockk()
        every { mockIapService.getUserData() } returns userRequestId
        every { mockBridgeContext.getUserId() } returns "user-queenfi"

        bridge.initPhase(mockBridgeContext)
        val requestId = bridge.preparePhase(mockListener)
        bridge.reportPhase("userData:user-queenfi")
        bridge.cleanupPhase()

        assertEquals(userRequestId, requestId)
        verify(exactly = 1) { mockIapService.getUserData() }
        verify { mockBridgeContext.report("userData:user-queenfi") }
        verify { mockBridgeContext.cleanup() }
    }

    // ── Edge cases ───────────────────────────────────────────────────────────

    @Test
    fun `multiple initPhase calls overwrite the bridge context`() {
        val secondContext: BridgeContext = mockk(relaxed = true)

        bridge.initPhase(mockBridgeContext)
        bridge.initPhase(secondContext)

        // Only the second context should receive the report
        bridge.reportPhase("after-reinit")

        verify(exactly = 0) { mockBridgeContext.report(any()) }
        verify(exactly = 1) { secondContext.report("after-reinit") }
    }

    @Test
    fun `cleanup then reinit allows bridge to be reused`() {
        bridge.initPhase(mockBridgeContext)
        bridge.cleanupPhase()

        val newContext: BridgeContext = mockk(relaxed = true)
        bridge.initPhase(newContext)
        bridge.reportPhase("reused")

        verify { newContext.report("reused") }
    }
}
