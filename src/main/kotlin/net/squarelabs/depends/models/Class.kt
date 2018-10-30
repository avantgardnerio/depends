package net.squarelabs.depends.models

data class Class(
        val name: String,
        val methods: Map<String,Method>
)