package com.coder.jetbrains.matcher

class PortMatcher(private val rule: String) {
    private sealed class MatchRule {
        data class SinglePort(val port: Int) : MatchRule()
        data class PortRange(val start: Int, val end: Int) : MatchRule()
        data class RegexPort(val pattern: Regex) : MatchRule()
    }

    private val parsedRule: MatchRule

    init {
        parsedRule = parseRule(rule)
    }

    fun matches(port: Int): Boolean {
        return when (parsedRule) {
            is MatchRule.SinglePort -> port == parsedRule.port
            is MatchRule.PortRange -> port in parsedRule.start..parsedRule.end
            is MatchRule.RegexPort -> parsedRule.pattern.matches(port.toString())
        }
    }

    private fun parseRule(rule: String): MatchRule {
        // Remove host part if present (e.g., "localhost:3000" -> "3000")
        val portPart = rule.substringAfter(':').takeIf { ':' in rule } ?: rule

        return when {
            // Try parsing as single port
            portPart.all { it.isDigit() } -> {
                MatchRule.SinglePort(portPart.toInt())
            }
            // Try parsing as port range (e.g., "40000-55000")
            portPart.matches("^\\d+-\\d+$".toRegex()) -> {
                val (start, end) = portPart.split('-')
                    .map { it.trim().toInt() }
                require(start <= end) { "Invalid port range: start must be less than or equal to end" }
                MatchRule.PortRange(start, end)
            }
            // If not a single port or range, treat as regex
            else -> {
                try {
                    MatchRule.RegexPort(portPart.toRegex())
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid port rule format: $rule")
                }
            }
        }
    }
}
