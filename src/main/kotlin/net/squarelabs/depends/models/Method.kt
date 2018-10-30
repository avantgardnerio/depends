package net.squarelabs.depends.models

data class Method(
        val name: String,
        val descriptor: String,
        val invocations: List<Invocation>
)