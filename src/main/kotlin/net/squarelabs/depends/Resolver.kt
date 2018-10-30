package net.squarelabs.depends

import com.google.common.reflect.ClassPath
import net.squarelabs.depends.models.Artifact
import net.squarelabs.depends.models.Class
import org.jboss.shrinkwrap.resolver.api.maven.Maven
import java.io.File
import java.net.URLClassLoader

fun resolve(coordinate: String, cache: HashMap<String, Artifact> = HashMap()): Artifact {
    return cache.computeIfAbsent(coordinate) {
        val artifacts = Maven.resolver()
                .resolve(it)
                .withoutTransitivity()
                .asResolvedArtifact()
        assert(artifacts.size == 1)
        val artifact = artifacts[0]
        val file = artifact.asFile()
        println("${cache.size} artifacts + ${file.absolutePath}")
        val classes = classesFromFile(file)
        val dependencies = artifact.dependencies.map { resolve(it.coordinate.toCanonicalForm(), cache) }
        Artifact(it, file, dependencies, classes)
    }
}

fun classesFromFile(file: File): List<Class> {
    return URLClassLoader(arrayOf(file.toURL())).use { it ->
        val cp = ClassPath.from(it)
        cp.topLevelClasses.map { Class(it.name) }
    }
}
