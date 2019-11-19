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

enum class OptName(val nameString: String) {

    LIST_DATA_MODELS("list-data-models"),
    IMPORT_DB_TO_EXISTING("import-db-to-existing-model"),
    TARGET_DATA_MODEL_NAME("target-data-model"),
    DPM_TOOL_CONFIG("dpm-tool-config"),
    USERNAME("username"),
    PASSWORD("password"),
    VERBOSE("verbose")
}

class DefinedOptions {
    private val optionParser = OptionParser()

    private val cmdShowHelp: OptionSpec<Void>
    private val cmdShowVersion: OptionSpec<Void>
    private val cmdListDataModels: OptionSpec<Void>
    private val cmdImportDbToExistingModel: OptionSpec<Path>
    private val targetDataModelName: OptionSpec<String>
    private val dpmToolConfig: OptionSpec<Path>
    private val verbosity: OptionSpec<Verbosity>
    private val username: OptionSpec<String>
    private val password: OptionSpec<String>

    init {
        cmdShowHelp = optionParser
            .accepts(
                "help",
                "show this help message"
            ).forHelp()

        cmdShowVersion = optionParser
            .accepts(
                "version",
                "show version information"
            )

        cmdListDataModels = optionParser
            .accepts(
                OptName.LIST_DATA_MODELS.nameString,
                "list data models from the DPM Tool"
            )

        cmdImportDbToExistingModel = optionParser
            .accepts(
                OptName.IMPORT_DB_TO_EXISTING.nameString,
                "import SQlite Database to existing data model on the DPM Tool"
            )
            .withOptionalArg()
            .withValuesConvertedBy(PathConverter())

        targetDataModelName = optionParser
            .accepts(
                OptName.TARGET_DATA_MODEL_NAME.nameString,
                "target data model name"
            )
            .withOptionalArg()

        dpmToolConfig = optionParser
            .accepts(
                OptName.DPM_TOOL_CONFIG.nameString,
                "configuration file describing the DPM Tool address and similar details"
            )
            .withOptionalArg()
            .withValuesConvertedBy(PathConverter())

        username = optionParser
            .accepts(
                OptName.USERNAME.nameString,
                "username for authenticating to the DPM Tool"
            )
            .withOptionalArg()

        password = optionParser
            .accepts(
                OptName.PASSWORD.nameString,
                "password for authenticating to the DPM Tool (optional, password will be asked later if not given as option)"
            )
            .withOptionalArg()

        verbosity = optionParser
            .accepts(
                OptName.VERBOSE.nameString,
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
            cmdShowVersion = optionSet.has(cmdShowVersion),
            cmdListDataModels = optionSet.has(cmdListDataModels),
            cmdImportDbToExistingModel = optionSet.valueOf(cmdImportDbToExistingModel),
            targetDataModelName = optionSet.valueOf(targetDataModelName),
            dpmToolConfig = optionSet.valueOf(dpmToolConfig),
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
