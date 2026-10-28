package com.illa.cashvan.sales.feature.checkout.data.repository

import com.illa.cashvan.core.supabase.SupabaseClient
import com.illa.cashvan.core.supabase.SupabaseException
import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.feature.checkout.data.model.FawryUploadRequest
import com.illa.cashvan.sales.feature.checkout.data.model.FawryUploadResponse
import com.illa.cashvan.sales.feature.checkout.data.model.PlaceOrderRequest
import com.illa.cashvan.sales.feature.checkout.data.model.PlaceOrderResponse
import com.illa.cashvan.sales.feature.checkout.data.model.PlacedOrder
import com.illa.cashvan.sales.feature.checkout.domain.repository.OrderSubmissionCodes
import com.illa.cashvan.sales.feature.checkout.domain.repository.OrderSubmissionException
import com.illa.cashvan.sales.feature.checkout.domain.repository.PresellCheckoutRepository
import com.illa.cashvan.sales.feature.checkout.domain.repository.normalizeOrderBackendError
import com.illa.cashvan.sales.domain.outofstock.OUT_OF_STOCK_NO_VALID_ITEMS_MESSAGE
import com.illa.cashvan.sales.domain.outofstock.SubmitOutOfStockRequestInput
import com.illa.cashvan.sales.domain.outofstock.SubmitOutOfStockRequestResult
import com.illa.cashvan.sales.domain.outofstock.collectValidOutOfStockItems
import com.illa.cashvan.sales.domain.outofstock.resolveOutOfStockRpcResponse
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.util.UUID

class PresellCheckoutRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val currentAgentProvider: CurrentAgentProvider
) : PresellCheckoutRepository {

    override suspend fun placeOrder(request: PlaceOrderRequest): PlacedOrder {
        val agent = currentAgentProvider.require()
        val location = request.location
        val params = buildJsonObject {
            put("p_agent_id", agent.agentId)
            put("p_merchant_id", request.merchantId)
            put("p_price_tier", request.priceTier)
            put("p_payment_type", request.paymentType)
            put("p_collection_date", request.collectionDate)
            put("p_is_ad_hoc", request.isAdHoc)
            put("p_latitude", location?.latitude)
            put("p_longitude", location?.longitude)
            put("p_location", location?.let { "${it.latitude},${it.longitude}" })
            put("p_fawry_invoice_url", request.fawryInvoiceUrl)
            put("p_total_value", request.totalValue)
            put("p_idempotency_key", request.idempotencyKey)
            putJsonArray("p_items") {
                request.lines.forEach { line ->
                    addJsonObject {
                        put("product_id", line.productId)
                        put("quantity", line.quantity)
                        put("unit_price", line.unitPrice)
                        put("price_snapshot", line.priceSnapshot?.withRegion(request.regionId) ?: JsonNull)
                    }
                }
            }
        }

        val response = try {
            supabaseClient.rpc<PlaceOrderResponse>("place_presell_order_with_inventory", params)
        } catch (e: SupabaseException) {
            throw normalizeOrderBackendError(e.code, e.message)
        }

        if (!response.success) {
            throw OrderSubmissionException(
                response.code ?: OrderSubmissionCodes.SUBMISSION_FAILED,
                response.shortages,
                response.message
            )
        }
        val orderId = response.order_id
        val visitId = response.visit_id
        if (orderId == null || visitId == null) throw OrderSubmissionException(OrderSubmissionCodes.INVALID_RESPONSE)

        return PlacedOrder(
            orderId = orderId,
            visitId = visitId,
            orderTotal = response.order_total ?: request.totalValue,
            inventoryReservationSource = response.inventory_reservation_source ?: "none"
        )
    }

    override suspend fun uploadFawryInvoice(dataUrl: String, contentType: String): String =
        supabaseClient.invokeFunction<FawryUploadRequest, FawryUploadResponse>(
            "upload-fawry-invoice",
            FawryUploadRequest(dataUrl, contentType, UUID.randomUUID().toString())
        ).url

    override suspend fun submitOutOfStockRequest(input: SubmitOutOfStockRequestInput): SubmitOutOfStockRequestResult {
        val items = collectValidOutOfStockItems(input.items)
        if (items.isEmpty()) return SubmitOutOfStockRequestResult.Failed(OUT_OF_STOCK_NO_VALID_ITEMS_MESSAGE)
        return try {
            val data = supabaseClient.rpc<JsonObject>(
                "submit_out_of_stock_request",
                buildJsonObject {
                    put("p_request_id", input.requestId)
                    put("p_agent_id", input.agentId)
                    put("p_merchant_id", input.merchantId)
                    putJsonArray("p_items") {
                        items.forEach { item ->
                            addJsonObject {
                                put("product_id", item.productId)
                                put("quantity", item.quantity)
                            }
                        }
                    }
                }
            )
            resolveOutOfStockRpcResponse(null, hasData = true, status = (data["status"] as? JsonPrimitive)?.contentOrNull)
        } catch (e: SupabaseException) {
            resolveOutOfStockRpcResponse(e.message, hasData = false, status = null)
        } catch (e: Exception) {
            resolveOutOfStockRpcResponse(e.message ?: "network error", hasData = false, status = null)
        }
    }

    /** The snapshot records the region the order was priced in, which can differ from the row's own. */
    private fun JsonObject.withRegion(regionId: String?): JsonObject {
        val region = regionId?.let(::JsonPrimitive) ?: this["governorate_id"] ?: JsonNull
        return JsonObject(this + ("governorate_id" to region))
    }
}
