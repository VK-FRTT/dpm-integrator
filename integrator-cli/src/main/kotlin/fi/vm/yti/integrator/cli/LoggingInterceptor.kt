package fi.vm.yti.integrator.cli

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.PrintWriter
import java.lang.Math.round

const val REQUEST_LOG_PREFIX = "> "
const val RESPONSE_LOG_PREFIX = "< "
const val BODY_SNIPPET_LENGTH = 1024

class LoggingInterceptor(val verbosity: Verbosity, val writer: PrintWriter) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val t1 = System.nanoTime()

        if (verbosity >= Verbosity.INFO) {
            writer.println("")
            writer.println("")
            writer.println("${REQUEST_LOG_PREFIX}Request: ${request.method()} ${request.url()}")
            writer.println("${REQUEST_LOG_PREFIX}Via: ${chain.connection()}")
        }

        if (verbosity >= Verbosity.DEBUG) {
            val headersMessage = formatHeadersLogMessage(request.headers(), REQUEST_LOG_PREFIX)
            writer.println(headersMessage)
        }

        if (verbosity >= Verbosity.TRACE) {
            val body = request.body()
            if (body != null) {
                val bodyBuffer = Buffer().also {
                    body.writeTo(it)
                    it.close()
                }

                val bodySnippet = bodyBuffer.snapshot().utf8().take(BODY_SNIPPET_LENGTH)
                writer.println("${REQUEST_LOG_PREFIX}Body snippet: $bodySnippet")
            }
        }

        val response = chain.proceed(request)

        val t2 = System.nanoTime()
        val timing = round((t2 - t1) / 1e6)

        if (verbosity >= Verbosity.INFO) {
            writer.println("")
            writer.println("")
            writer.println("${RESPONSE_LOG_PREFIX}Response: ${request.method()} ${request.url()}")
            writer.println("${RESPONSE_LOG_PREFIX}Total time: $timing ms")
        }

        if (verbosity >= Verbosity.DEBUG) {
            val headersMessage = formatHeadersLogMessage(response.headers(), RESPONSE_LOG_PREFIX)
            writer.println(headersMessage)
        }

        if (verbosity >= Verbosity.TRACE) {
            writer.println("${RESPONSE_LOG_PREFIX}Body snippet: ${response.peekBody(BODY_SNIPPET_LENGTH.toLong()).string()}")
        }

        return response
    }

    private fun formatHeadersLogMessage(headers: Headers, linePrefix: String): String {
        val headerLines = mutableListOf<String>()

        for (i in 0 until headers.size()) {
            headerLines.add("${linePrefix}${headers.name(i)}: ${headers.value(i)}")
        }

        return headerLines.joinToString(separator = "\n")
    }
}
