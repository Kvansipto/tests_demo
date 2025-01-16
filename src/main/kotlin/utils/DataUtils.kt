package utils

import java.math.BigDecimal
import kotlin.random.Random

object DataUtils {

    fun generateRandomName(): String {
        val chars = ('A'..'Z') + ('a'..'z')
        val length = Random.nextInt(4, 21)
        return (1..length)
            .map { chars.random() }
            .joinToString("")
            .replaceFirstChar { it.uppercase() }
    }

    fun generateRandomPhone(): String {
        return (99000000..99999999).random().toString()
    }

    fun generateRandomAmount(): String {
        val amount = Random.nextInt(10_00, 1000_00 + 1)
        return BigDecimal(amount)
            .divide(BigDecimal(100))
            .toString()
    }
}