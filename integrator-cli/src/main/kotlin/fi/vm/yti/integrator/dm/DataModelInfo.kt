package fi.vm.yti.integrator.dm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataModelInfo(
    val type: String,
    val id: String,
    val name: String,
    val dataModelVersions: List<DataModelVersionInfo>
)
