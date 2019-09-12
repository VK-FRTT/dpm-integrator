package fi.vm.yti.integrator.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Command ´--version´")
internal class IntegratorCli_Version_Test : IntegratorCli_TestBase(
    primaryCommand = "--version"
) {

    @Test
    fun `Should show version info`() {
        val args = arrayOf("--version")

        executeCliAndExpectSuccess(args) { outText ->

            assertThat(outText).containsSubsequence(
                "DPM Tool Integrator",
                "Version:      0.0.0-DEV",
                "Build time:   -",
                "Revision:     -"
            )
        }
    }
}
