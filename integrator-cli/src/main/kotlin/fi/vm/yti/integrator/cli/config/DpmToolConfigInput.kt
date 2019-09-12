package fi.vm.yti.integrator.cli.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.vm.yti.integrator.cli.throwFail
import kotlin.reflect.KProperty0

@JsonIgnoreProperties(ignoreUnknown = true)
@Suppress("MemberVisibilityCanBePrivate")
class DpmToolConfigInput(
    val dpmToolName: String?,
    val clientAuthBasic: ClientAuthBasicInput?,
    val serviceAddress: ServiceAddressInput?
) {
    fun toValidConfig(): DpmToolConfig {
        validateValueNotNull(this::dpmToolName)
        validateValueNotNull(this::clientAuthBasic)
        validateValueNotNull(this::serviceAddress)

        return DpmToolConfig(
            dpmToolName = dpmToolName!!,
            clientAuthBasic = clientAuthBasic!!.toValidConfig(),
            serviceAddress = serviceAddress!!.toValidConfig()
        )
    }

    private fun <T : Any?> validateValueNotNull(property: KProperty0<T>) {
        if (property.get() == null) {
            throwFail("DpmToolConfig: No value for '${property.name}'")
        }
    }
}
