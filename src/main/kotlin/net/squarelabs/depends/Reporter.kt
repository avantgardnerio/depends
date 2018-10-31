package net.squarelabs.depends

import net.squarelabs.depends.models.Artifact

fun fqmns(a: Artifact): HashSet<String> = HashSet(a.classes.values.flatMap { c -> c.methods.values.map { m -> "${c.name}:${m.name}${m.descriptor}" } })

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