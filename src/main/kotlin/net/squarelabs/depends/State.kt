package net.squarelabs.depends

import net.squarelabs.depends.models.Artifact

data class State(
        val artifactsByGa: MutableMap<String, MutableMap<String, Artifact>> = mutableMapOf(),
        val artifactsByMethod: MutableMap<String, MutableSet<String>> = mutableMapOf()
)