package utils

import java.util.Base64

object AuthUtils {
    private const val MID = "4327306933PP"
    private const val MKEY = "fhzdkytO}8nGw23"

    fun getAuthHeader(): String {
        val credentials = "$MID:$MKEY"
        val encoded = Base64.getEncoder().encodeToString(credentials.toByteArray())
        return "Basic $encoded"
    }
}
