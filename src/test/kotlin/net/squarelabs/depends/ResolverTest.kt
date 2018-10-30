package net.squarelabs.depends

import net.squarelabs.depends.models.Artifact
import org.junit.Test

class ResolverTest {
    @Test
    fun `resolver should resolve`() {
        val state = State()
        val root: Artifact = resolve("io.dropwizard:dropwizard-core:1.3.7", state)
        val conflicts = state.artifactsByGa.keys
                .filter { key -> state.artifactsByGa[key]!!.size > 1 }
                .map { key -> key + ": " + state.artifactsByGa[key]!!.keys.joinToString() }
                .joinToString("\n")
        println("unique artifacts: ${state.artifactsByGa.size}")
        println("conflicts: $conflicts")
    }
}