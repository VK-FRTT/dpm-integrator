package fi.vm.yti.integrator.apimodel

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataModelInfo(
    val type: String,
    val id: String,
    val name: String,
    val dataModelVersions: List<DataModelVersionInfo>
)
