package de.robnice.elly.ui.setup

object IpAddressValidator {

    fun isValidIpv4(input: String): Boolean {
        val parts = input.trim().split('.')
        return parts.size == 4 && parts.all { part ->
            part.isNotEmpty() && part.length <= 3 && part.all(Char::isDigit) && part.toInt() in 0..255
        }
    }
}
