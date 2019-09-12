package fi.vm.yti.integrator.cli.config

data class ServiceAddress(
    val authServiceHost: String,
    val hmrServiceHost: String,
    val exportImportServiceHost: String
)
