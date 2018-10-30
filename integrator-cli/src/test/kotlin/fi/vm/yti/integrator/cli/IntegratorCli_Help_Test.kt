package fi.vm.yti.integrator.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Command ´--help´")
internal class IntegratorCli_Help_Test : IntegratorCli_TestBase(
    primaryCommand = "--help"
) {

    @Test
    fun `Should list available command line options`() {
        val args = arrayOf("--help")
        val (status, outText, errText) = executeCli(args)

        assertThat(errText).isBlank()

        assertThat(outText).containsSubsequence(
            "--help"
        )

        assertThat(status).isEqualTo(INTEGRATOR_CLI_SUCCESS)
    }
}
