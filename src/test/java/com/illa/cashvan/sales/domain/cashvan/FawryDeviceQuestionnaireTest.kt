package com.illa.cashvan.sales.domain.cashvan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported from the PWA's src/lib/fawryDeviceQuestionnaire.test.ts. The TS tests
// mock the Supabase update; here they assert the pure payload and error mapping
// the data layer uses for the same writes.
class FawryDeviceQuestionnaireTest {

    @Test
    fun `DIS-543 resets wants_fawry_device to null in the same write, not just fawry_device_number`() {
        assertEquals(
            mapOf("fawry_device_number" to "12345", "wants_fawry_device" to null),
            fawryDeviceNumberUpdate("12345"),
        )
    }

    @Test
    fun `throws DuplicateFawryDeviceNumberException on a unique-constraint violation`() {
        val error = fawryDeviceNumberSaveError(code = "23505", message = "duplicate")

        assertTrue(error is DuplicateFawryDeviceNumberException)
        assertEquals("This Fawry device number is already assigned to another merchant", error.message)
    }

    @Test
    fun `throws a plain exception on any other failure`() {
        val error = fawryDeviceNumberSaveError(code = "500", message = "boom")

        assertFalse(error is DuplicateFawryDeviceNumberException)
        assertEquals("boom", error.message)
    }

    @Test
    fun `saving the answer only writes wants_fawry_device, leaving fawry_device_number untouched`() {
        assertEquals(mapOf("wants_fawry_device" to false), fawryDeviceWantedUpdate(false))
    }

    // Kotlin-only: checkNeedsFawryQuestionnaire's decision.
    @Test
    fun `asks until the merchant has a device or has agreed to get one, and fails open`() {
        assertTrue(needsFawryQuestionnaire(MerchantFawryStatus(fawryDeviceNumber = null, wantsFawryDevice = null)))
        assertTrue(needsFawryQuestionnaire(MerchantFawryStatus(fawryDeviceNumber = null, wantsFawryDevice = false)))
        assertFalse(needsFawryQuestionnaire(MerchantFawryStatus(fawryDeviceNumber = null, wantsFawryDevice = true)))
        assertFalse(needsFawryQuestionnaire(MerchantFawryStatus(fawryDeviceNumber = "12345", wantsFawryDevice = false)))
        // An empty string is still a value on file (the TS checks `== null`).
        assertFalse(needsFawryQuestionnaire(MerchantFawryStatus(fawryDeviceNumber = "", wantsFawryDevice = null)))
        assertFalse(needsFawryQuestionnaire(null))
    }
}
