package com.illa.cashvan.feature.commission.domain.usecase

import com.illa.cashvan.feature.commission.data.repository.CommissionRepository
import com.illa.cashvan.feature.commission.domain.model.FulfillmentCommission
import com.illa.cashvan.feature.commission.domain.model.toFulfillmentCommission
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class GetFulfillmentCommissionUseCase(private val repository: CommissionRepository) {
    suspend operator fun invoke(agentName: String): List<FulfillmentCommission> {
        return repository.getFulfillmentCommission(agentName, currentCommissionMonth())
            .filter { (it.target_orders ?: 0) > 0 }
            .map { it.toFulfillmentCommission() }
    }

    // Commission months are stored as the first day of the month in Cairo time
    private fun currentCommissionMonth(): String {
        val sdf = SimpleDateFormat("yyyy-MM-01", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Africa/Cairo")
        return sdf.format(Date())
    }
}
