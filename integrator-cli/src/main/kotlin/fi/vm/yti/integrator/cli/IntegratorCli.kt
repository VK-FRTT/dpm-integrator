package fi.vm.yti.integrator.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
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

                val profile =
                    jacksonObjectMapper().readValue<ClientProfileInput>(detectedOptions.clientProfile!!.toUri().toURL())
                        .toValidProfile()

                val client = OkHttpClient.Builder()
                    .addNetworkInterceptor(LoggingInterceptor(detectedOptions.verbosity, outWriter))
                    .build()

                val mediaType = MediaType.parse("application/json")

                val payload = jacksonObjectMapper().writeValueAsString(
                    mapOf(
                        "grant_type" to "password",
                        "username" to profile.userName,
                        "password" to profile.userPassword
                    ) as Any
                )

                val body = RequestBody.create(
                    mediaType,
                    payload.toByteArray(Charset.forName("UTF-8"))
                )

                val clientCredentials = "${profile.clientId}:${profile.clientSecret}"
                val clientCredentialsBase64 = Base64
                    .getEncoder()
                    .encodeToString(
                        clientCredentials.toByteArray(
                            Charset.forName("UTF-8")
                        )
                    )

                val authorization = "Basic ${clientCredentialsBase64}"

                val request = Request.Builder()
                    .url("${profile.authUrl}/oauth/token")
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

