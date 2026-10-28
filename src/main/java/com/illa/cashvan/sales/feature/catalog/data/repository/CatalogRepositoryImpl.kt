package com.illa.cashvan.sales.feature.catalog.data.repository

import com.illa.cashvan.core.supabase.SupabaseClient
import com.illa.cashvan.core.supabase.selectAll
import com.illa.cashvan.core.supabase.selectInChunks
import com.illa.cashvan.sales.data.CurrentAgentProvider
import com.illa.cashvan.sales.feature.catalog.data.model.MerchantPricing
import com.illa.cashvan.sales.feature.catalog.data.model.MerchantPricingRow
import com.illa.cashvan.sales.feature.catalog.data.model.PricedProductRow
import com.illa.cashvan.sales.feature.catalog.data.model.PrincipalRow
import com.illa.cashvan.sales.feature.catalog.data.model.ProductIdRow
import com.illa.cashvan.sales.feature.catalog.data.model.RegionIdRow
import com.illa.cashvan.sales.feature.catalog.data.model.Vendor
import com.illa.cashvan.sales.feature.catalog.data.model.VendorCatalog
import com.illa.cashvan.sales.feature.catalog.domain.repository.CatalogRepository
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.text.Collator
import java.util.Locale

class CatalogRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val currentAgentProvider: CurrentAgentProvider
) : CatalogRepository {

    override suspend fun getMerchantPricing(merchantId: String): MerchantPricing? = runCatching {
        supabaseClient.selectFirstOrNull<MerchantPricingRow>("merchants") {
            select("price_tier,region_id")
            eq("id", merchantId)
        }
    }.getOrNull()?.let { MerchantPricing(it.price_tier ?: DEFAULT_PRICE_TIER, it.region_id) }

    override suspend fun getVendors(pricing: MerchantPricing): VendorCatalog {
        val products = pricedProducts(pricing, principalId = null, columns = "principal_id,sku,product_prices!inner(price_tier,final_price,base_price)")
            .filter { it.principal_id != null }
        val counts = products.groupingBy { it.principal_id!! }.eachCount()
        if (counts.isEmpty()) return VendorCatalog(pricing, emptyList(), emptyList())

        val principals = supabaseClient.select<PrincipalRow>("principals") {
            select("id,name,vendor_image")
            isIn("id", counts.keys)
        }
        val arabic = Collator.getInstance(Locale("ar"))
        val vendors = principals
            .map { Vendor(it.id, it.name, it.vendor_image, counts[it.id] ?: 0) }
            .sortedWith(compareBy(arabic) { it.name })
        return VendorCatalog(pricing, vendors, products.map { it.principal_id!! to it.sku })
    }

    override suspend fun getProducts(pricing: MerchantPricing, principalId: String?): List<PricedProductRow> =
        pricedProducts(pricing, principalId, PRODUCT_COLUMNS).sortedBy { it.description.orEmpty() }

    override suspend fun getPriceRows(productIds: List<String>, pricing: MerchantPricing): List<JsonObject> {
        val regionId = pricing.regionId ?: return emptyList()
        return supabaseClient.selectInChunks("product_prices", productIds) { chunk ->
            select("*")
            isIn("product_id", chunk)
            eq("price_tier", pricing.priceTier)
            eq("governorate_id", regionId)
            notNull("final_price")
            notNull("base_price")
        }
    }

    override suspend fun getPresellInventory(merchantId: String, skus: List<String>): JsonObject =
        supabaseClient.rpc(
            "get_presell_inventory",
            buildJsonObject {
                put("p_merchant_id", merchantId)
                putJsonArray("p_skus") {
                    skus.map(String::trim).filter(String::isNotEmpty).distinct().sorted().forEach { add(it) }
                }
            }
        )

    /**
     * Products in the agent's regions with a non-null price for the merchant's tier and region
     * (`product_prices.governorate_id` holds the merchant's region id).
     */
    private suspend fun pricedProducts(
        pricing: MerchantPricing,
        principalId: String?,
        columns: String
    ): List<PricedProductRow> {
        val regionId = pricing.regionId ?: return emptyList()
        val productIds = accessibleProductIds()
        if (productIds.isEmpty()) return emptyList()

        return supabaseClient.selectInChunks<PricedProductRow>("products", productIds) { chunk ->
            select(columns)
            isIn("id", chunk)
            isNull("discarded_at")
            eq("product_prices.price_tier", pricing.priceTier)
            eq("product_prices.governorate_id", regionId)
            notNull("product_prices.final_price")
            notNull("product_prices.base_price")
            if (principalId != null) eq("principal_id", principalId) else notNull("principal_id")
        }
    }

    private suspend fun accessibleProductIds(): List<String> {
        val agent = currentAgentProvider.require()
        val regionIds = supabaseClient.select<RegionIdRow>("agent_regions") {
            select("region_id")
            eq("agent_id", agent.agentId)
        }.map { it.region_id }
        if (regionIds.isEmpty()) return emptyList()

        return supabaseClient.selectAll<ProductIdRow>("product_regions") {
            select("product_id")
            isIn("region_id", regionIds)
        }.map { it.product_id }.distinct()
    }

    private companion object {
        const val DEFAULT_PRICE_TIER = "retail"
        const val PRODUCT_COLUMNS = "id,sku,description,principal_id,image_url," +
            "product_prices!inner(id,product_id,price_tier,price,base_price,final_price,discounted," +
            "discount_details,vat_percentage,vat_application,start_date,end_date,active,discount_type," +
            "cash_discount,governorate_id,created_at,updated_at)"
    }
}
