package net.squarelabs.depends.models

data class Invocation(
        val fqcn: String,
        val methodName: String,
        val descriptor: String
)