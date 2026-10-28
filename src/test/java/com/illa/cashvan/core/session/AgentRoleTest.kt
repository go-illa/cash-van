package com.illa.cashvan.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgentRoleTest {

    @Test
    fun `missing or empty roles mean fulfillment`() {
        assertEquals(setOf(AgentRole.FULFILLMENT), AgentRole.parse(null))
        assertEquals(setOf(AgentRole.FULFILLMENT), AgentRole.parse(emptyList()))
    }

    @Test
    fun `unknown roles are ignored and known ones are case insensitive`() {
        assertEquals(setOf(AgentRole.SALES), AgentRole.parse(listOf(" Sales ", "supervisor")))
        assertEquals(setOf(AgentRole.FULFILLMENT), AgentRole.parse(listOf("supervisor")))
    }

    @Test
    fun `single role opens its module`() {
        assertEquals(AgentRole.SALES, resolveStartModule(setOf(AgentRole.SALES), lastActive = AgentRole.FULFILLMENT))
        assertEquals(AgentRole.FULFILLMENT, resolveStartModule(setOf(AgentRole.FULFILLMENT), lastActive = null))
    }

    @Test
    fun `both roles reopen the last module or ask`() {
        val both = setOf(AgentRole.SALES, AgentRole.FULFILLMENT)
        assertEquals(AgentRole.SALES, resolveStartModule(both, lastActive = AgentRole.SALES))
        assertNull(resolveStartModule(both, lastActive = null))
    }

    @Test
    fun `no roles opens fulfillment`() {
        assertEquals(AgentRole.FULFILLMENT, resolveStartModule(emptySet(), lastActive = AgentRole.SALES))
    }
}
