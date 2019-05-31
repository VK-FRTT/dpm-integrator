package fi.vm.yti.integrator.cli

import fi.vm.yti.integrator.ext.kotlin.getPropertyValue
import java.nio.file.Path

data class DetectedOptions(
    val cmdShowHelp: Boolean,
    val cmdUploadDatabase: Path?,
    val targetDataModelName: String?,
    val clientProfile: Path?,
    val verbosity: Verbosity,
    val username: String,
    val password: String
) {

    fun ensureSingleCommandGiven() {
        val commandCount = listOf<Any?>(
            cmdUploadDatabase
        ).count { it != null }

        if (commandCount != 1) {
            throwFail("Single command with proper argument must be given")
        }
    }

    fun ensureOptionHasValue(propertyName: String) {
        val value = getPropertyValue(propertyName)

        if (value == null) {
            throwFail("No value given for option: $propertyName")
        }
    }
}
