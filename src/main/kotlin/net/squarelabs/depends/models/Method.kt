package net.squarelabs.depends.models

data class Method(
        val name: String,
        val invocations: List<Method>
)