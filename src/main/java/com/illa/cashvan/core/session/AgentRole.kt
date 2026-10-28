package com.illa.cashvan.core.session

/**
 * A role decides which module an agent can open. The active module is stored as one of these roles.
 */
enum class AgentRole(val apiValue: String) {
    SALES("sales"),
    FULFILLMENT("fulfillment");

    companion object {
        fun fromApiValue(value: String): AgentRole? =
            entries.firstOrNull { it.apiValue.equals(value.trim(), ignoreCase = true) }

        /**
         * Agents logged in before roles existed have none stored; they are fulfillment agents.
         */
        fun parse(values: List<String>?): Set<AgentRole> {
            val roles = values.orEmpty().mapNotNull(::fromApiValue).toSet()
            return roles.ifEmpty { setOf(FULFILLMENT) }
        }
    }
}

/**
 * Which module to open after sign-in or on app start. `null` means the agent has to pick one.
 */
fun resolveStartModule(roles: Set<AgentRole>, lastActive: AgentRole?): AgentRole? = when {
    roles.isEmpty() -> AgentRole.FULFILLMENT
    roles.size == 1 -> roles.first()
    lastActive != null && lastActive in roles -> lastActive
    else -> null
}
