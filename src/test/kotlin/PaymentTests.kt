import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import utils.AuthUtils
import utils.DataUtils
import utils.wait

class PaymentTests {
    private val createUrl = "https://test-app-ext.2ip.in/api/payment/create"
    private val statusUrl = "https://test-app-ext.2ip.in/api/payment/status/%s"

    private val objectMapper = ObjectMapper()

    companion object {
        @JvmStatic
        @AfterAll
        fun tearDown(): Unit {
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
        //todo post?
        webDriver.get("$uri?operationId=$operationId")

        val paymentCode = if (isRejected) 1231 else 1234
        val paymentCodeInput = webDriver.findElement(By.id("code"))
        paymentCodeInput.sendKeys(paymentCode.toString())

        val submitButton = webDriver.findElement(By.cssSelector("button[type='submit']"))
        submitButton.click()

        val successBlockLocator = By.className("success-container")

        val wait = WebDriverWait(webDriver, 10)
        val successBlock = wait.until(ExpectedConditions.visibilityOfElementLocated(successBlockLocator))

        val successBlockTitle = successBlock.findElement(By.cssSelector("h1")).text
        val successBlockText = successBlock.findElement(By.cssSelector("p")).text

        assertEquals("Success!", successBlockTitle, "Expected success message not found")
        assertTrue(
            successBlockText.contains("Operation $operationId was accepted"),
            "Operation ID in success message is incorrect"
        )
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
