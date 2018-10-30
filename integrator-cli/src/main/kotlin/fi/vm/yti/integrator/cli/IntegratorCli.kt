package fi.vm.yti.integrator.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import java.io.BufferedWriter
import java.io.Closeable
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.io.PrintWriter
import java.nio.charset.Charset
import java.util.Base64


const val INTEGRATOR_CLI_SUCCESS = 0
const val INTEGRATOR_CLI_FAIL = 1

class IntegratorCli(
    outStream: PrintStream,
    errStream: PrintStream,
    charset: Charset,
    private val definedOptions: DefinedOptions
) : Closeable {

    private val outWriter = PrintWriter(BufferedWriter(OutputStreamWriter(outStream, charset)), true)
    private val errWriter = PrintWriter(BufferedWriter(OutputStreamWriter(errStream, charset)), true)

    override fun close() {
        outWriter.close()
        errWriter.close()
    }

    fun execute(args: Array<String>): Int {
        return withExceptionHarness {
            val detectedOptions = definedOptions.detectOptionsFromArgs(args)

            if (detectedOptions.cmdShowHelp) {
                definedOptions.printHelp(outWriter)
                throwHalt()
            }

            detectedOptions.ensureSingleCommandGiven()

            if (detectedOptions.cmdUploadSqliteDb != null) {

                val client = OkHttpClient.Builder()
                    .addNetworkInterceptor(LoggingInterceptor())
                    .build()

                val mediaType = MediaType.parse("application/json")

                val payload = jacksonObjectMapper().writeValueAsString(
                    mapOf(
                        "grant_type" to "password",
                        "password" to "password",
                        "username" to "username"
                    )
                )

                val body = RequestBody.create(
                    mediaType,
                    payload.toByteArray(Charset.forName("UTF-8"))
                )


                val clientCredentials = "client_api_key:client_api_secret"
                val clientCredentialsBase64 = Base64
                    .getEncoder()
                    .encodeToString(
                        clientCredentials.toByteArray(
                            Charset.forName("UTF-8")
                        )
                    )

                val authorization = "Basic ${clientCredentialsBase64}"

                val request = Request.Builder()
                    .url("https://dmUserSsoApi/oauth/token")
                    .addHeader("Authorization", authorization)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
            }
        }
    }

    private fun withExceptionHarness(steps: () -> Unit): Int {
        return try {
            steps()
            INTEGRATOR_CLI_SUCCESS
        } catch (exception: HaltException) {
            INTEGRATOR_CLI_SUCCESS
        } catch (exception: FailException) {
            errWriter.println("yti-brag-dm-integrator: ${exception.message}")
            errWriter.println()

            INTEGRATOR_CLI_FAIL
        } catch (exception: Throwable) {
            errWriter.println("yti-brag-dm-integrator:")
            exception.printStackTrace(errWriter)
            errWriter.println()

            INTEGRATOR_CLI_FAIL
        }
    }
}


internal class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val t1 = System.nanoTime()

        System.out.println("Sending request ${request.url()} on ${chain.connection()}")
        System.out.print(request.headers())

        request.body()?.let { body ->
            val buffer = Buffer().also { body.writeTo(it); it.close() }
            System.out.println("Body:\n${buffer.snapshot().utf8()}\n\n")
        }

        val response = chain.proceed(request)

        val t2 = System.nanoTime()
        System.out.println("Received response for ${response.request().url()} in ${(t2 - t1) / 1e6}ms")
        System.out.println(response.headers())

        System.out.println("Body:\n${response.peekBody(2048).string()}\n\n")

        return response
    }


}
