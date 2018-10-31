package fi.vm.yti.integrator.cli

import kotlin.reflect.KProperty0

@Suppress("MemberVisibilityCanBePrivate")
class ClientProfileInput(
    val userName: String?,
    val userPassword: String?,

    val clientId: String?,
    val clientSecret: String?,

    val authUrl: String?
) {
    fun toValidProfile(): ClientProfile {
        validateValueNotNull(this::userName)
        validateValueNotNull(this::userPassword)
        validateValueNotNull(this::clientId)
        validateValueNotNull(this::clientSecret)
        validateValueNotNull(this::authUrl)

        return ClientProfile(
            userName = userName!!,
            userPassword = userPassword!!,
            clientId = clientId!!,
            clientSecret = clientSecret!!,
            authUrl = authUrl!!
        )
    }

    private fun <T : Any?> validateValueNotNull(property: KProperty0<T>) {
        if (property.get() == null) {
            throwFail("ClientProfile: No value for '${property.name}'")
        }
    }
}
