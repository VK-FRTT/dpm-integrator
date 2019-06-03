package fi.vm.yti.integrator.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Command ´--upload-database´")
internal class IntegratorCli_UploadDatabase_Test : IntegratorCli_TestBase(
    primaryCommand = "--upload-database"
) {

    @Test
    fun `Should error when required options are not given`() {
        val args = arrayOf("--upload-database", "some.db")
        executeCliAndExpectFail(args) { outText, errText ->

            assertThat(outText).containsSubsequence(
                "Importing database to Atome Matter"
            )

            assertThat(errText).contains(
                "Error:",
                "- target-data-model: missing required parameter value",
                "- username: missing required parameter value",
                "- password: missing required parameter value",
                "- default client profile: file not found (",
                "default-profile.json)"
            )
        }
    }
}
