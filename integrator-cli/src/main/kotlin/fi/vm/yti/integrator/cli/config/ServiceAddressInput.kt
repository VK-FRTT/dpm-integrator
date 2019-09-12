package fi.vm.yti.integrator.cli.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.vm.yti.integrator.cli.throwFail
import kotlin.reflect.KProperty0

@JsonIgnoreProperties(ignoreUnknown = true)
@Suppress("MemberVisibilityCanBePrivate")
class ServiceAddressInput(
    val authServiceHost: String?,
    val hmrServiceHost: String?,
    val exportImportServiceHost: String?
) {
    fun toValidConfig(): ServiceAddress {
        validateValueNotNull(this::authServiceHost)
        validateValueNotNull(this::hmrServiceHost)
        validateValueNotNull(this::exportImportServiceHost)

        return ServiceAddress(
            authServiceHost = authServiceHost!!,
            hmrServiceHost = hmrServiceHost!!,
            exportImportServiceHost = exportImportServiceHost!!
        )
    }

    private fun <T : Any?> validateValueNotNull(property: KProperty0<T>) {
        if (property.get() == null) {
            throwFail("DpmToolConfig.ServiceAddress: No value for '${property.name}'")
        }
    }
}
