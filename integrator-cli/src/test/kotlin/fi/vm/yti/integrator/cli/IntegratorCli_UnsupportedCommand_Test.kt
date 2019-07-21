package fi.vm.yti.integrator.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Command ´--unknown-option´")
internal class IntegratorCli_UnsupportedCommand_Test : IntegratorCli_TestBase(
    primaryCommand = "--unknown-option"
) {

    @Test
    fun `Should error when given option is unknown`() {
        val args = arrayOf("--unknown-option")
        val (status, outText, errText) = executeCli(args)

        assertThat(errText).containsSubsequence(
            "unknown-option is not a recognized option"
        )

        assertThat(outText).containsSubsequence(
            "DPM Tool Integrator"
        )

        assertThat(status).isEqualTo(INTEGRATOR_CLI_FAIL)
    }
}
