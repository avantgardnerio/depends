package net.squarelabs.depends

import com.google.common.reflect.ClassPath
import net.squarelabs.depends.models.Artifact
import net.squarelabs.depends.models.Class
import net.squarelabs.depends.models.Method
import org.jboss.shrinkwrap.resolver.api.maven.Maven
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.InstructionAdapter
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
        cp.topLevelClasses.map { clazz ->
            val methods = methodsFromClass(clazz, it)
            Class(clazz.name, methods)
        }
    }
}

fun methodsFromClass(clazz: ClassPath.ClassInfo, loader: URLClassLoader): List<Method> {
    val methods = mutableListOf<Method>()
    val cl = object : ClassVisitor(Opcodes.ASM7) {
        override fun visitMethod(access: Int, methodName: String,
                                 desc: String, signature: String?, exceptions: Array<String>?): MethodVisitor? {
            val oriMv: MethodVisitor = object : MethodVisitor(Opcodes.ASM7) {}
            val instMv = object : InstructionAdapter(Opcodes.ASM7, oriMv) {
                override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, descriptor: String?, isInterface: Boolean) {
                    //println("invoke $owner.$name from ${clazz.name}.$methodName()")
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                }
            }
            val method = Method(methodName, desc)
            methods.add(method)
            return instMv
        }
    }

    loader.getResourceAsStream(clazz.resourceName).use { stream ->
        val classReader = ClassReader(stream)
        classReader.accept(cl, 0)
    }

    return methods.toList()
}