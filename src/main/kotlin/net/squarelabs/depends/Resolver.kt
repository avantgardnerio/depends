package net.squarelabs.depends

import com.google.common.reflect.ClassPath
import net.squarelabs.depends.models.Artifact
import net.squarelabs.depends.models.Class
import org.jboss.shrinkwrap.resolver.api.maven.Maven
import java.net.URLClassLoader

fun resolve(coordinate: String, cache: HashMap<String, Artifact> = HashMap()): Artifact {
    return cache.computeIfAbsent(coordinate) { coordinate ->
        val artifacts = Maven.resolver()
                .resolve(coordinate)
                .withoutTransitivity()
                .asResolvedArtifact()
        assert(artifacts.size == 1)
        val artifact = artifacts[0]
        val file = artifact.asFile()
        val classes = URLClassLoader(arrayOf(file.toURL())).use { it ->
            println("${cache.size} artifacts + ${file.absolutePath}")
            val cp = ClassPath.from(it)
            cp.topLevelClasses.map { Class(it.name) }
        }
        val dependencies: List<Artifact> = artifact.dependencies.map {
            //println("resolving ${it.coordinate}")
            resolve(it.coordinate.toCanonicalForm(), cache)
        }
        Artifact(coordinate, file, dependencies, classes)
    }
}
