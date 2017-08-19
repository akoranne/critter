package com.antwerkz.critter

import com.antwerkz.critter.Visibility.PUBLIC
import org.mongodb.morphia.annotations.Embedded

abstract class CritterClass(var pkgName: String?, var name: String) : AnnotationHolder, Visible {
    val qualifiedName: String by lazy {
        pkgName?.let { "${pkgName}.${name}" } ?: name
    }

    abstract val fields: List<CritterField>

    val isEmbedded: Boolean by lazy {
        hasAnnotation(Embedded::class.java)
    }

    override var visibility: Visibility = PUBLIC

    abstract fun lastModified(): Long
}

class CritterAnnotation(val name: String, val values: Map<String, Any> = mapOf<String, Any>()) {

    var klass: Class<out Annotation>? = null

    fun matches(aClass: Class<out Annotation>): Boolean {
        return aClass == klass || aClass.name == name
    }

    fun getValue(): String? {
        return values["value"] as String?
    }

}

fun String.nameCase(): String {
    return substring(0, 1).toUpperCase() + substring(1)
}
