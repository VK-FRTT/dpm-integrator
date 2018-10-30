package fi.vm.yti.integrator.cli

import java.nio.file.Path

data class DetectedOptions(
    val cmdShowHelp: Boolean,
    val cmdUploadSqliteDb: Path?,
    val clientProfile: Path?
) {

    fun ensureSingleCommandGiven() {
        val commandCount = listOf<Any?>(
            cmdUploadSqliteDb
        ).count { it != null }

        if (commandCount != 1) {
            throwFail("Single command with proper argument must be given")
        }
    }
}
