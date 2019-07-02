package fi.vm.yti.integrator.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.vm.yti.integrator.dm.DataModelInfo
import fi.vm.yti.integrator.dm.DataModelVersionInfo
import fi.vm.yti.integrator.dm.TaskStatusInfo
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

class DataModelerClient(
    clientProfilePath: Path,
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
    private val profile = loadClientProfile(clientProfilePath)
    private var accessToken: String? = null

    fun authenticate(username: String, password: String) {
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
                "${profile.clientId}:${profile.clientSecret}".toByteArray(utf8Charset)
            )

        val authorization = "Basic $clientCredentialsBase64"

        val request = Request.Builder()
            .url("${profile.authServiceHost}/oauth/token")
            .addHeader("Authorization", authorization)
            .post(body)
            .build()

        val result = executeAndExpectSuccess(request, "Authentication")

        val responseJson = mapper.readTree(result.responseBody)
        accessToken = responseJson.get("access_token").asText()
    }

    fun listDataModels(): List<DataModelInfo> {
        val request = authorizedRequestBuilder()
            .get()
            .url("${profile.hmrServiceHost}/model/data/")
            .build()

        val result = executeAndExpectSuccess(request, "Listing data models")

        return mapper.readValue(result.responseBody)
    }

    fun selectTargetDataModelVersion(
        targetDataModelName: String
    ): DataModelVersionInfo {

        fun dataModelSelectionFailed(description: String) {
            throwFail("Data model selection failed. $description")
        }

        val dataModels = listDataModels()

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

    fun uploadDatabaseAndScheduleImport(
        sourceFilePath: Path,
        targtDataModelId: String
    ): String {
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
            .url("${profile.exportImportServiceHost}/api/import/db/schedule")
            .addHeader("dataModelId", targtDataModelId)
            .build()

        val result = executeAndExpectSuccess(request, "Database upload")

        return result.responseBody
    }

    fun fetchTaskStatus(
        taskId: String,
        dataModelVersionId: String
    ): TaskStatusInfo {
        val request = authorizedRequestBuilder()
            .get()
            .url("${profile.exportImportServiceHost}/api/task/$taskId/import")
            .addHeader("dataModelVersionId", dataModelVersionId)
            .build()

        val result = executeAndExpectSuccess(request, "Fetch task status")

        return mapper.readValue(result.responseBody)
    }

    private fun loadClientProfile(clientProfilePath: Path): ClientProfile {
        val input = mapper.readValue<ClientProfileInput>(clientProfilePath.toUri().toURL())
        return input.toValidProfile()
    }

    private fun authorizedRequestBuilder(): Request.Builder {
        requireNotNull(accessToken)

        return Request.Builder()
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer $accessToken")
    }

    private fun execute(request: Request): HttpRequestResult {
        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: java.net.UnknownHostException) {
            throwFail("Could not determine the server IP address. Url: ${request.url()}")
        } catch (e: java.net.ConnectException) {
            throwFail("Could not connect the server. Url: ${request.url()}")
        } catch (e: java.net.SocketTimeoutException) {
            throwFail("The server communication timeout. ${request.url()}")
        }

        return HttpRequestResult(
            statusCode = response.code(),
            statusMessage = response.message(),
            responseBody = response.body().use {
                it?.string() ?: ""
            }
        )
    }

    private fun executeAndExpectSuccess(request: Request, requestName: String): HttpRequestResult {
        val result = execute(request)

        if (!result.isSuccessful()) {
            throwFail("$requestName failed. HTTP status: ${result.statusCode}. Response body: ${result.responseBody}")
        }

        return result
    }
}
