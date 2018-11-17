package fi.vm.yti.integrator.cli

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.PrintWriter

class LoggingInterceptor(val verbosity: Verbosity, val writer: PrintWriter) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val t1 = System.nanoTime()

        if (verbosity >= Verbosity.INFO) {
            writer.println("\nSending request ${request.url()} on ${chain.connection()}")
        }

        if (verbosity >= Verbosity.DEBUG) {
            writer.print(request.headers())

            request.body()?.let { body ->
                val buffer = Buffer().also { body.writeTo(it); it.close() }
                writer.println("`${buffer.snapshot().utf8()}`")
            }
        }

        val response = chain.proceed(request)

        val t2 = System.nanoTime()

        if (verbosity >= Verbosity.INFO) {
            writer.println("\nReceived response for ${response.request().url()} in ${(t2 - t1) / 1e6}ms")
        }

        if (verbosity >= Verbosity.DEBUG) {
            writer.print(response.headers())
            writer.println("`${response.peekBody(32768).string()}`")
        }

        return response
    }
}
