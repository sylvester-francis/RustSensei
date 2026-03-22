package com.sylvester.rustsensei.content

data class OwnershipScenario(
    val id: String,
    val title: String,
    val description: String,
    val relatedChapter: String,
    val steps: List<VisualizationStep>
)

data class VisualizationStep(
    val label: String,
    val code: String,
    val stackVariables: List<StackVariable>,
    val heapAllocations: List<HeapAllocation>,
    val annotation: String
)

data class StackVariable(
    val name: String,
    val type: String,
    val status: VariableStatus,
    val pointsTo: String? = null
)

data class HeapAllocation(
    val id: String,
    val value: String,
    val type: String,
    val status: AllocationStatus
)

enum class VariableStatus { ACTIVE, MOVED, DROPPED, BORROWED }
enum class AllocationStatus { ALIVE, FREED }
