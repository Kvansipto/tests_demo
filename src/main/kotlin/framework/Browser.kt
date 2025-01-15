package framework

import io.github.bonigarcia.wdm.WebDriverManager
import java.time.Duration
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

enum class Browser {
    CHROME {
        override fun installDriver() {
            val driverVersion = downloadedChromedriver
            println("Запускаем тесты на chromedriver $driverVersion")
        }

        override fun createDriver(): WebDriver {
            val webDriver = ChromeDriver(ChromeOptions().addArguments("--disable-search-engine-choice-screen"))
            return configure(webDriver)
        }
    }
    ;

    abstract fun createDriver(): WebDriver

    open fun installDriver() = Unit
}

private fun configure(webDriver: WebDriver): WebDriver {
    webDriver.manage().timeouts().implicitlyWait(Duration.ZERO)
    return webDriver
}

private val downloadedChromedriver by lazy {
    WebDriverManager.chromedriver().run {
        setup()
        downloadedDriverVersion
    }
}