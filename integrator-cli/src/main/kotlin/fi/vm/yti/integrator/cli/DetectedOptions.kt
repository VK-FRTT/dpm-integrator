package fi.vm.yti.integrator.cli

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class ImportDatabaseParams(
    val databasePath: Path,
    val targetDataModelName: String,
    val dpmToolConfigPath: Path,
    val username: String,
    val password: String?,
    val verbosity: Verbosity
)

data class ListDataModelsParams(
    val dpmToolConfigPath: Path,
    val username: String,
    val password: String?,
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

        results.failOnErrors()

        return ImportDatabaseParams(
            databasePath = cmdImportDbToExistingModel,
            targetDataModelName = targetDataModelName!!,
            dpmToolConfigPath = resolvedToolConfigPath,
            username = username!!,
            password = password,
            verbosity = verbosity
        )
    }

    fun validListDataModelsParams(): ListDataModelsParams {
        require(cmdListDataModels)

        val validationResults = CommandValidationResults()

        val resolvedToolConfigPath = resolveValidDpmToolConfigPath(validationResults)
        requireNonNullOptionValue(username, OptName.USERNAME, validationResults)

        validationResults.failOnErrors()

        return ListDataModelsParams(
            dpmToolConfigPath = resolvedToolConfigPath,
            username = username!!,
            password = password,
            verbosity = verbosity
        )
    }

    private fun resolveValidDpmToolConfigPath(validationResults: CommandValidationResults): Path {
        if (dpmToolConfig != null) {
            if (!Files.exists(dpmToolConfig)) {

                validationResults.add(
                    OptName.DPM_TOOL_CONFIG.nameString,
                    "file not found",
                    dpmToolConfig
                )
            }
            return dpmToolConfig.toAbsolutePath()
        } else {
            val defaultFileName = "default-dpm-tool-config.json"

            val currentDirConfigPath = resolveCurrentDirConfig(defaultFileName)

            if (Files.exists(currentDirConfigPath)) {
                return currentDirConfigPath
            }

            val jarDirConfigPath = resolveJarDirConfig(defaultFileName)

            if (Files.exists(jarDirConfigPath)) {
                return jarDirConfigPath
            }

            validationResults.add(
                "DPM Tool default configuration",
                "file not found",
                listOf(currentDirConfigPath, jarDirConfigPath)
            )

            return Paths.get("")
        }
    }

    private fun resolveCurrentDirConfig(confFileName: String): Path {
        val currentDir = Paths.get("")
        return currentDir.resolve(confFileName).toAbsolutePath()
    }

    private fun resolveJarDirConfig(confFileName: String): Path {
        val jarPathComponent = this.javaClass.protectionDomain.codeSource.location.toURI().path
        jarPathComponent ?: throwFail("DPM Tool default configuration lookup failed: Null JAR path")

        val jarDirPath = Paths.get(jarPathComponent).parent
        jarDirPath ?: throwFail("DPM Tool default configuration lookup failed: Null JAR parent path")

        return jarDirPath.resolve(confFileName).toAbsolutePath()
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
