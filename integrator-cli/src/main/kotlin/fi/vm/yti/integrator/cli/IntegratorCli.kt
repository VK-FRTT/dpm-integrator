package fi.vm.yti.integrator.cli

import fi.vm.yti.integrator.dm.DataModelInfo
import fi.vm.yti.integrator.dm.DataModelVersionInfo
import java.io.BufferedWriter
import java.io.Closeable
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.io.PrintWriter
import java.nio.charset.Charset

const val INTEGRATOR_CLI_SUCCESS = 0
const val INTEGRATOR_CLI_FAIL = 1

class IntegratorCli(
    outStream: PrintStream,
    errStream: PrintStream,
    charset: Charset,
    private val definedOptions: DefinedOptions
) : Closeable {

    private val outWriter = PrintWriter(BufferedWriter(OutputStreamWriter(outStream, charset)), true)
    private val errWriter = PrintWriter(BufferedWriter(OutputStreamWriter(errStream, charset)), true)

    override fun close() {
        outWriter.close()
        errWriter.close()
    }

    fun execute(args: Array<String>): Int {
        return withExceptionHarness {
            val detectedOptions = definedOptions.detectOptionsFromArgs(args)

            if (detectedOptions.cmdShowHelp) {
                definedOptions.printHelp(outWriter)
                throwHalt()
            }

            detectedOptions.ensureSingleCommandGiven()

            if (detectedOptions.cmdUploadDatabase != null) {
                uploadDatabase(detectedOptions)
            }
        }
    }

    private fun withExceptionHarness(steps: () -> Unit): Int {
        return try {
            steps()
            INTEGRATOR_CLI_SUCCESS
        } catch (exception: HaltException) {
            INTEGRATOR_CLI_SUCCESS
        } catch (exception: FailException) {
            errWriter.println("${exception.message}")
            errWriter.println()

            INTEGRATOR_CLI_FAIL
        } catch (exception: Throwable) {
            errWriter.println("yti-brag-dm-integrator:")
            exception.printStackTrace(errWriter)
            errWriter.println()

            INTEGRATOR_CLI_FAIL
        }
    }

    private fun uploadDatabase(detectedOptions: DetectedOptions) {
        outWriter.println("Importing database to Atome Matter")
        outWriter.println("")

        detectedOptions.ensureOptionHasValue("targetDataModelName")
        detectedOptions.ensureOptionHasValue("clientProfile")

        val dmClient = DataModelerClient(
            detectedOptions.clientProfile!!,
            detectedOptions.verbosity,
            outWriter
        )

        outWriter.println("Authenticating: ${detectedOptions.username}")
        dmClient.authenticate(
            detectedOptions.username,
            detectedOptions.password
        )

        outWriter.println("Selecting target data model: ${detectedOptions.targetDataModelName}")
        val dataModels = dmClient.listDataModels()

        val targetDataModelVersion = selectTargetDataModelVersion(
            dataModels,
            detectedOptions.targetDataModelName!!
        )

        outWriter.println("Uploading database file: ${detectedOptions.cmdUploadDatabase!!}")
        val taskId = dmClient.uploadDatabaseAndScheduleImport(
            detectedOptions.cmdUploadDatabase,
            targetDataModelVersion.dataModelId
        )

        outWriter.print("Waiting import to complete")
        while (true) {
            outWriter.print("..")
            outWriter.flush()

            val taskStatus = dmClient.fetchTaskStatus(
                taskId,
                targetDataModelVersion.id
            )

            when (taskStatus.taskStatus) {

                "SCHEDULED", "IN_PROGRESS" -> {
                    Thread.sleep(2_500)
                }

                "FINISHED" -> {
                    outWriter.println("")
                    outWriter.println("Database import done")
                    throwHalt()
                }

                else -> {
                    outWriter.println("")
                    outWriter.println("Database import failed. Status message: ${taskStatus.taskStatus}")

                    throwHalt()
                }
            }
        }
    }

    private fun selectTargetDataModelVersion(
        dataModels: List<DataModelInfo>,
        targetDataModelName: String
    ): DataModelVersionInfo {
        val matchingModels = dataModels.filter { it.name == targetDataModelName }

        if (matchingModels.isEmpty()) {
            throwFail("Data model selection failed. No data model found for name: $targetDataModelName")
        }

        if (matchingModels.size >= 2) {
            throwFail("Data model selection failed. Multiple data models having same name: $targetDataModelName")
        }

        val targetModel = matchingModels.first()
        val versions = targetModel.dataModelVersions.sortedBy { it.creationDate }
        if (versions.isEmpty()) {
            throwFail("Data model selection failed. Data model does not have any version: $targetDataModelName")
        }

        val targetVersion = versions.last()

        if (targetModel.id != targetVersion.dataModelId) {
            throwFail("Data model selection failed. Data model ID and version ID do not match: $targetDataModelName")
        }

        return targetVersion
    }
}
