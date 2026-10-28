package com.illa.cashvan.feature.auth

import com.illa.cashvan.feature.auth.data.model.SalesAgent
import com.illa.cashvan.feature.auth.domain.model.LoginIdentifier
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginIdentifierTest {

    @Test
    fun `blank input is rejected`() {
        assertNull(LoginIdentifier.parse("   "))
    }

    @Test
    fun `phone numbers become 20 plus the last 10 digits`() {
        val expected = LoginIdentifier.Phone("201012345678")
        assertEquals(expected, LoginIdentifier.parse("01012345678"))
        assertEquals(expected, LoginIdentifier.parse("1012345678"))
        assertEquals(expected, LoginIdentifier.parse("+20 101 234 5678"))
        assertEquals(expected, LoginIdentifier.parse("201012345678"))
        assertEquals(expected, LoginIdentifier.parse("010-1234-5678"))
    }

    @Test
    fun `arabic digits are read as a phone number`() {
        assertEquals(LoginIdentifier.Phone("201012345678"), LoginIdentifier.parse("٠١٠١٢٣٤٥٦٧٨"))
    }

    @Test
    fun `codes are trimmed and upper cased`() {
        assertEquals(LoginIdentifier.AgentCode("ILLA_001"), LoginIdentifier.parse(" illa_001 "))
    }

    @Test
    fun `short numbers are left for the backend to decide`() {
        assertEquals(LoginIdentifier.AgentCode("12341"), LoginIdentifier.parse("12341"))
    }

    @Test
    fun `phones are sent as phone_number and codes as login`() {
        val json = Json
        assertEquals(
            """{"password":"secret","phone_number":"201012345678"}""",
            json.encodeToString(SalesAgent.serializer(), SalesAgent.from(LoginIdentifier.Phone("201012345678"), "secret"))
        )
        assertEquals(
            """{"password":"secret","login":"ILLA_001"}""",
            json.encodeToString(SalesAgent.serializer(), SalesAgent.from(LoginIdentifier.AgentCode("ILLA_001"), "secret"))
        )
    }
}
