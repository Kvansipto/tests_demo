package helpers

import org.apache.http.impl.client.CloseableHttpClient
import org.zalando.logbook.Logbook
import org.zalando.logbook.StreamHttpLogWriter
import org.zalando.logbook.httpclient.LogbookHttpRequestInterceptor
import org.zalando.logbook.httpclient.LogbookHttpResponseInterceptor

private val logbook by lazy {
    Logbook.builder()
        .writer(StreamHttpLogWriter(System.out))
        .build()
}

private val logbookRequestInterceptor by lazy { LogbookHttpRequestInterceptor(logbook) }
private val logbookResponseInterceptor by lazy { LogbookHttpResponseInterceptor() }


val httpClient: CloseableHttpClient
    get() = HttpClientFactory.customHttpClient {
        addInterceptorLast(logbookRequestInterceptor)
        addInterceptorLast(logbookResponseInterceptor)
    }
