import com.google.common.reflect.ClassPath
import org.junit.Assert
import org.junit.Test
import org.jboss.shrinkwrap.resolver.api.maven.Maven
import java.net.URLClassLoader
import org.objectweb.asm.*
import org.objectweb.asm.commons.InstructionAdapter

class ResolverTest {
    @Test
    fun `should resolve`() {
        // Resolve
        val artifacts = Maven.resolver()
                .resolve("io.dropwizard:dropwizard-core:1.3.7")
                .withoutTransitivity()
                .asResolvedArtifact()
        Assert.assertEquals(1, artifacts.size)

        // get file
        val artifact = artifacts[0]
        val file = artifact.asFile()
        Assert.assertTrue(file.exists())

        // load jar
        val loader = URLClassLoader(arrayOf(file.toURL()), this.javaClass.classLoader)
        val cp = ClassPath.from(loader)
        Assert.assertTrue(cp.topLevelClasses.size > 0)
        for (clazz in cp.topLevelClasses) {
            val cl = object : ClassVisitor(Opcodes.ASM7) {
                override fun visitMethod(access: Int, methodName: String?,
                                         desc: String?, signature: String?, exceptions: Array<String>?): MethodVisitor? {
                    println("Method: $methodName $desc")
                    System.out.println("\n" + methodName + desc)
                    val oriMv: MethodVisitor = object : MethodVisitor(Opcodes.ASM7) {}
                    val instMv = object : InstructionAdapter(Opcodes.ASM7, oriMv) {
                        override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, descriptor: String?, isInterface: Boolean) {
                            println("invoke ${owner}.${name} from ${file.name} ${clazz.name}.${methodName}()")
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                        }

                    }
                    return instMv
                }

                override fun visitEnd() {
                    println("Method ends here")
                    super.visitEnd()
                }
            }

            loader.getResourceAsStream(clazz.resourceName).use {
                val classReader = ClassReader(it)
                classReader.accept(cl, 0)
            }
        }
    }
}