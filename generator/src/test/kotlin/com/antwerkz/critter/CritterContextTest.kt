package com.antwerkz.critter

import com.antwerkz.critter.java.JavaBuilder
import com.antwerkz.critter.java.JavaClass
import com.antwerkz.critter.kotlin.KotlinBuilder
import com.antwerkz.critter.kotlin.KotlinClass
import com.antwerkz.kibble.Kibble
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File

class CritterContextTest {
    @Test(dataProvider = "forceScenarios")
    fun force(source: Long?, output: Long?, force: Boolean, result: Boolean) {
        val critterContext = CritterContext(force = force)
        Assert.assertEquals(critterContext.shouldGenerate(source, output), result)
    }

    @DataProvider(name = "forceScenarios")
    private fun forceScenarios(): Array<Array<out Any?>> {
        return arrayOf(
                arrayOf(null, null, false, true),    // both virtual
                arrayOf(100L, null, false, true),     // new output
                arrayOf(null, 100L, false, true),     // virtual in, existing out
                arrayOf(100L, 100L, false, false),      // same ages
                arrayOf(100L, 1000L, false, false),    // output is newer
                arrayOf(1000L, 100L, false, true),     // input is newer

                arrayOf(null, null, true, true),    // both virtual
                arrayOf(100L, null, true, true),     // new output
                arrayOf(null, 100L, true, true),     // virtual in, existing out
                arrayOf(100L, 100L, true, true),      // same ages
                arrayOf(100L, 1000L, true, true),    // output is newer
                arrayOf(1000L, 100L, true, true)      // input is newer

        )
    }

    @Test
    fun forceJava() {
        val files = File("../tests/java/src/main/java/").walkTopDown()
                .filter { it.name.endsWith(".java") }
        val directory = File("target/javaClassTest/")

        val critterContext = CritterContext(force = true)
        files.forEach { critterContext.add(JavaClass(critterContext, it)) }

        val builder = JavaBuilder(critterContext)
        builder.build(directory)

        val file = File(directory, "com/antwerkz/critter/test/criteria/PersonCriteria.java")
        Assert.assertTrue(file.exists())
        Assert.assertFalse(file.readLines().contains("test update"))

        file.writeText("test update")
        Assert.assertTrue(file.readLines().contains("test update"))
        builder.build(directory)
        Assert.assertFalse(file.readLines().contains("test update"))

        critterContext.force = false
        file.writeText("test update")
        Assert.assertTrue(file.readLines().contains("test update"))
        builder.build(directory)
        Assert.assertTrue(file.readLines().contains("test update"))

        critterContext.force = true
        builder.build(directory)
        Assert.assertFalse(file.readLines().contains("test update"))
    }

    @Test
    fun forceKotlin() {
        val files = File("../tests/kotlin/src/main/kotlin/").walkTopDown()
                .filter { it.name.endsWith(".kt") }
                .toList()
        val directory = File("target/kotlinClassTest/")

        val critterContext = CritterContext(force = true)
        Kibble.parse(files)
                .flatMap { it.classes }
                .forEach {
                    critterContext.add(KotlinClass(it))
                }

        val kotlinBuilder = KotlinBuilder(critterContext)
        kotlinBuilder.build(directory)

        val file = File(directory, "com/antwerkz/critter/test/criteria/PersonCriteria.kt")
        Assert.assertTrue(file.exists())
        Assert.assertFalse(file.readLines().contains("test update"))

        file.writeText("test update")
        Assert.assertTrue(file.readLines().contains("test update"))
        kotlinBuilder.build(directory)
        Assert.assertFalse(file.readLines().contains("test update"))

        critterContext.force = false
        file.writeText("test update")
        Assert.assertTrue(file.readLines().contains("test update"))
        kotlinBuilder.build(directory)
        Assert.assertTrue(file.readLines().contains("test update"))

        critterContext.force = true
        kotlinBuilder.build(directory)
        Assert.assertFalse(file.readLines().contains("test update"))
    }
}