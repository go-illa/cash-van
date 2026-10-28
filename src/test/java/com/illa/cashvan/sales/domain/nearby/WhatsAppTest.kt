package com.illa.cashvan.sales.domain.nearby

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

// Ported from the PWA's src/lib/whatsapp.test.ts. Expected links are spelled
// out with JS encodeURIComponent's output instead of calling it.
class WhatsAppTest {

    private val multilineArabic = "طلب نقل تاجر\nالحالة: قيد المراجعة."
    private val encodedMultilineArabic =
        "%D8%B7%D9%84%D8%A8%20%D9%86%D9%82%D9%84%20%D8%AA%D8%A7%D8%AC%D8%B1%0A" +
            "%D8%A7%D9%84%D8%AD%D8%A7%D9%84%D8%A9%3A%20%D9%82%D9%8A%D8%AF%20" +
            "%D8%A7%D9%84%D9%85%D8%B1%D8%A7%D8%AC%D8%B9%D8%A9."

    @Test
    fun `builds a direct approval notification for unassigned merchants`() {
        val message = buildDirectApprovalMessage(
            DirectApprovalMessageParams(
                agentName = "أحمد علي",
                agentPhone = "01000000000",
                merchantName = "سوبر ماركت النور",
                merchantCode = "M-123",
                toRouteName = "مسار مدينة نصر",
            ),
        )

        assertFalse(message.contains("رسالة لخيرت"))
        assertTrue(message.contains("إشعار إضافة تاجر غير مُعيّن إلى مسار"))
        assertTrue(message.contains("نوع العملية: إضافة مباشرة - تمت الموافقة تلقائيًا"))
        assertTrue(message.contains("المندوب: أحمد علي"))
        assertTrue(message.contains("رقم المندوب: 01000000000"))
        assertTrue(message.contains("التاجر: سوبر ماركت النور"))
        assertTrue(message.contains("كود التاجر: M-123"))
        assertTrue(message.contains("من: غير مُعيّن"))
        assertTrue(message.contains("إلى: مسار مدينة نصر"))
        assertTrue(message.contains("الحالة: تم إضافة التاجر على النظام ولا يحتاج موافقة إضافية."))
    }

    @Test
    fun `builds a pending assignment request for merchants on another route`() {
        val message = buildReassignmentMessage(
            ReassignmentMessageParams(
                agentName = "أحمد علي",
                agentPhone = "01000000000",
                merchantName = "سوبر ماركت النور",
                merchantCode = "M-123",
                fromRouteName = "مسار مصر الجديدة",
                toRouteName = "مسار مدينة نصر",
            ),
        )

        assertFalse(message.contains("رسالة لخيرت"))
        assertTrue(message.contains("طلب نقل تاجر بين المسارات"))
        assertTrue(message.contains("نوع العملية: طلب موافقة على نقل تاجر مُعيّن لمسار آخر"))
        assertTrue(message.contains("المسار الحالي: مسار مصر الجديدة"))
        assertTrue(message.contains("المسار المطلوب: مسار مدينة نصر"))
        assertTrue(message.contains("الحالة: قيد المراجعة."))
        assertTrue(message.contains("الإجراء المطلوب: مراجعة الطلب والموافقة أو الرفض من لوحة الإدارة."))
        assertTrue(message.contains("رابط لوحة الإدارة: https://admin.illatrucking.com/routes/reassignment-requests"))
    }

    @Test
    fun `omits merchant code cleanly when it is missing`() {
        val message = buildReassignmentMessage(
            ReassignmentMessageParams(
                agentName = "أحمد علي",
                agentPhone = "",
                merchantName = "سوبر ماركت النور",
                merchantCode = null,
                fromRouteName = "مسار مصر الجديدة",
                toRouteName = "مسار مدينة نصر",
            ),
        )

        assertFalse(message.contains("كود التاجر:"))
        assertFalse(message.contains("null"))
        assertFalse(message.contains("undefined"))
    }

    @Test
    fun `joins the direct approval lines exactly like the PWA (extra)`() {
        val message = buildDirectApprovalMessage(
            DirectApprovalMessageParams(agentName = "أحمد", agentPhone = "", merchantName = "النور", toRouteName = "مسار 1"),
        )

        assertEquals(
            listOf(
                "إشعار إضافة تاجر غير مُعيّن إلى مسار",
                "",
                "نوع العملية: إضافة مباشرة - تمت الموافقة تلقائيًا",
                "المندوب: أحمد",
                "التاجر: النور",
                "من: غير مُعيّن",
                "إلى: مسار 1",
                "",
                "الحالة: تم إضافة التاجر على النظام ولا يحتاج موافقة إضافية.",
            ),
            message.split("\n"),
        )
    }

    @Test
    fun `encodes multiline Arabic text in the wa_me link`() {
        assertEquals(
            "https://wa.me/201000000000?text=$encodedMultilineArabic",
            buildReassignmentWaLink(multilineArabic, "+20 100-000-0000"),
        )
    }

    @Test
    fun `builds a native WhatsApp mobile link with encoded Arabic text`() {
        assertEquals(
            "whatsapp://send?phone=201000000000&text=$encodedMultilineArabic",
            buildNativeWhatsAppLink(multilineArabic, "+20 100-000-0000"),
        )
    }

    @Test
    fun `normalizes Egyptian local mobile numbers before building links`() {
        assertEquals("https://wa.me/201000000000?text=hello", buildReassignmentWaLink("hello", "01000000000"))
        assertEquals("whatsapp://send?phone=201000000000&text=hello", buildNativeWhatsAppLink("hello", "01000000000"))
    }

    @Test
    fun `returns null for empty WhatsApp numbers`() {
        assertNull(buildReassignmentWaLink("hello", "---"))
        assertNull(buildNativeWhatsAppLink("hello", "---"))
        assertNull(buildWhatsAppLaunch("hello", null))
    }

    @Test
    fun `uses current-window native launch on mobile instead of a blank popup`() {
        assertEquals(
            WhatsAppLaunch("whatsapp://send?phone=201000000000&text=hello", WhatsAppLaunchTarget.CURRENT),
            buildWhatsAppLaunch("hello", "01000000000", useNativeLink = true),
        )
    }

    @Test
    fun `uses a reserved popup launch for desktop web links`() {
        assertEquals(
            WhatsAppLaunch("https://wa.me/201000000000?text=hello", WhatsAppLaunchTarget.POPUP),
            buildWhatsAppLaunch("hello", "01000000000", useNativeLink = false),
        )
    }

    @Test
    fun `defaults to the native link on Android (extra)`() {
        assertEquals(WhatsAppLaunchTarget.CURRENT, buildWhatsAppLaunch("hello", "01000000000")?.target)
    }

    @Test
    fun `keeps non-local numbers as their digits (extra)`() {
        assertEquals("https://wa.me/971501234567?text=hello", buildReassignmentWaLink("hello", "+971 50 123 4567"))
        // 12 digits starting with 01 is not a local mobile number.
        assertEquals("https://wa.me/010000000000?text=hello", buildReassignmentWaLink("hello", "010000000000"))
    }

    @Test
    fun `percent-encodes like JS encodeURIComponent (extra)`() {
        assertEquals("a%20b!~*'()-_.%3B%2F%3F%3A%40%26%3D%2B%24%2C%23", encodeURIComponent("a b!~*'()-_.;/?:@&=+$,#"))
        assertEquals("%F0%9F%98%80", encodeURIComponent("😀"))
        assertThrows(IllegalArgumentException::class.java) { encodeURIComponent("\uD83D") }
    }
}
