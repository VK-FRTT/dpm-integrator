package fi.vm.yti.integrator.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.vm.yti.integrator.dm.DataModelInfo
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

class DataModelerClient(val profile: ClientProfile, verbosity: Verbosity, outWriter: PrintWriter) {

    private val httpClient = OkHttpClient
        .Builder()
        .addNetworkInterceptor(LoggingInterceptor(verbosity, outWriter))
        .build()

    private val mapper = jacksonObjectMapper()
    private var accessToken: String? = null

    fun login(username: String, password: String) {
        val mediaType = MediaType.parse("application/json")

        val payload = jacksonObjectMapper().writeValueAsString(
            mapOf(
                "grant_type" to "password",
                "username" to username,
                "password" to password
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
            .url("${profile.authApiUrl}/oauth/token")
            .addHeader("Authorization", authorization)
            .post(body)
            .build()

        val responseText = executeAndGetResponseText(request, "Login")
        val responseJson = mapper.readTree(responseText)
        accessToken = responseJson.get("access_token").asText()
    }

    fun listDataModels(): List<DataModelInfo> {
        val modelsRequest = newAuthorizedRequestBuilder()
            .get()
            .url("${profile.hmrApiUrl}/model/data/")
            .build()

        val responseText = executeAndGetResponseText(modelsRequest, "Get available models")

        return mapper.readValue(responseText)
    }


    fun importDataModel(filePath: Path, dataModelId: String): String {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                filePath.fileName.toString(),
                RequestBody.create(MediaType.parse("text/csv"), filePath.toFile()))
            .build()

        val request = newAuthorizedRequestBuilder()
            .post(requestBody)
            .url("${profile.exportImportApiUrl}/api/import/db/schedule")
            .addHeader("dataModelId", dataModelId)
            .build()

        return executeAndGetResponseText(request, "Import database to Data Modeler")
    }

    fun fetchTaskStatus(taskId: String, dataModelVersionId: String): TaskStatusInfo {
        val request = newAuthorizedRequestBuilder()
            .get()
            .url("${profile.exportImportApiUrl}/api/task/$taskId/import")
            .addHeader("dataModelVersionId", dataModelVersionId)
            .build()

        val response = httpClient.newCall(request).execute()
        val bodyText = response.body()!!.string()

        if (response.isSuccessful) {
            return mapper.readValue(bodyText)
        }

        if (response.code() == 404) {
            return TaskStatusInfo(
                type = "",
                taskId = "",
                taskStatus = "UNAVAILABLE",
                taskType = "",
                resultDataModelId = "",
                sourceDBFileName = ""
            )
        }

        throwFail("Get task details failed. $bodyText")
    }

    private fun newAuthorizedRequestBuilder(): Request.Builder {
        return Request.Builder()
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer ${accessToken}")
    }

    private fun executeAndGetResponseText(request: Request, reqDescription: String): String {
        val response = httpClient.newCall(request).execute()
        val bodyText = response.body()!!.string()
        if (!response.isSuccessful) {
            throwFail("$reqDescription failed. $bodyText")
        }

        return bodyText
    }
}
