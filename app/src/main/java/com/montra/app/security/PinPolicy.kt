package com.montra.app.security

/** Rules for the app-lock PIN: exactly 6 digits, and nothing trivially guessable. */
object PinPolicy {
    const val LENGTH = 6

    private val COMMON = setOf(
        "000000", "111111", "123456", "654321", "112233", "123321", "121212", "123123",
        "159753", "147258", "258369", "789456", "456789", "696969", "202020", "101010"
    )

    /** Returns a message describing the problem, or null when the PIN is acceptable. */
    fun validate(pin: String): String? {
        if (pin.length != LENGTH || !pin.all { it in '0'..'9' }) return "PIN must be exactly $LENGTH digits"
        if (pin.toSet().size == 1) return "Don't use one digit repeated, like 111111"
        if (isSequence(pin)) return "Don't use a sequence like 123456 or 654321"
        if (isRepeatingPattern(pin) || pin in COMMON) return "That PIN is too easy to guess. Pick something less predictable"
        return null
    }

    private fun isSequence(pin: String): Boolean {
        val diffs = (1 until pin.length).map { pin[it] - pin[it - 1] }
        return diffs.all { it == 1 } || diffs.all { it == -1 }
    }

    /** 121212 (period 2) and 123123 (period 3). */
    private fun isRepeatingPattern(pin: String): Boolean =
        listOf(2, 3).any { period -> (period until pin.length).all { pin[it] == pin[it - period] } }
}

/** How long to block PIN entry after wrong attempts. The failure count survives app restarts. */
object LockoutPolicy {
    const val FREE_ATTEMPTS = 5
    private val STEPS_SECONDS = longArrayOf(30, 60, 300, 900, 3600)

    /** Lockout applied right after the [failures]-th wrong PIN in a row (0 = none yet). */
    fun lockoutMillis(failures: Int): Long {
        if (failures < FREE_ATTEMPTS) return 0
        val step = (failures - FREE_ATTEMPTS).coerceAtMost(STEPS_SECONDS.size - 1)
        return STEPS_SECONDS[step] * 1000
    }

    fun attemptsLeft(failures: Int): Int = (FREE_ATTEMPTS - failures).coerceAtLeast(0)
}
