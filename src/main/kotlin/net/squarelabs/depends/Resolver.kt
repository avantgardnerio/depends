package net.squarelabs.depends

import com.google.common.reflect.ClassPath
import net.squarelabs.depends.models.Artifact
import net.squarelabs.depends.models.Class
import net.squarelabs.depends.models.Invocation
import net.squarelabs.depends.models.Method
import org.jboss.shrinkwrap.resolver.api.maven.Maven
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.InstructionAdapter
import java.io.File
import java.net.URLClassLoader

var methodCount = 0
var invocationCount = 0

fun resolve(coordinate: String, state: State): Artifact {
    val terms = coordinate.split(":")
    val ga = state.artifactsByGa.computeIfAbsent("${terms[0]}:${terms[1]}") { HashMap() }
    val version = terms[terms.size - 1]
    return ga.computeIfAbsent(version) { version ->
        val artifacts = Maven.resolver()
                .resolve(coordinate)
                .withoutTransitivity()
                .asResolvedArtifact()
        assert(artifacts.size == 1)
        val artifact = artifacts[0]
        val file = artifact.asFile()
        println("${state.artifactsByGa.size} artifacts + ${file.absolutePath}")
        val classes = classesFromFile(file)
        val dependencies = artifact.dependencies.map { resolve(it.coordinate.toCanonicalForm(), state) }
        Artifact(coordinate, file, dependencies, classes)
    }
}

fun classesFromFile(file: File): Map<String,Class> {
    return URLClassLoader(arrayOf(file.toURL())).use { it ->
        val cp = ClassPath.from(it)
        val size = cp.topLevelClasses.size
        val classes = mutableMapOf<String,Class>()
        cp.topLevelClasses.forEachIndexed { index, clazz ->
            val methods = methodsFromClass(clazz, it)
            if (index % 1000 == 0) println("$methodCount methods and $invocationCount invocations class $index / $size is ${clazz.name}")
            // assert(classes.get(clazz.name) == null) // TODO: handle duplicates /foo/bar/class vs /foo.bar/class
            classes[clazz.name] = Class(clazz.name, methods)
        }
        classes.toMap()
    }
}

fun methodsFromClass(clazz: ClassPath.ClassInfo, loader: URLClassLoader): Map<String, Method> {
    val methods = mutableMapOf<String, Method>()
    val cl = object : ClassVisitor(Opcodes.ASM7) {
        override fun visitMethod(access: Int, methodName: String,
                                 desc: String, signature: String?, exceptions: Array<String>?): MethodVisitor? {
            val method = Method(methodName, desc)
            val methodId = "$methodName$desc"
            assert(methods[methodId] == null)
            methods[methodId] = method

            val oriMv: MethodVisitor = object : MethodVisitor(Opcodes.ASM7) {}
            val instMv = object : InstructionAdapter(Opcodes.ASM7, oriMv) {
                override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
                    invocationCount++
                    method.invocations.add(Invocation(owner, name, descriptor))
                    //println("invoke $owner.$name from ${clazz.name}.$methodName()")
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                }
            }
            methodCount++
            return instMv
        }
    }

    loader.getResourceAsStream(clazz.resourceName).use { stream ->
        val classReader = ClassReader(stream)
        classReader.accept(cl, 0)
    }

    return methods.toMap()
}