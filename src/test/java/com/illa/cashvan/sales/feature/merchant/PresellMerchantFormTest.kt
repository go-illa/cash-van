package com.illa.cashvan.sales.feature.merchant

import com.illa.cashvan.sales.feature.merchant.presentation.viewmodel.PresellMerchantFormState
import com.illa.cashvan.sales.feature.merchant.presentation.viewmodel.isValidEgyptianMobile
import com.illa.cashvan.sales.feature.merchant.presentation.viewmodel.validateLocationStep
import com.illa.cashvan.sales.feature.merchant.presentation.viewmodel.validateMerchantStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresellMerchantFormTest {

    private val details = PresellMerchantFormState(
        merchantName = "Ahmed",
        signName = "Ahmed Market",
        merchantTypeId = "t1",
        phoneNumber = "010 1234-5678"
    )
    private val located = details.copy(
        governorate = "القاهرة",
        regionId = "r1",
        detailedAddress = "12 Street",
        visitDays = setOf("sunday")
    )

    @Test
    fun `egyptian mobiles are 01 plus 9 digits, ignoring spaces and dashes`() {
        assertTrue(isValidEgyptianMobile("01012345678"))
        assertTrue(isValidEgyptianMobile("010 1234-5678"))
        assertFalse(isValidEgyptianMobile("0201234567"))
        assertFalse(isValidEgyptianMobile("+201012345678"))
    }

    @Test
    fun `merchant step reports the first missing field`() {
        assertNull(validateMerchantStep(details))
        assertEquals("يرجى إدخال اسم اللافتة", validateMerchantStep(details.copy(signName = " ")))
        assertEquals("يرجى إدخال رقم هاتف مصري صحيح", validateMerchantStep(details.copy(phoneNumber = "123")))
        assertEquals("رقم الهاتف الثانوي غير صحيح", validateMerchantStep(details.copy(secondaryPhone = "999")))
    }

    @Test
    fun `location step needs a priced region, a route when there are several, and a visit day`() {
        assertNull(validateLocationStep(located, routeCount = 1, isLocating = false))
        assertEquals("جاري تحديد الموقع، يرجى الانتظار...", validateLocationStep(located, 1, isLocating = true))
        assertTrue(validateLocationStep(located.copy(regionId = ""), 1, false)!!.startsWith("لم يتم تحديد منطقة التسعير"))
        assertEquals("يرجى اختيار المسار", validateLocationStep(located, routeCount = 2, isLocating = false))
        assertEquals("يرجى اختيار يوم زيارة واحد على الأقل", validateLocationStep(located.copy(visitDays = emptySet()), 1, false))
    }
}
