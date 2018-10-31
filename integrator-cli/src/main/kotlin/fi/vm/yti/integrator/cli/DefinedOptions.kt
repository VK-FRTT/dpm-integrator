package fi.vm.yti.integrator.cli

import joptsimple.BuiltinHelpFormatter
import joptsimple.OptionDescriptor
import joptsimple.OptionException
import joptsimple.OptionParser
import joptsimple.OptionSpec
import joptsimple.ValueConversionException
import joptsimple.util.EnumConverter
import joptsimple.util.PathConverter
import java.io.PrintWriter
import java.nio.file.Path
import java.util.LinkedHashSet

class DefinedOptions {
    private val optionParser = OptionParser()

    private val cmdShowHelp: OptionSpec<Void>
    private val cmdUploadSqliteDb: OptionSpec<Path>
    private val clientProfile: OptionSpec<Path>
    private val verbosity: OptionSpec<Verbosity>

    init {
        cmdShowHelp = optionParser
            .accepts(
                "help",
                "show this help message"
            ).forHelp()

        cmdUploadSqliteDb = optionParser
            .accepts(
                "upload-sqlite-db",
                "upload SQLite DB to DataModeler"
            )
            .withOptionalArg()
            .withValuesConvertedBy(PathConverter())

        clientProfile = optionParser
            .accepts(
                "client-profile",
                "client profile describing DataModeler service address etc details"
            )
            .withOptionalArg()
            .withValuesConvertedBy(PathConverter())

        verbosity = optionParser
            .accepts(
                "verbose",
                "verbose mode [${Verbosity.INFO}, ${Verbosity.DEBUG}]"
            )
            .withOptionalArg()
            .withValuesConvertedBy(VerbosityConverter())
            .defaultsTo(Verbosity.NONE)
    }

    fun detectOptionsFromArgs(args: Array<String>): DetectedOptions {
        return try {
            doDetectOptions(args)
        } catch (exception: OptionException) {
            val cause = exception.cause

            if (cause is ValueConversionException) {
                throwFail("Option ${exception.options().first()}: ${cause.message}")
            } else {
                throwFail("${exception.message}")
            }
        }
    }


    fun printHelp(outWriter: PrintWriter) {
        optionParser.formatHelpWith(FixedOrderHelpFormatter())
        optionParser.printHelpOn(outWriter)
    }

    private fun doDetectOptions(args: Array<String>): DetectedOptions {
        val optionSet = optionParser.parse(*args)

        if (!optionSet.hasOptions()) {
            throwFail("No options given (-h will show valid options)")
        }

        return DetectedOptions(
            cmdShowHelp = optionSet.has(cmdShowHelp),
            cmdUploadSqliteDb = optionSet.valueOf(cmdUploadSqliteDb),
            clientProfile = optionSet.valueOf(clientProfile),
            verbosity = optionSet.valueOf(verbosity)
        )
    }

    private class VerbosityConverter : EnumConverter<Verbosity>(Verbosity::class.java)

    private class FixedOrderHelpFormatter :
        BuiltinHelpFormatter(120, 4) {

        override fun format(options: Map<String, OptionDescriptor>): String {
            addRows(LinkedHashSet(options.values))
            return formattedHelpOutput()
        }
    }
}
