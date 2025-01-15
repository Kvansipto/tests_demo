import framework.byClass
import framework.byText
import framework.closeWebDriver
import framework.webDriver
import helpers.httpClient
import org.apache.http.client.methods.RequestBuilder
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.should
import ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.timeoutHasExpired
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed

class ExampleTest {

    private val gettingStartedLink by lazy { webDriver.findElement(byText("Getting started")) }
    private val leadContent by lazy { webDriver.findElement(byClass("td-content")).findElement(byClass("lead")) }

    @AfterEach
    fun tearDown() {
        closeWebDriver()
    }

    @Test
    fun simpleWebTest() {
        webDriver.get("https://www.selenium.dev/documentation/webdriver/")
        assertThat(
            "проверяем наличие ссылки 'Getting started'",
            gettingStartedLink, should(isDisplayed()).whileWaitingUntil(timeoutHasExpired(1000))
        )
        gettingStartedLink.click()

        assertThat(
            "проверяем наличие вводного текста",
            leadContent, should(isDisplayed()).whileWaitingUntil(timeoutHasExpired(1000))
        )
        assertThat(
            "проверяем текст заголовка",
            leadContent.text, equalTo("If you are new to Selenium, we have a few resources that can help you get up to speed right away.")
        )
    }

    @Test
    fun simpleHttpTest() {
        val request = RequestBuilder.create("GET")
            .setUri("https://httpbin.org/get")
            .build()
        val response = httpClient.execute(request)

        assertThat(
            "проверяем статус ответа",
            response.statusLine.statusCode, equalTo(200)
        )
    }

}