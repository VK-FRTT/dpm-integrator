package fi.vm.yti.integrator.cli

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlin.reflect.KProperty0

@JsonIgnoreProperties(ignoreUnknown = true)
@Suppress("MemberVisibilityCanBePrivate")
class ClientProfileInput(
    val clientId: String?,
    val clientSecret: String?,
    val authServiceHost: String?,
    val hmrServiceHost: String?,
    val exportImportServiceHost: String?
) {
    fun toValidProfile(): ClientProfile {
        validateValueNotNull(this::clientId)
        validateValueNotNull(this::clientSecret)
        validateValueNotNull(this::authServiceHost)
        validateValueNotNull(this::hmrServiceHost)
        validateValueNotNull(this::exportImportServiceHost)

        return ClientProfile(
            clientId = clientId!!,
            clientSecret = clientSecret!!,
            authServiceHost = authServiceHost!!,
            hmrServiceHost = hmrServiceHost!!,
            exportImportServiceHost = exportImportServiceHost!!
        )
    }

    private fun <T : Any?> validateValueNotNull(property: KProperty0<T>) {
        if (property.get() == null) {
            throwFail("ClientProfile: No value for '${property.name}'")
        }
    }
}
