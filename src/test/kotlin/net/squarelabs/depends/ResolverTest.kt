package net.squarelabs.depends

import net.squarelabs.depends.models.Artifact
import org.junit.Test

class ResolverTest {
    @Test
    fun `resolver should resolve`() {
        val root: Artifact = resolve("io.dropwizard:dropwizard-core:1.3.7")
        println("root=${root.coordinate}")
    }
}