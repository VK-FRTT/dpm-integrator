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

const val TARGET_DATA_MODEL_OPTION_NAME = "target-data-model"
const val CLIENT_PROFILE_OPTION_NAME = "client-profile"
const val USERNAME_OPTION_NAME = "username"
const val PASSWORD_OPTION_NAME = "password"

class DefinedOptions {
    private val optionParser = OptionParser()

    private val cmdShowHelp: OptionSpec<Void>
    private val cmdListDataModels: OptionSpec<Void>
    private val cmdUploadDatabase: OptionSpec<Path>
    private val targetDataModel: OptionSpec<String>
    private val clientProfile: OptionSpec<Path>
    private val verbosity: OptionSpec<Verbosity>
    private val username: OptionSpec<String>
    private val password: OptionSpec<String>

    init {
        cmdShowHelp = optionParser
            .accepts(
                "help",
                "show this help message"
            ).forHelp()

        cmdListDataModels = optionParser
            .accepts(
                "list-data-models",
                "list data models existing in Atome Matter"
            )

        cmdUploadDatabase = optionParser
            .accepts(
                "upload-database",
                "upload database to Atome Matter"
            )
            .withOptionalArg()
            .withValuesConvertedBy(PathConverter())

        targetDataModel = optionParser
            .accepts(
                TARGET_DATA_MODEL_OPTION_NAME,
                "target data model name"
            )
            .withOptionalArg()

        clientProfile = optionParser
            .accepts(
                CLIENT_PROFILE_OPTION_NAME,
                "client profile describing Atome Matter service address etc details"
            )
            .withOptionalArg()
            .withValuesConvertedBy(PathConverter())

        username = optionParser
            .accepts(
                USERNAME_OPTION_NAME,
                "Data Modeler username"
            )
            .withOptionalArg()

        password = optionParser
            .accepts(
                PASSWORD_OPTION_NAME,
                "Data Modeler user password"
            )
            .withOptionalArg()

        verbosity = optionParser
            .accepts(
                "verbose",
                "verbose mode ${Verbosity.INFO}, ${Verbosity.DEBUG}, ${Verbosity.TRACE}"
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
            cmdListDataModels = optionSet.has(cmdListDataModels),
            cmdUploadDatabase = optionSet.valueOf(cmdUploadDatabase),
            targetDataModelName = optionSet.valueOf(targetDataModel),
            clientProfile = optionSet.valueOf(clientProfile),
            verbosity = optionSet.valueOf(verbosity),
            username = optionSet.valueOf(username),
            password = optionSet.valueOf(password)
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
