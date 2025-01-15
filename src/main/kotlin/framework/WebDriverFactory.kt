package framework

import org.openqa.selenium.WebDriver

fun createWebDriver(): WebDriver {
    return Browser.CHROME.run {
        installDriver()
        createDriver()
    }
}