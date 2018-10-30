package net.squarelabs.depends

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import net.squarelabs.depends.models.Artifact
import org.junit.Test
import java.io.File

class ResolverTest {
    @Test
    fun `resolver should resolve`() {
        val root: Artifact = resolve("io.dropwizard:dropwizard-core:1.3.7")
        File("out.json").printWriter().use { writer ->
            val mapper = ObjectMapper()
            mapper.enable(SerializationFeature.INDENT_OUTPUT)
            mapper.writeValue(writer, root)
        }
    }
}