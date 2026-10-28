package com.illa.cashvan.sales.feature.visit.domain.repository

import com.illa.cashvan.core.location.LocationData
import com.illa.cashvan.sales.feature.visit.data.model.NoOrderReason
import com.illa.cashvan.sales.feature.visit.data.model.VisitHistoryItem

interface PresellVisitRepository {
    /**
     * @return the new visit id.
     * @throws DuplicateVisitException when the database rejects a second visit within 5 minutes.
     */
    suspend fun createVisit(merchantId: String, location: LocationData, isAdHoc: Boolean = false): String

    suspend fun completeVisitWithoutOrder(visitId: String, reasonId: String)

    suspend fun getNoOrderReasons(): List<NoOrderReason>

    /** Up to the 20 latest visits by any agent, with their orders and items. */
    suspend fun getVisitHistory(merchantId: String): List<VisitHistoryItem>
}

class DuplicateVisitException(cause: Throwable) : Exception(cause.message, cause)
