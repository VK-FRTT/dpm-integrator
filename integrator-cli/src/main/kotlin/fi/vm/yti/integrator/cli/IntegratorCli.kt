package fi.vm.yti.integrator.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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

            if (detectedOptions.cmdUploadSqliteDb != null) {
                outWriter.println("Uploading database to BR-AG Data Modeler")

                detectedOptions.ensureOptionHasValue("targetDataModel")

                val profile =
                    jacksonObjectMapper().readValue<ClientProfileInput>(detectedOptions.clientProfile!!.toUri().toURL())
                        .toValidProfile()

                val dmClient = DataModelerClient(profile, detectedOptions.verbosity, outWriter)

                dmClient.login(detectedOptions.username, detectedOptions.password)
                val dataModels = dmClient.listDataModels()
                val targetDataModelVersion = selectImportTargetByDataModelName(dataModels, detectedOptions.targetDataModel!!)
                val taskId = dmClient.importDataModel(detectedOptions.cmdUploadSqliteDb!!, targetDataModelVersion.dataModelId)

                while (true) {
                    val taskStatus = dmClient.fetchTaskStatus(taskId, targetDataModelVersion.id)
                    if (taskStatus.taskStatus == "ERROR") {
                        throwFail("Data model import failed.\n$taskStatus")
                    }

                    Thread.sleep(5_000)
                }
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
            errWriter.println("yti-brag-dm-integrator: ${exception.message}")
            errWriter.println()

            INTEGRATOR_CLI_FAIL
        } catch (exception: Throwable) {
            errWriter.println("yti-brag-dm-integrator:")
            exception.printStackTrace(errWriter)
            errWriter.println()

            INTEGRATOR_CLI_FAIL
        }
    }

    private fun selectImportTargetByDataModelName(dataModels: List<DataModelInfo>, targetDataModelName: String): DataModelVersionInfo {
        val matchingModels = dataModels.filter { it.name == targetDataModelName }

        if (matchingModels.isEmpty()) {
            throwFail("No matching data model found with given name: $targetDataModelName")
        }

        if (matchingModels.size >= 2) {
            throwFail("Multiple matching data models found with given name: $targetDataModelName")
        }

        val targetModel = matchingModels.first()
        val versions = targetModel.dataModelVersions.sortedBy { it.creationDate }
        if (versions.isEmpty()) {
            throwFail("Data model does not have any version: $targetDataModelName")
        }

        val targetVersion = versions.last()

        if (targetModel.id != targetVersion.dataModelId) {
            throwFail("Data model ID and version ID does not match: $targetDataModelName")
        }

        return targetVersion
    }
}

