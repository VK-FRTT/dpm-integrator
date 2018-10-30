package fi.vm.yti.integrator.cli

import java.nio.charset.Charset
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val status = IntegratorCli(
        System.out,
        System.err,
        Charset.defaultCharset(),
        DefinedOptions()
    ).use { cli ->
        cli.execute(args)
    }

    exitProcess(status)
}
