package fi.vm.yti.integrator.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.vm.yti.integrator.apimodel.DataModelInfo
import fi.vm.yti.integrator.apimodel.DataModelVersionInfo
import fi.vm.yti.integrator.apimodel.TaskStatusInfo
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.PrintWriter
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.Base64
import java.util.concurrent.TimeUnit

internal class DpmToolClient(
    private val toolConfig: DpmToolConfig,
    verbosity: Verbosity,
    outWriter: PrintWriter
) {
    private val httpClient = OkHttpClient
        .Builder()
        .addNetworkInterceptor(LoggingInterceptor(verbosity, outWriter))
        .writeTimeout(360, TimeUnit.SECONDS)
        .build()

    private val mapper = jacksonObjectMapper()
    private val utf8Charset = Charset.forName("UTF-8")
    private var accessToken: String? = null

    fun authenticateSync(username: String, password: String) {
        val loginPayload = mapper.writeValueAsString(
            mapOf(
                "grant_type" to "password",
                "username" to username,
                "password" to password
            ) as Any
        )

        val body = RequestBody.create(
            MediaType.parse("application/json"),
            loginPayload.toByteArray(utf8Charset)
        )

        val clientCredentialsBase64 = Base64
            .getEncoder()
            .encodeToString(
                "${toolConfig.clientId}:${toolConfig.clientSecret}".toByteArray(utf8Charset)
            )

        val authorization = "Basic $clientCredentialsBase64"

        val request = Request.Builder()
            .url("${toolConfig.authServiceHost}/oauth/token")
            .addHeader("Authorization", authorization)
            .post(body)
            .build()

        val result = executeRequestSyncAndExpectSuccess(request, "Authentication")

        val responseJson = mapper.readTree(result.responseBody)
        accessToken = responseJson.get("access_token").asText()
    }

    fun listDataModelsSync(): List<DataModelInfo> {
        val request = authorizedRequestBuilder()
            .get()
            .url("${toolConfig.hmrServiceHost}/model/data/")
            .build()

        val result = executeRequestSyncAndExpectSuccess(request, "Listing data models")

        return mapper.readValue(result.responseBody)
    }

    fun selectTargetDataModelVersionSync(
        targetDataModelName: String
    ): DataModelVersionInfo {

        fun dataModelSelectionFailed(description: String) {
            throwFail("Data model selection failed. $description")
        }

        val dataModels = listDataModelsSync()

        val matchingModels = dataModels.filter { it.name == targetDataModelName }

        if (matchingModels.isEmpty()) {
            dataModelSelectionFailed("No data model found with given name: $targetDataModelName")
        }

        if (matchingModels.size >= 2) {
            dataModelSelectionFailed("Multiple data models having given name: $targetDataModelName")
        }

        val targetModel = matchingModels.first()
        val versions = targetModel.dataModelVersions.sortedBy { it.creationDate }
        if (versions.isEmpty()) {
            dataModelSelectionFailed("Data model does not have any version: $targetDataModelName")
        }

        val targetVersion = versions.last()

        if (targetModel.id != targetVersion.dataModelId) {
            dataModelSelectionFailed("Data model selection failed. Data model ID and version ID do not match: $targetDataModelName")
        }

        return targetVersion
    }

    fun uploadDatabaseAndScheduleImportAsync(
        sourceFilePath: Path,
        targtDataModelId: String
    ): AsyncHttpOp {
        val requestBody = MultipartBody
            .Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                sourceFilePath.fileName.toString(),
                RequestBody.create(
                    MediaType.parse("application/octet-stream"),
                    sourceFilePath.toFile()
                )
            )
            .build()

        val request = authorizedRequestBuilder()
            .post(requestBody)
            .url("${toolConfig.exportImportServiceHost}/api/import/db/schedule")
            .addHeader("dataModelId", targtDataModelId)
            .build()

        return enqueueAsyncOp(request, "Database upload")
    }

    fun fetchTaskStatusSync(
        taskId: String,
        dataModelVersionId: String
    ): TaskStatusInfo {
        val request = authorizedRequestBuilder()
            .get()
            .url("${toolConfig.exportImportServiceHost}/api/task/$taskId/import")
            .addHeader("dataModelVersionId", dataModelVersionId)
            .build()

        val result = executeRequestSyncAndExpectSuccess(request, "Fetch task status")

        return mapper.readValue(result.responseBody)
    }

    private fun authorizedRequestBuilder(): Request.Builder {
        requireNotNull(accessToken)

        return Request.Builder()
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer $accessToken")
    }

    private fun executeRequestSync(
        request: Request
    ): HttpRequestResult {
        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: Exception) {
            HttpResultMapper.handleRequestException(request, e)
        }

        return HttpResultMapper.responseToHttpRequestResult(response)
    }

    private fun executeRequestSyncAndExpectSuccess(
        request: Request,
        requestName: String
    ): HttpRequestResult {
        val result = executeRequestSync(request)

        HttpResultMapper.handleRequestHttpFailure(requestName, result)

        return result
    }

    private fun enqueueAsyncOp(request: Request, requestName: String): AsyncHttpOp {
        val asyncOp = AsyncHttpOp(requestName)
        httpClient.newCall(request).enqueue(asyncOp)
        return asyncOp
    }
}
