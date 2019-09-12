package fi.vm.yti.integrator.cli.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.vm.yti.integrator.cli.throwFail
import kotlin.reflect.KProperty0

@JsonIgnoreProperties(ignoreUnknown = true)
@Suppress("MemberVisibilityCanBePrivate")
class ClientAuthBasicInput(
    val username: String?,
    val password: String?
) {
    fun toValidConfig(): ClientAuthBasic {
        validateValueNotNull(this::username)
        validateValueNotNull(this::password)

        return ClientAuthBasic(
            username = username!!,
            password = password!!
        )
    }

    private fun <T : Any?> validateValueNotNull(property: KProperty0<T>) {
        if (property.get() == null) {
            throwFail("DpmToolConfig.ClientAuthBasic: No value for '${property.name}'")
        }
    }
}
