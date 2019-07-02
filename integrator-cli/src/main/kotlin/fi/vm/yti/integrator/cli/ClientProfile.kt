package fi.vm.yti.integrator.cli

data class ClientProfile(
    val clientId: String,
    val clientSecret: String,

    val authServiceHost: String,
    val hmrServiceHost: String,
    val exportImportServiceHost: String
)
