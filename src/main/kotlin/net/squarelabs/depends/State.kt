package net.squarelabs.depends

import net.squarelabs.depends.models.Artifact

data class State(
        val artifactsByGa: HashMap<String, HashMap<String, Artifact>> = HashMap(),
        val artifactsByMethod: HashMap<String, HashSet<String>> = HashMap()
)