package fi.vm.yti.integrator.cli

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

            if (detectedOptions.cmdListDataModels) {
                listDataModels(detectedOptions)
                throwHalt()
            }

            if (detectedOptions.cmdUploadDatabase != null) {
                uploadDatabase(detectedOptions)
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

    private fun listDataModels(detectedOptions: DetectedOptions) {
        outWriter.println("Listing data models from to Atome Matter")
        outWriter.println("")

        val listParams = detectedOptions.validListDataModelsParams()

        val dmClient = DataModelerClient(
            listParams.clientProfile,
            listParams.verbosity,
            outWriter
        )

        outWriter.println("Authenticating: ${listParams.username}")
        dmClient.authenticate(
            listParams.username,
            listParams.password
        )

        outWriter.println("Retrieving data models list")
        val dataModels = dmClient.listDataModels()

        outWriter.println("Data models:")
        if (dataModels.isEmpty()) {
            outWriter.println("- None")
        } else {
            dataModels.forEach { modelInfo ->
                outWriter.println("- ${modelInfo.name}")
            }
        }
    }

    private fun uploadDatabase(detectedOptions: DetectedOptions) {
        outWriter.println("Importing database to Atome Matter")
        outWriter.println("")

        val uploadParams = detectedOptions.validUploadDatabaseParams()

        val dmClient = DataModelerClient(
            uploadParams.clientProfile,
            uploadParams.verbosity,
            outWriter
        )

        outWriter.println("Authenticating: ${uploadParams.username}")
        dmClient.authenticate(
            uploadParams.username,
            uploadParams.password
        )

        outWriter.println("Selecting target data model: ${uploadParams.targetDataModelName}")
        val targetDataModelVersion = dmClient.selectTargetDataModelVersion(
            uploadParams.targetDataModelName
        )

        outWriter.println("Uploading database file: ${uploadParams.databasePath}")
        val taskId = dmClient.uploadDatabaseAndScheduleImport(
            uploadParams.databasePath,
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
}
