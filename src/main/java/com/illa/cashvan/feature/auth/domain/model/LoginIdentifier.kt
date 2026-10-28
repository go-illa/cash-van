package com.illa.cashvan.feature.auth.domain.model

/**
 * What the agent typed in the sign-in field: a phone number or an agent code.
 */
sealed interface LoginIdentifier {
    val value: String

    /** Normalised to `20` + the last 10 digits, the same rule as the sales PWA. */
    data class Phone(override val value: String) : LoginIdentifier

    /** Upper-cased, e.g. `ILLA_001`. */
    data class AgentCode(override val value: String) : LoginIdentifier

    companion object {
        private const val COUNTRY_CODE = "20"
        private const val PHONE_DIGITS = 10
        private const val PHONE_SEPARATORS = "+- "

        /** Returns null for blank input. Digits-only input shorter than a phone number is sent as a code. */
        fun parse(input: String): LoginIdentifier? {
            val text = input.trim().map(::toWesternDigit).joinToString("")
            if (text.isEmpty()) return null

            val digits = text.filter { it in '0'..'9' }
            val isNumeric = text.all { it in '0'..'9' || it in PHONE_SEPARATORS }
            if (isNumeric && digits.length >= PHONE_DIGITS) {
                return Phone(COUNTRY_CODE + digits.takeLast(PHONE_DIGITS))
            }
            return AgentCode(text.filterNot(Char::isWhitespace).uppercase())
        }

        /** Arabic keyboards type ٠-٩ (or ۰-۹); the backend expects 0-9. */
        private fun toWesternDigit(char: Char): Char = when (char) {
            in '٠'..'٩' -> '0' + (char - '٠')
            in '۰'..'۹' -> '0' + (char - '۰')
            else -> char
        }
    }
}
