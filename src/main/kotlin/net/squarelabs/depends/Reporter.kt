package net.squarelabs.depends

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import net.squarelabs.depends.models.Artifact

import picocli.CommandLine
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File

@CommandLine.Command(name = "depends", mixinStandardHelpOptions = true)
class Example : Runnable {
    @Option(
            names = arrayOf("-v", "--verbose"),
            description = arrayOf("Verbose mode." + "Multiple -v options increase the verbosity.")
    )
    private val verbose = BooleanArray(0)

    @Parameters(arity = "1", paramLabel = "coordinate", description = arrayOf("Root coordinate to resolve"))
    private var coordinate: String? = null

    @Option(
            names = arrayOf("-f", "--filter"),
            description = arrayOf("Filter results by this term")
    )
    private var filter: String? = null

    override fun run() {
        if (verbose.size > 0) {
            println("Resolving ${coordinate!!}...")
        }
        val state = State()
        val root: Artifact = resolve(coordinate!!, state)
        populateIndices(state)

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

        val results = if(filter != null) brokenApis.filter { it.contains(filter!!) } else brokenApis

        val output = results.map {
            val artifacts = state.artifactsByMethod[it]!!
            val gas = artifacts
                    .map { "${it.split(":")[0]}:${it.split(":")[1]}" }
                    .distinct()
            val gavs = gas.map { ga ->
                val versions = artifacts
                        .filter { ga == "${it.split(":")[0]}:${it.split(":")[1]}" }
                        .map { it.split(":").last() }
                        .joinToString(", ")
                "${ga} [${versions}]"
            }
            "$it is only present in \n\t${gavs}\n"
        }
        println("broken apis:\n\t ${output.joinToString("\n\t")}")
    }
}

fun main(args: Array<String>) {
    CommandLine.run(Example(), *args)
}

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
