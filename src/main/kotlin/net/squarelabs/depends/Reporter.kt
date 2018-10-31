package net.squarelabs.depends

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import net.squarelabs.depends.models.Artifact
import java.io.File

fun fqmns(a: Artifact): HashSet<String> = HashSet(a.classes.values.flatMap { c -> c.methods.values.map { m -> "${c.name}.${m.name}${m.descriptor}" } })

fun allMethods(artifacts: Collection<Artifact>): HashSet<String> {
    val reducer = { acc: HashSet<String>, cur: Artifact ->
        acc.addAll(fqmns(cur))
        acc
    }
    return artifacts.fold(HashSet(), reducer)
}

fun commonMethods(artifacts: Collection<Artifact>, all: Set<String>): HashSet<String> {
    val reducer = { acc: HashSet<String>, cur: Artifact ->
        acc.retainAll(fqmns(cur))
        acc
    }
    return artifacts.fold(HashSet(all), reducer)
}

fun removedMethods(artifacts: Collection<Artifact>): Set<String> {
    val accumulator: HashSet<String> = allMethods(artifacts)
    val common: Set<String> = commonMethods(artifacts, accumulator)
    accumulator.removeAll(common)
    return accumulator
}

fun apiMethods(state: State): Set<String> {
    val methods = mutableSetOf<String>()
    val missing = mutableSetOf<String>()
    state.artifactsByGa.values.forEach { artifacts ->
        artifacts.values.forEach { artifact ->
            println("finding api methods for artifact: ${artifact.coordinate}")
            val callerGa = "${artifact.coordinate.split(":")[0]}:${artifact.coordinate.split(":")[1]}"
            artifact.classes.values.forEach { clazz ->
                clazz.methods.values.forEach { method ->
                    method.invocations.forEach { call ->
                        val callee = "${call.fqcn}.${call.methodName}${call.descriptor}"
                        //println("${state.artifactsByMethod.size} callee: $callee example: ${state.artifactsByMethod.keys.first()}")
                        val providers:MutableSet<String>? = state.artifactsByMethod.get(callee)
                        if(providers != null) {
                            val exporters = providers.filter { art ->
                                val providerGa = "${art.split(":")[0]}:${art.split(":")[1]}"
                                providerGa != callerGa
                            }
                            if(exporters.isNotEmpty()) methods.add(callee)
                        } else {
                            if(!callee.startsWith("java")) {
                                missing.add(callee)
                            }
                        }
                    }
                }
            }
        }
    }
    File("missing.json").printWriter().use { writer ->
        val mapper = ObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
        mapper.writeValue(writer, missing)
    }

    return methods
}

fun populateIndices(state: State) {
    state.artifactsByGa.values.forEach { artifacts ->
        artifacts.values.forEach { artifact ->
            println("indexing artifact: ${artifact.coordinate}")
            artifact.classes.values.forEach { c ->
                c.methods.keys.forEach { m ->
                    val fqmn = "${c.name}.$m"
                    val methodProviders = state.artifactsByMethod.computeIfAbsent(fqmn) { HashSet() }
                    methodProviders.add(artifact.coordinate)
                }
            }
        }
    }
}