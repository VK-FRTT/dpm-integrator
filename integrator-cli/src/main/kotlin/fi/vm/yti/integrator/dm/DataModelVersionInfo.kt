package fi.vm.yti.integrator.dm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataModelVersionInfo(
    val type: String,
    val id: String,
    val creationDate: String,
    val dataModelId: String,
    val name: String
)
