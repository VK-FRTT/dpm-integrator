package fi.vm.yti.integrator.cli

import okhttp3.Request
import okhttp3.Response

internal class HttpResultMapper {

    companion object {

        fun responseToHttpRequestResult(response: Response): HttpRequestResult {
            return HttpRequestResult(
                statusCode = response.code(),
                statusMessage = response.message(),
                responseBody = response.body().use
                {
                    it?.string() ?: ""
                }
            )
        }

        fun handleRequestException(request: Request, exception: Exception): Nothing {

            when (exception) {
                is java.net.UnknownHostException ->
                    throwFail("Could not determine the server IP address. Url: ${request.url()}")
                is java.net.ConnectException ->
                    throwFail("Could not connect the server. Url: ${request.url()}")
                is java.net.SocketTimeoutException ->
                    throwFail("The server communication timeout. ${request.url()}")
                else -> throw exception
            }
        }

        fun handleRequestHttpFailure(requestName: String, requestResult: HttpRequestResult) {
            if (!requestResult.isSuccessful()) {
                throwFail("$requestName failed. HTTP status: ${requestResult.statusCode}. Response body: ${requestResult.responseBody}")
            }
        }
    }
}
