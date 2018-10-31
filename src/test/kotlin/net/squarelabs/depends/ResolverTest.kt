package net.squarelabs.depends

import net.squarelabs.depends.models.Artifact
import org.junit.Test
import java.text.DecimalFormat

class ResolverTest {
    @Test
    fun `resolver should resolve`() {
        val runtime = Runtime.getRuntime()
        println("Used mem: ${DecimalFormat("#,##0").format(runtime.totalMemory() - runtime.freeMemory())}")
        val state = State()
        val root: Artifact = resolve("io.dropwizard:dropwizard-core:1.3.7", state)
        populateIndices(state)
        val conflicts = state.artifactsByGa.keys
                .filter { key -> state.artifactsByGa[key]!!.size > 1 }
                .map { key -> key + ": " + state.artifactsByGa[key]!!.keys.joinToString() }
                .joinToString(separator = "\n\t")
        println("unique artifacts: ${state.artifactsByGa.size}")
        println("conflicts:\n\t$conflicts")

        // find removals
        val removedMethods = state.artifactsByGa.keys
                .filter { key -> state.artifactsByGa[key]!!.size > 1 }
                .map { key -> state.artifactsByGa[key]!!.values }
                .flatMap { artifacts -> removedMethods(artifacts) }
        println("methods removed: ${removedMethods.size}")

        // print memory
        Runtime.getRuntime().gc()
        println("Used mem: ${DecimalFormat("#,##0").format(runtime.totalMemory() - runtime.freeMemory())}")
    }
}