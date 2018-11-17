package fi.vm.yti.integrator.dm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaskStatusInfo(
    val type: String,
    val taskId: String,
    val taskStatus: String,
    val taskType: String,
    val resultDataModelId: String,
    val sourceDBFileName: String
)
