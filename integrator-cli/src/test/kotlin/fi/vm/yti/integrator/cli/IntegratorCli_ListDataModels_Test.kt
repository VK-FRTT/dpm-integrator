package fi.vm.yti.integrator.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Command ´--list-data-models´")
internal class IntegratorCli_ListDataModels_Test : IntegratorCli_TestBase(
    primaryCommand = "--list-data-models"
) {

    @Test
    fun `Should error when required options are not given`() {
        val args = arrayOf("--list-data-models")
        executeCliAndExpectFail(args) { outText, errText ->

            assertThat(outText).containsSubsequence(
                "DPM Tool Integrator"
            )

            assertThat(errText).contains(
                "Error:",
                "- username: missing required parameter value",
                "- DPM Tool default configuration: file not found", "dpm-integrator-config.json"
            )
        }
    }
}
