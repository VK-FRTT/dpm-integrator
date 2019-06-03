package fi.vm.yti.integrator.cli

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KProperty1

data class UploadDatabaseParams(
    val databasePath: Path,
    val targetDataModelName: String,
    val clientProfile: Path,
    val username: String,
    val password: String,
    val verbosity: Verbosity
)

data class ListDataModelsParams(
    val clientProfile: Path,
    val username: String,
    val password: String,
    val verbosity: Verbosity
)

data class DetectedOptions(
    val cmdShowHelp: Boolean,
    val cmdListDataModels: Boolean,
    val cmdUploadDatabase: Path?,
    val targetDataModelName: String?,
    val clientProfile: Path?,
    val username: String?,
    val password: String?,
    val verbosity: Verbosity
) {

    fun ensureSingleCommandGiven() {
        val commandCount = listOf<Any?>(
            cmdListDataModels,
            cmdUploadDatabase
        ).count {
            if (it is Boolean) {
                it == true
            } else {
                it != null
            }
        }

        if (commandCount != 1) {
            throwFail("Single command with proper argument must be given")
        }
    }

    fun validUploadDatabaseParams(): UploadDatabaseParams {
        requireNotNull(cmdUploadDatabase)

        val results = CommandValidationResults()

        val clientProfile = selectValidClientProfile(results)
        requireNonNullOption(DetectedOptions::targetDataModelName, TARGET_DATA_MODEL_OPTION_NAME, results)
        requireNonNullOption(DetectedOptions::username, USERNAME_OPTION_NAME, results)
        requireNonNullOption(DetectedOptions::password, PASSWORD_OPTION_NAME, results)

        results.failOnErrors()

        return UploadDatabaseParams(
            databasePath = cmdUploadDatabase!!,
            targetDataModelName = targetDataModelName!!,
            clientProfile = clientProfile,
            username = username!!,
            password = password!!,
            verbosity = verbosity
        )
    }

    fun validListDataModelsParams(): ListDataModelsParams {
        require(cmdListDataModels)

        val validationResults = CommandValidationResults()

        val clientProfile = selectValidClientProfile(validationResults)
        requireNonNullOption(DetectedOptions::username, USERNAME_OPTION_NAME, validationResults)
        requireNonNullOption(DetectedOptions::password, PASSWORD_OPTION_NAME, validationResults)

        validationResults.failOnErrors()

        return ListDataModelsParams(
            clientProfile = clientProfile,
            username = username!!,
            password = password!!,
            verbosity = verbosity
        )
    }

    private fun <I : Any, P : Any?> requireNonNullOption(
        property: KProperty1<I, P>,
        optionName: String,
        validationResults: CommandValidationResults
    ) {
        property.getter.call(this)
            ?: validationResults.add(optionName, "missing required parameter value")
    }

    private fun selectValidClientProfile(validationResults: CommandValidationResults): Path {
        return if (clientProfile != null) {
            if (!Files.exists(clientProfile)) {

                validationResults.add("client profile", "file not found", clientProfile)
            }
            clientProfile.toAbsolutePath()
        } else {
            val defaultProfile = Paths.get("").resolve("default-profile.json").toAbsolutePath()

            if (!Files.exists(defaultProfile)) {
                validationResults.add("default client profile", "file not found", defaultProfile)
            }

            defaultProfile
        }
    }
}
