package net.squarelabs.depends

import com.google.common.reflect.ClassPath
import net.squarelabs.depends.models.Artifact
import net.squarelabs.depends.models.Class
import net.squarelabs.depends.models.Invocation
import net.squarelabs.depends.models.Method
import org.apache.commons.io.FilenameUtils
import org.jboss.shrinkwrap.resolver.api.maven.Maven
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.InstructionAdapter
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URLClassLoader
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

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
    val classes = mutableMapOf<String,Class>()
    ZipFile(file).use { zipFile ->
        zipFile.stream().forEach { entry ->
            val ext = FilenameUtils.getExtension(entry.name)
            if(ext == "class") {
                val packageName = FilenameUtils.getPath(entry.name).replace("/", ".")
                val className = FilenameUtils.getBaseName(entry.name)
                val fqcn = "$packageName$className"
                zipFile.getInputStream(entry).use { stream ->
                    val methods = methodsFromClass(stream)
                    classes[fqcn] = Class(fqcn, methods)
                }
            }
        }
    }
    return classes.toMap()
}

fun methodsFromClass(stream: InputStream): Map<String, Method> {
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
                    method.invocations.add(Invocation(owner.replace("/", "."), name, descriptor))
                    //println("invoke $owner.$name from ${clazz.name}.$methodName()")
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                }
            }
            methodCount++
            return instMv
        }
    }

    val classReader = ClassReader(stream)
    classReader.accept(cl, 0)

    return methods.toMap()
}