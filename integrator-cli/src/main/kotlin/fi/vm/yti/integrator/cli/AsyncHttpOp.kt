package fi.vm.yti.integrator.cli

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

internal class AsyncHttpOp(private val requestName: String) : Callback {

    private lateinit var request: Request
    private var failureException: Exception? = null
    private var successResult: HttpRequestResult? = null

    override fun onFailure(call: Call, e: IOException) {
        request = call.request()
        failureException = e
    }

    @Throws(IOException::class)
    override fun onResponse(call: Call, response: Response) {
        request = call.request()
        successResult = HttpResultMapper.responseToHttpRequestResult(response)
    }

    fun isPending(): Boolean = !isCompleted()

    fun isCompleted(): Boolean {
        if (failureException != null) {
            return true
        }

        if (successResult != null) {
            return true
        }

        return false
    }

    fun expectSuccessAndGetResponseBody(): String {
        require(isCompleted())

        failureException?.run {
            HttpResultMapper.handleRequestException(request, this)
        }

        successResult?.run {
            HttpResultMapper.handleRequestHttpFailure(requestName, this)
            return this.responseBody
        }

        thisShouldNeverHappen("AsyncHttpOp not completed")
    }
}
