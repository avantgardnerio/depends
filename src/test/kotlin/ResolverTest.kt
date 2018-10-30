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
            // Do stuff with classes here...
            val cl = object : ClassVisitor(Opcodes.ASM7) {

                /**
                 * Called when a class is visited. This is the method called first
                 */
                override fun visit(version: Int, access: Int, name: String?,
                                   signature: String?, superName: String?, interfaces: Array<String>?) {
                    println("Visiting class: $name")
                    println("Class Major Version: $version")
                    println("Super class: $superName")
                    super.visit(version, access, name, signature, superName, interfaces)
                }

                /**
                 * Invoked only when the class being visited is an inner class
                 */
                override fun visitOuterClass(owner: String, name: String, desc: String) {
                    println("Outer class: $owner")
                    super.visitOuterClass(owner, name, desc)
                }

                /**
                 * Invoked when a class level annotation is encountered
                 */
                override fun visitAnnotation(desc: String?,
                                             visible: Boolean): AnnotationVisitor? {
                    println("Annotation: $desc")
                    return super.visitAnnotation(desc, visible)
                }

                /**
                 * When a class attribute is encountered
                 */
                override fun visitAttribute(attr: Attribute) {
                    System.out.println("Class Attribute: " + attr.type)
                    super.visitAttribute(attr)
                }

                /**
                 * When an inner class is encountered
                 */
                override fun visitInnerClass(name: String?, outerName: String?,
                                             innerName: String?, access: Int) {
                    println("Inner Class: $innerName defined in $outerName")
                    super.visitInnerClass(name, outerName, innerName, access)
                }

                /**
                 * When a field is encountered
                 */
                override fun visitField(access: Int, name: String?,
                                        desc: String?, signature: String?, value: Any?): FieldVisitor? {
                    println("Field: $name $desc value:$value")
                    return super.visitField(access, name, desc, signature, value)
                }


                override fun visitEnd() {
                    println("Method ends here")
                    super.visitEnd()
                }

                /**
                 * When a method is encountered
                 */
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
                    //return super.visitMethod(access, name, desc, signature, exceptions)
                }

                /**
                 * When the optional source is encountered
                 */
                override fun visitSource(source: String?, debug: String?) {
                    println("Source: $source")
                    super.visitSource(source, debug)
                }


            }
            loader.getResourceAsStream(clazz.resourceName).use {
                val classReader = ClassReader(it)
                classReader.accept(cl, 0)
            }
        }
    }
}