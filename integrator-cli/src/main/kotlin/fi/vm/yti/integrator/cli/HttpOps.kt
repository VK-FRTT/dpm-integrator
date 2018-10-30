package fi.vm.yti.integrator.cli

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.Closeable

object HttpOps : Closeable {

    private var httpClient: OkHttpClient? = null

    fun fetchJsonData(
        url: HttpUrl
    ): String {

        val request = Request.Builder()
            .get()
            .url(url)
            .header("Accept", "application/json")
            .build()

        val response = try {
            httpClient().newCall(request).execute()
        } catch (e: java.net.UnknownHostException) {
            throwFail("Could not determine the server IP address")
        } catch (e: java.net.ConnectException) {
            throwFail("Could not connect the server")
        } catch (e: java.net.SocketTimeoutException) {
            throwFail("The server communication timeout")
        }

        if (!response.isSuccessful) {
            throwFail("JSON content fetch failed: HTTP ${response.code()} (${response.message()})")
        }

        return response.body().use {
            it ?: thisShouldNeverHappen("HTTP response body missing")
            it.string()
        }
    }

    override fun close() {
        httpClient?.let { it.dispatcher().executorService().shutdown() }
        httpClient = null
    }

    private fun httpClient(): OkHttpClient {
        return httpClient ?: createHttpClient().also { httpClient = it }
    }

    private fun createHttpClient(): OkHttpClient {
        return OkHttpClient().newBuilder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun useCustomHttpClient(httpClient: OkHttpClient) {
        close()

        this.httpClient = httpClient
    }
}
