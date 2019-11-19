package fi.vm.yti.integrator.cli

import java.io.File
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
            val defaultFileName = "dpm-integrator-config.json"

            val currentDirConfigPath = resolveCurrentDirConfigPath(defaultFileName)

            if (Files.exists(currentDirConfigPath)) {
                return currentDirConfigPath
            }

            val jarDirConfigPath = tryResolveJarDirConfigPath(defaultFileName)

            if (jarDirConfigPath != null && Files.exists(jarDirConfigPath)) {
                return jarDirConfigPath
            }

            validationResults.add(
                "DPM Tool default configuration",
                "file not found",
                listOfNotNull(currentDirConfigPath, jarDirConfigPath)
            )

            return Paths.get("")
        }
    }

    private fun resolveCurrentDirConfigPath(confFileName: String): Path {
        val currentDir = Paths.get("")
        return currentDir.resolve(confFileName).toAbsolutePath()
    }

    private val jarPathPattern =
        """
            \A
            jar:file:
            (?<jarPath>[^!]+)
            !.+
            \z
        """.trimIndent().toRegex(RegexOption.COMMENTS)

    private fun tryResolveJarDirConfigPath(confFileName: String): Path? {
        val selfResourceName = "/${javaClass.name.replace('.', '/')}.class"
        val selfResourceUrl = javaClass.getResource(selfResourceName)

        val jarPathMatch = jarPathPattern.matchEntire(selfResourceUrl.toString())
        jarPathMatch ?: return null

        val rawJarPath = (jarPathMatch.groups as MatchNamedGroupCollection)["jarPath"]?.value
        rawJarPath ?: return null

        val normalizedJarPath = File(rawJarPath).toPath().toAbsolutePath()

        val jarDirPath = normalizedJarPath.parent

        return jarDirPath.resolve(confFileName)
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
