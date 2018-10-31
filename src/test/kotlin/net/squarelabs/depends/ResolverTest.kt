package net.squarelabs.depends

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import net.squarelabs.depends.models.Artifact
import org.junit.Test
import java.io.File
import java.text.DecimalFormat

class ResolverTest {
    @Test
    fun `resolver should resolve`() {
        val runtime = Runtime.getRuntime()
        println("Used mem: ${DecimalFormat("#,##0").format(runtime.totalMemory() - runtime.freeMemory())}")
        val state = State()
        val root: Artifact = resolve("io.dropwizard:dropwizard-core:1.3.7", state)
        populateIndices(state)
        File("artifactsByMethod.json").printWriter().use { writer ->
            val mapper = ObjectMapper()
            mapper.enable(SerializationFeature.INDENT_OUTPUT)
            mapper.writeValue(writer, state.artifactsByMethod)
        }

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

        // find apis
        val apis = apiMethods(state)
        println("api methods: ${apis.size}")

        // find removed apis
        val brokenApis = apis.intersect(removedMethods)
        println("broken apis: ${brokenApis.size}")

        // print memory
        Runtime.getRuntime().gc()
        println("Used mem: ${DecimalFormat("#,##0").format(runtime.totalMemory() - runtime.freeMemory())}")
    }
}