package fi.vm.yti.integrator.cli.config

data class DpmToolConfig(
    val dpmToolName: String,
    val clientAuthBasic: ClientAuthBasic,
    val serviceAddress: ServiceAddress
)
