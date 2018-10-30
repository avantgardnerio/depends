package net.squarelabs.depends.models

import java.io.File

data class Artifact(
        val coordinate: String,
        val file: File,
        val dependencies: List<Artifact>,
        val classes: List<Class>
)