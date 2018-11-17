package fi.vm.yti.integrator.cli

data class ClientProfile(
    val clientId: String,
    val clientSecret: String,

    val authApiUrl: String,
    val hmrApiUrl: String,
    val exportImportApiUrl: String
)
