package framework

import org.openqa.selenium.WebDriver
import kotlin.concurrent.getOrSet

private val webDrivers = ThreadLocal<WebDriver>()

val webDriver: WebDriver
    get() = webDrivers.getOrSet {
        createWebDriver()
    }

fun closeWebDriver() {
    webDriverOptional?.apply {
        quit()
        webDrivers.remove()
    }
}

val webDriverOptional: WebDriver?
    get() = webDrivers.get()