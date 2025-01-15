package helpers

import org.apache.http.client.config.RequestConfig
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.ssl.SSLContexts

object HttpClientFactory {

    @Synchronized
    fun customHttpClient(httpClientInit: HttpClientBuilder.() -> Unit = {}): CloseableHttpClient {
        System.setProperty("jsse.enableSNIExtension", "false")

        val httpClientBuilder = HttpClients
            .custom()
            .setKeepAliveStrategy { _, _ -> 20000 }
            .setDefaultRequestConfig(
                RequestConfig
                    .custom()
                    .setConnectTimeout(20000)
                    .setSocketTimeout(20000)
                    .build()
            )
            .setSSLContext(
                SSLContexts
                    .custom()
                    .loadTrustMaterial(null) { _, _ -> true }
                    .setProtocol("TLS")
                    .build()
            )
            .setConnectionManager(PoolingHttpClientConnectionManager(
                RegistryBuilder.create<ConnectionSocketFactory>()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", SSLConnectionSocketFactory(SSLContexts
                        .custom()
                        .loadTrustMaterial(null) { _, _ -> true }
                        .setProtocol("TLS")
                        .build(), NoopHostnameVerifier()))
                    .build()
            ))

        httpClientBuilder.httpClientInit()
        return httpClientBuilder.build()
    }

}