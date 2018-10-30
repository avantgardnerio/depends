package net.squarelabs.depends

import net.squarelabs.depends.models.Artifact
import org.junit.Test

class ResolverTest {
    @Test
    fun `resolver should resolve`() {
        val state = State()
        val root: Artifact = resolve("io.dropwizard:dropwizard-core:1.3.7", state)
        println("artifactsByGa=${state.artifactsByGa.size}")
    }
}