package fi.vm.yti.integrator.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.BufferedWriter
import java.io.Closeable
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.io.PrintWriter
import java.nio.charset.Charset
import java.nio.file.Path

const val INTEGRATOR_CLI_SUCCESS = 0
const val INTEGRATOR_CLI_FAIL = 1
const val INTEGRATOR_TITLE = "DPM Tool Integrator"

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
            outWriter.println(INTEGRATOR_TITLE)

            val detectedOptions = definedOptions.detectOptionsFromArgs(args)

            if (detectedOptions.cmdShowHelp) {
                definedOptions.printHelp(outWriter)
                throwHalt()
            }

            detectedOptions.ensureSingleCommandGiven()

            if (detectedOptions.cmdListDataModels) {
                listDataModels(detectedOptions)
                throwHalt()
            }

            if (detectedOptions.cmdImportDbToExistingModel != null) {
                importDbToExistingModel(detectedOptions)
                throwHalt()
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
            errWriter.println("\n ${exception.message}")
            errWriter.println()

            INTEGRATOR_CLI_FAIL
        } catch (exception: Throwable) {
            errWriter.println(INTEGRATOR_TITLE)
            exception.printStackTrace(errWriter)
            errWriter.println()

            INTEGRATOR_CLI_FAIL
        }
    }

    private fun listDataModels(detectedOptions: DetectedOptions) {
        val listParams = detectedOptions.validListDataModelsParams()
        val toolConfig = loadToolConfig(listParams.dpmToolConfigPath)

        outWriter.println("Listing data models from: ${toolConfig.dpmToolName}")
        outWriter.println("")

        val client = DpmToolClient(
            toolConfig,
            listParams.verbosity,
            outWriter
        )

        outWriter.println("Authenticating: ${listParams.username}")
        client.authenticate(
            listParams.username,
            listParams.password
        )

        outWriter.println("Retrieving data models list")
        val dataModels = client.listDataModels()

        outWriter.println("Data models:")
        if (dataModels.isEmpty()) {
            outWriter.println("- None")
        } else {
            dataModels.forEach { modelInfo ->
                outWriter.println("- ${modelInfo.name}")
            }
        }
    }

    private fun importDbToExistingModel(detectedOptions: DetectedOptions) {
        val importParams = detectedOptions.validImportDatabaseParams()
        val toolConfig = loadToolConfig(importParams.dpmToolConfigPath)

        outWriter.println("Importing database to: ${toolConfig.dpmToolName}")
        outWriter.println("")

        val client = DpmToolClient(
            toolConfig,
            importParams.verbosity,
            outWriter
        )

        outWriter.println("Authenticating: ${importParams.username}")
        client.authenticate(
            importParams.username,
            importParams.password
        )

        outWriter.println("Selecting target data model: ${importParams.targetDataModelName}")
        val targetDataModelVersion = client.selectTargetDataModelVersion(
            importParams.targetDataModelName
        )

        outWriter.println("Uploading database file: ${importParams.databasePath}")
        val taskId = client.uploadDatabaseAndScheduleImport(
            importParams.databasePath,
            targetDataModelVersion.dataModelId
        )

        outWriter.print("Waiting import to complete")
        while (true) {
            outWriter.print("..")
            outWriter.flush()

            val taskStatus = client.fetchTaskStatus(
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

    private fun loadToolConfig(toolConfigPath: Path): DpmToolConfig {
        val mapper = jacksonObjectMapper()

        val input = mapper.readValue<DpmToolConfigInput>(toolConfigPath.toUri().toURL())

        return input.toValidConfig()
    }
}
