package fi.vm.yti.integrator.cli

data class ClientProfile(
    val userName: String,
    val userPassword: String,

    val clientId: String,
    val clientSecret: String,

    val authUrl: String
)
