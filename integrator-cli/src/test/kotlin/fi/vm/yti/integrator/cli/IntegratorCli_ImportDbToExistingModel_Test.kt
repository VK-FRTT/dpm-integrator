package fi.vm.yti.integrator.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Command ´--import-db-to-existing-model´")
internal class IntegratorCli_ImportDbToExistingModel_Test : IntegratorCli_TestBase(
    primaryCommand = "--import-db-to-existing-model"
) {

    @Test
    fun `Should error when required options are not given`() {
        val args = arrayOf("--import-db-to-existing-model", "some.db")
        executeCliAndExpectFail(args) { outText, errText ->

            assertThat(outText).containsSubsequence(
                "DPM Tool Integrator"
            )

            assertThat(errText).contains(
                "Error:",
                "- target-data-model: missing required parameter value",
                "- username: missing required parameter value",
                "- dpm-tool-config (default configuration): file not found (", "default-dpm-tool-config.json)"
            )
        }
    }
}
