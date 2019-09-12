package fi.vm.yti.integrator.cli

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class ImportDatabaseParams(
    val databasePath: Path,
    val targetDataModelName: String,
    val dpmToolConfigPath: Path,
    val username: String,
    val password: String,
    val verbosity: Verbosity
)

data class ListDataModelsParams(
    val dpmToolConfigPath: Path,
    val username: String,
    val password: String,
    val verbosity: Verbosity
)

data class DetectedOptions(
    val cmdShowHelp: Boolean,
    val cmdShowVersion: Boolean,
    val cmdListDataModels: Boolean,
    val cmdImportDbToExistingModel: Path?,
    val targetDataModelName: String?,
    val dpmToolConfig: Path?,
    val username: String?,
    val password: String?,
    val verbosity: Verbosity
) {

    fun ensureSingleCommandGiven() {
        val commandCount = listOf<Any?>(
            cmdListDataModels,
            cmdImportDbToExistingModel
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

    fun validImportDatabaseParams(): ImportDatabaseParams {
        requireNotNull(cmdImportDbToExistingModel)

        val results = CommandValidationResults()

        val resolvedToolConfigPath = resolveValidDpmToolConfigPath(results)
        requireFileExists(cmdImportDbToExistingModel, OptName.IMPORT_DB_TO_EXISTING, results)
        requireNonNullOptionValue(targetDataModelName, OptName.TARGET_DATA_MODEL_NAME, results)
        requireNonNullOptionValue(username, OptName.USERNAME, results)
        requireNonNullOptionValue(password, OptName.PASSWORD, results)

        results.failOnErrors()

        return ImportDatabaseParams(
            databasePath = cmdImportDbToExistingModel,
            targetDataModelName = targetDataModelName!!,
            dpmToolConfigPath = resolvedToolConfigPath,
            username = username!!,
            password = password!!,
            verbosity = verbosity
        )
    }

    fun validListDataModelsParams(): ListDataModelsParams {
        require(cmdListDataModels)

        val validationResults = CommandValidationResults()

        val resolvedToolConfigPath = resolveValidDpmToolConfigPath(validationResults)
        requireNonNullOptionValue(username, OptName.USERNAME, validationResults)
        requireNonNullOptionValue(password, OptName.PASSWORD, validationResults)

        validationResults.failOnErrors()

        return ListDataModelsParams(
            dpmToolConfigPath = resolvedToolConfigPath,
            username = username!!,
            password = password!!,
            verbosity = verbosity
        )
    }

    private fun resolveValidDpmToolConfigPath(validationResults: CommandValidationResults): Path {
        return if (dpmToolConfig != null) {
            if (!Files.exists(dpmToolConfig)) {

                validationResults.add(
                    OptName.DPM_TOOL_CONFIG.nameString,
                    "file not found",
                    dpmToolConfig
                )
            }
            dpmToolConfig.toAbsolutePath()
        } else {
            val defaulConfig = Paths.get("").resolve("default-dpm-tool-config.json").toAbsolutePath()

            if (!Files.exists(defaulConfig)) {
                validationResults.add(
                    "${OptName.DPM_TOOL_CONFIG.nameString} (default configuration)",
                    "file not found",
                    defaulConfig
                )
            }

            defaulConfig
        }
    }

    private fun requireNonNullOptionValue(
        optionValue: Any?,
        optName: OptName,
        validationResults: CommandValidationResults
    ) {
        optionValue
            ?: validationResults.add(optName.nameString, "missing required parameter value")
    }

    private fun requireFileExists(
        filePath: Path,
        optName: OptName,
        validationResults: CommandValidationResults
    ) {
        if (!Files.exists(filePath)) {
            validationResults.add(optName.nameString, "file not found ($filePath)")
        }
    }
}
