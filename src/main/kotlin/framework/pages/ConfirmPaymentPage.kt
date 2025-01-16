package framework.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

class ConfirmPaymentPage(private val driver: WebDriver) {

    private val paymentCodeInput: By = By.id("code")
    private val submitButton: By = By.cssSelector("button[type='submit']")
    private val successBlock: By = By.className("success-container")
    private val failBlock: By = By.className("fail-container")

    fun openConfirmPage(uri: String, operationId: String) {
        driver.get("$uri?operationId=$operationId")
    }

    fun enterPaymentCode(code: String) {
        driver.findElement(paymentCodeInput).sendKeys(code)
    }

    fun submitPayment() {
        driver.findElement(submitButton).click()
    }

    fun getSuccessMessage(): String {
        return getMessageFromBlock(successBlock)
    }

    fun getFailureMessage(): String {
        return getMessageFromBlock(failBlock)
    }

    private fun getMessageFromBlock(blockLocator: By): String {
        val wait = WebDriverWait(driver, 10)
        val blockElement: WebElement = wait.until(ExpectedConditions.visibilityOfElementLocated(blockLocator))
        return blockElement.findElement(By.cssSelector("p")).text
    }
}
