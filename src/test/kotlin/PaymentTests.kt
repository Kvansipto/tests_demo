import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import framework.pages.ConfirmPaymentPage
import framework.webDriver
import helpers.httpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import utils.AuthUtils
import utils.DataUtils
import utils.wait

class PaymentTests {
    private val baseUrl = "https://test-app-ext.2ip.in"
    private val createUrl = "$baseUrl/api/payment/create"
    private val statusUrl = "$baseUrl/api/payment/status/%s"
    private val confirmUrl = "$baseUrl/operations/confirm"
    private val fakeOperationId = "fake"

    private val objectMapper = ObjectMapper()

    companion object {
        @JvmStatic
        @AfterAll
        fun tearDown() {
            webDriver.quit()
        }
    }

    @Test
    fun `test payment done status`() {
        val (uri, operationId) = createPayment()

        checkPaymentStatusWithRetries(operationId, "created")

        confirmPayment(uri, operationId, false)

        checkPaymentStatusWithRetries(operationId, "done")
    }

    @Test
    fun `test payment rejected status`() {
        val (uri, operationId) = createPayment()

        checkPaymentStatusWithRetries(operationId, "created")

        confirmPayment(uri, operationId, true)

        checkPaymentStatusWithRetries(operationId, "rejected")
    }

    @Test
    fun `test payment already processed error`() {
        val (uri, operationId) = createPayment()

        checkPaymentStatusWithRetries(operationId, "created")

        confirmPayment(uri, operationId, isRejected = false)
        checkPaymentStatusWithRetries(operationId, "done")

        val failMessage = confirmPaymentWithInvalidId(uri, operationId)

        assertEquals("Operation already processed.", failMessage, "Expected error message not found")
    }

    @Test
    fun `test payment does not exist error`() {
        val failMessage = confirmPaymentWithInvalidId(confirmUrl, fakeOperationId)
        assertEquals("Operation does not exists.", failMessage, "Expected error message not found")
    }

    private fun createPayment(): Pair<String, String> {
        val createRequest = HttpPost(createUrl)

        createRequest.addHeader("Authorization", AuthUtils.getAuthHeader())
        createRequest.addHeader("Content-Type", "application/json")

        val name = DataUtils.generateRandomName()
        val phone = DataUtils.generateRandomPhone()
        val amount = DataUtils.generateRandomAmount()

        val requestBody = """
        {
          "endUser": {
            "fullName": "%s",
            "phone": "%s"
          },
          "order": {
            "amount": {
              "currency": "USD",
              "value": "%s"
            }
          }
        }
    """.trimIndent().format(name, phone, amount)

        createRequest.entity = StringEntity(requestBody)

        val createResponse = httpClient.execute(createRequest)
        val createResponseBody = EntityUtils.toString(createResponse.entity)

        assertEquals(200, createResponse.statusLine.statusCode)
        assertTrue(createResponseBody.contains("\"status\":\"success\""))
        assertTrue(createResponseBody.contains("\"status\":\"created\""))

        val jsonNode: JsonNode = objectMapper.readTree(createResponseBody)
        val uri = jsonNode["result"]["location"]["uri"].asText()
        val operationId = jsonNode["result"]["operation"]["id"].asText()

        return Pair(uri, operationId)
    }

    private fun confirmPayment(uri: String, operationId: String, isRejected: Boolean) {
        val page = ConfirmPaymentPage(webDriver)
        page.openConfirmPage(uri, operationId)

        val paymentCode = if (isRejected) 1231 else 1234
        page.enterPaymentCode(paymentCode.toString())
        page.submitPayment()

        val successMessage = page.getSuccessMessage()
        assertTrue(
            successMessage.contains("Operation $operationId was accepted"),
            "Operation ID in success message is incorrect"
        )
    }

    private fun confirmPaymentWithInvalidId(uri: String, operationId: String): String {
        val page = ConfirmPaymentPage(webDriver)
        page.openConfirmPage(uri, operationId)
        page.enterPaymentCode("1234")
        page.submitPayment()

        return page.getFailureMessage()
    }


    private fun checkPaymentStatusWithRetries(operationId: String, expectedStatus: String, maxRetries: Int = 14) {
        val statusRequest = HttpGet(statusUrl.format(operationId))

        var attempts = 0
        while (attempts < maxRetries) {

            val statusResponse = httpClient.execute(statusRequest)
            val statusResponseBody = EntityUtils.toString(statusResponse.entity)

            assertEquals(200, statusResponse.statusLine.statusCode)

            val jsonNode: JsonNode = objectMapper.readTree(statusResponseBody)
            when (val operationStatus = jsonNode["result"]["operation"]["status"].asText()) {
                expectedStatus -> {
                    return
                }

                "processing" -> {
                    attempts++
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
