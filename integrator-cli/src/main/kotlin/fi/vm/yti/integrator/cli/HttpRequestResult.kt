package fi.vm.yti.integrator.cli

internal data class HttpRequestResult(
    val statusCode: Int,
    val statusMessage: String,
    val responseBody: String
) {
    fun isSuccessful(): Boolean {
        return statusCode >= 200 && statusCode < 300
    }
}
