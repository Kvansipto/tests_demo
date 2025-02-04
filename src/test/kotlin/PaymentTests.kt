import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import framework.closeWebDriver
import framework.pages.ConfirmPaymentPage
import framework.webDriver
import helpers.httpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import utils.AuthUtils
import utils.DataUtils
import utils.wait
import java.util.stream.Stream

class PaymentTests {
    companion object {
        private const val BASE_URL = "https://test-app-ext.2ip.in"
        private const val CREATE_URL = "$BASE_URL/api/payment/create"
        private const val STATUS_URL_TEMPLATE = "$BASE_URL/api/payment/status/%s"
        private const val CONFIRM_URL = "$BASE_URL/operations/confirm"
        private const val FAKE_OPERATION_ID = "fake"
        private const val PAYMENT_STATUS_UPDATE_TIMEOUT_SECONDS = 15

        private const val OPERATION_SUCCESS_TEMPLATE = "Operation %s was accepted"
        private const val OPERATION_ALREADY_PROCESSED = "Operation already processed."
        private const val OPERATION_DOES_NOT_EXIST = "Operation does not exists."

        private val ILLEGAL_ERRORS = listOf(
            "endUser.fullName" to "IllegalParameter",
            "endUser.phone" to "IllegalParameter",
            "order.amount.currency" to "IllegalParameter",
            "order.amount.value" to "IllegalParameter"
        )


        @JvmStatic
        fun invalidPaymentDataProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    null, null, null, null,
                    listOf(
                        "endUser.fullName" to "IllegalParameter",
                        "endUser.phone" to "MissingRequiredParameter",
                        "order.amount.currency" to "IllegalParameter",
                        "order.amount.value" to "MissingRequiredParameter"
                    )
                ),
                Arguments.of(
                    "Joe",
                    "98999999",
                    "9.99",
                    "RUR",
                    ILLEGAL_ERRORS
                ),
                Arguments.of(
                    "A very long name limit",
                    "999999999",
                    "1000.01",
                    "EUR",
                    ILLEGAL_ERRORS
                )
            )
        }
    }

    private val objectMapper = ObjectMapper()

    @AfterEach
    fun tearDown() {
        closeWebDriver()
    }

    @Test
    fun `test payment done status`() {
        val (uri, parameters) = createPayment()
        val operationId = parameters["operationId"]!!

        checkPaymentStatusWithRetries(operationId, "created")

        confirmPayment(uri, parameters)

        validateSuccessMessage(operationId)

        checkPaymentStatusWithRetries(operationId, "done")
    }

    @Test
    fun `test payment rejected status`() {
        val (uri, parameters) = createPayment()
        val operationId = parameters["operationId"]!!

        checkPaymentStatusWithRetries(operationId, "created")

        confirmPayment(uri, parameters, true)

        validateSuccessMessage(operationId)

        checkPaymentStatusWithRetries(operationId, "rejected")
    }

    @Test
    fun `test payment already processed error`() {
        val (uri, parameters) = createPayment()
        val operationId = parameters["operationId"]!!

        checkPaymentStatusWithRetries(operationId, "created")

        confirmPayment(uri, parameters)
        checkPaymentStatusWithRetries(operationId, "done")

        confirmPayment(uri, parameters)

        validateFailureMessage(OPERATION_ALREADY_PROCESSED)

    }

    @Test
    fun `test payment does not exist error`() {
        confirmPayment(CONFIRM_URL, mapOf("operationId" to FAKE_OPERATION_ID))
        validateFailureMessage(OPERATION_DOES_NOT_EXIST)
    }

    @ParameterizedTest
    @MethodSource("invalidPaymentDataProvider")
    fun `test create payment with invalid or missing data`(
        fullName: String?,
        phone: String?,
        amount: String?,
        currency: String?,
        expectedErrors: List<Pair<String, String>>
    ) {
        val requestBody = buildCreatePaymentRequest(fullName, phone, amount, currency)
        val response = sendCreatePaymentRequest(requestBody)
        val errors = response["errors"]

        assertTrue(errors.isArray, "Expected 'errors' to be an array, but was $errors")

        for ((parameterName, code) in expectedErrors) {
            assertTrue(
                errors.any {
                    it["parameterName"].asText() == parameterName && it["code"].asText() == code
                },
                "Expected error with parameter '$parameterName' and code '$code'. Errors: $errors"
            )
        }
    }

    @Test
    fun `test create payment with invalid authorization token`() {
        val requestBody = buildCreatePaymentRequest()

        val response = sendCreatePaymentRequest(requestBody, authHeader = "Basic INVALID_TOKEN")
        val errors = response["errors"]

        assertTrue(errors.isArray, "Expected 'errors' to be an array, but was $errors")

        assertTrue(
            errors.any {
                it["parameterName"].asText() == "Authorization" && it["code"].asText() == "IllegalHeader"
            },
            "Expected error for 'Authorization' with code 'IllegalHeader'. Errors: $errors"
        )
    }

    private fun createPayment(): Pair<String, Map<String, String>> {
        val requestBody = buildCreatePaymentRequest()
        val responseBody = sendCreatePaymentRequest(requestBody)
        return verifyCreatePaymentResponse(responseBody)
    }

    private fun sendCreatePaymentRequest(
        requestBody: String,
        authHeader: String = AuthUtils.getAuthHeader()
    ): JsonNode {
        val createRequest = HttpPost(CREATE_URL)

        createRequest.addHeader("Authorization", authHeader)
        createRequest.addHeader("Content-Type", "application/json")

        createRequest.entity = StringEntity(requestBody)

        val createResponse = httpClient.execute(createRequest)
        assertEquals(200, createResponse.statusLine.statusCode)
        val responseBody = EntityUtils.toString(createResponse.entity)
        return objectMapper.readTree(responseBody)
    }

    private fun buildCreatePaymentRequest(
        fullName: String? = DataUtils.generateRandomName(),
        phone: String? = DataUtils.generateRandomPhone(),
        amount: String? = DataUtils.generateRandomAmount(),
        currency: String? = "USD"
    ): String {
        val endUser = mutableMapOf<String, String>()
        fullName?.let { endUser["fullName"] = it }
        phone?.let { endUser["phone"] = it }

        val amountMap = mutableMapOf<String, String>()
        currency?.let { amountMap["currency"] = it }
        amount?.let { amountMap["value"] = it }

        return ObjectMapper().writeValueAsString(
            mapOf(
                "endUser" to endUser,
                "order" to mapOf("amount" to amountMap)
            )
        )
    }

    private fun verifyCreatePaymentResponse(jsonNode: JsonNode): Pair<String, Map<String, String>> {
        val uri = jsonNode["result"]["location"]["uri"].asText()

        val parametersNode = jsonNode["result"]["location"]["parameters"]
        val parameters = mutableMapOf<String, String>()

        if (parametersNode != null && parametersNode.isArray) {
            parametersNode.forEach { paramNode ->
                val name = paramNode["name"].asText()
                val value = paramNode["value"].asText()
                parameters[name] = value
            }
        }
        if (parameters["operationId"].isNullOrBlank()) {
            throw IllegalArgumentException("Missing or empty required parameter: operationId in response")
        }

        assertEquals("success", jsonNode["status"].asText(), "Expected status to be 'success'")
        assertEquals(
            "created",
            jsonNode["result"]["operation"]["status"].asText(),
            "Expected operation status to be 'created'"
        )
        return Pair(uri, parameters)
    }

    private fun confirmPayment(uri: String, parameters: Map<String, String>, isRejected: Boolean = false) {
        val page = ConfirmPaymentPage(webDriver)
        page.openConfirmPage(uri, parameters)
        val paymentCode = if (isRejected) 1231 else 1234
        page.enterPaymentCode(paymentCode.toString())
        page.submitPayment()
    }

    private fun validateSuccessMessage(operationId: String) {
        val page = ConfirmPaymentPage(webDriver)
        val successMessage = page.getSuccessMessage()
        assertTrue(
            successMessage.contains(OPERATION_SUCCESS_TEMPLATE.format(operationId)),
            "Operation ID in success message is incorrect"
        )
    }

    private fun validateFailureMessage(expectedMessage: String) {
        val page = ConfirmPaymentPage(webDriver)
        val failureMessage = page.getFailureMessage()
        assertEquals(expectedMessage, failureMessage, "Expected error message not found")
    }

    private fun checkPaymentStatusWithRetries(
        operationId: String,
        expectedStatus: String,
        maxRetries: Int = PAYMENT_STATUS_UPDATE_TIMEOUT_SECONDS
    ) {
        val statusRequest = HttpGet(STATUS_URL_TEMPLATE.format(operationId))

        repeat(maxRetries) {
            val statusResponse = httpClient.execute(statusRequest)
            val statusResponseBody = EntityUtils.toString(statusResponse.entity)

            assertEquals(200, statusResponse.statusLine.statusCode)

            val jsonNode: JsonNode = objectMapper.readTree(statusResponseBody)
            when (val operationStatus = jsonNode["result"]["operation"]["status"].asText()) {
                expectedStatus -> {
                    return
                }

                "processing" -> {
                    wait(1000)
                }

                else -> {
                    throw AssertionError("Unexpected status '$operationStatus'.")
                }
            }
        }
        throw AssertionError("Max retries reached. Status did not change to '$expectedStatus'.")
    }
}
