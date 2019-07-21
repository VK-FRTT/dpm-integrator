package fi.vm.yti.integrator.cli

data class DpmToolConfig(
    val dpmToolName: String,

    val clientId: String,
    val clientSecret: String,

    val authServiceHost: String,
    val hmrServiceHost: String,
    val exportImportServiceHost: String
)
