package fi.vm.yti.integrator.cli

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlin.reflect.KProperty0

@JsonIgnoreProperties(ignoreUnknown = true)
@Suppress("MemberVisibilityCanBePrivate")
class ClientProfileInput(
    val clientId: String?,
    val clientSecret: String?,
    val authApiUrl: String?,
    val hmrApiUrl: String?,
    val exportImportApiUrl: String?
) {
    fun toValidProfile(): ClientProfile {
        validateValueNotNull(this::clientId)
        validateValueNotNull(this::clientSecret)
        validateValueNotNull(this::authApiUrl)
        validateValueNotNull(this::hmrApiUrl)
        validateValueNotNull(this::exportImportApiUrl)

        return ClientProfile(
            clientId = clientId!!,
            clientSecret = clientSecret!!,
            authApiUrl = authApiUrl!!,
            hmrApiUrl = hmrApiUrl!!,
            exportImportApiUrl = exportImportApiUrl!!
        )
    }

    private fun <T : Any?> validateValueNotNull(property: KProperty0<T>) {
        if (property.get() == null) {
            throwFail("ClientProfile: No value for '${property.name}'")
        }
    }
}
