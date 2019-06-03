package fi.vm.yti.integrator.cli

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KProperty1

data class DetectedOptions(
    val cmdShowHelp: Boolean,
    val cmdListDataModels: Boolean,
    val cmdUploadDatabase: Path?,
    val targetDataModelName: String?,
    val clientProfile: Path?,
    val verbosity: Verbosity,
    val username: String?,
    val password: String?
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

    fun <I : Any, P : Any?> validateOptionIsNonNull(
        property: KProperty1<I, P>,
        validationResults: CommandValidationResults
    ) {
        property.getter.call(this)
            ?: validationResults.add("${property.name}", "missing required parameter value")
    }

    fun selectValidClientProfile(validationResults: CommandValidationResults): Path {
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
