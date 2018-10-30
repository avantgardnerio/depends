package net.squarelabs.depends

import net.squarelabs.depends.models.Artifact
import org.jboss.shrinkwrap.resolver.api.maven.Maven

fun resolve(coordinate: String): Artifact {
    val artifacts = Maven.resolver()
            .resolve(coordinate)
            .withoutTransitivity()
            .asResolvedArtifact()
    assert(artifacts.size == 1)
    val artifact = artifacts[0]
    val dependencies: List<Artifact> = artifact.dependencies.map {
        println("resolving ${it.coordinate}")
        resolve(it.coordinate.toCanonicalForm())
    }
    return Artifact(coordinate, artifact.asFile(), dependencies)
}
