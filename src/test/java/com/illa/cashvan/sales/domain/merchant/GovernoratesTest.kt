package com.illa.cashvan.sales.domain.merchant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GovernoratesTest {

    private val regions = listOf(
        GovernorateRegion("cairo", listOf("Cairo", "القاهرة", "Cairo")),
        GovernorateRegion("aswan", listOf("Aswan", "أسوان", "Aswan Governorate")),
        GovernorateRegion("red-sea", listOf("Red Sea", "البحر الأحمر"))
    )

    @Test
    fun `prefixes, diacritics and spacing are ignored`() {
        assertEquals("cairo", findGovernorateRegion(regions, "محافظة القاهرة")?.id)
        assertEquals("cairo", findGovernorateRegion(regions, "القَاهِرَة")?.id)
        assertEquals("red-sea", findGovernorateRegion(regions, "البحرالأحمر")?.id)
    }

    @Test
    fun `english names from geocoding match`() {
        assertEquals("cairo", findGovernorateRegion(regions, "Cairo Governorate")?.id)
        assertEquals("aswan", findGovernorateRegion(regions, "aswan")?.id)
    }

    @Test
    fun `unknown or blank names match nothing`() {
        assertNull(findGovernorateRegion(regions, "Giza"))
        assertNull(findGovernorateRegion(regions, "  "))
    }
}
