package fi.vm.yti.integrator.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset

open class IntegratorCli_TestBase(val primaryCommand: String? = null) {

    private lateinit var charset: Charset
    private lateinit var outCollector: PrintStreamCollector
    private lateinit var errCollector: PrintStreamCollector

    private lateinit var cli: IntegratorCli

    @BeforeEach
    fun baseInit() {

        cli = IntegratorCli(
            outStream = outCollector.printStream(),
            errStream = errCollector.printStream(),
            charset = charset,
            definedOptions = DefinedOptions()
        )
    }

    @AfterEach
    fun baseTeardown() {
    }


    protected fun executeCli(args: Array<String>): ExecuteResult {
        if (primaryCommand != null) {
            assertThat(args).contains(primaryCommand)
        }

        val status = cli.execute(args)

        val result = ExecuteResult(
            status,
            outCollector.grabText(),
            errCollector.grabText()
        )

        //println("OUT >>>\n${result.outText}\n<<< OUT")
        //println("ERR >>>\n${result.errText}\n<<< ERR")

        return result
    }

    private class PrintStreamCollector(val charset: Charset) {
        private val baos = ByteArrayOutputStream()
        private val ps = PrintStream(baos, true, charset.name())

        fun printStream(): PrintStream = ps

        fun grabText(): String {
            ps.close()
            return String(baos.toByteArray(), charset)
        }
    }

    protected data class ExecuteResult(
        val status: Int,
        val outText: String,
        val errText: String
    )
}
