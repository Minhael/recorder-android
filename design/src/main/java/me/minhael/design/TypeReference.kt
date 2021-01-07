package me.minhael.design

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Get [Class] or [Type] with preserved generic type.
 *
 * Reference to fasterxml/Jackson library & some old sources
 */
abstract class TypeReference<T> : Comparable<TypeReference<T>> {

    protected val type: Type
    protected val cls: Class<T>

    protected constructor() {
        val superClass = javaClass.genericSuperclass
        require(superClass !is Class<*>) {
            // sanity check, should never happen
            "Internal error: TypeReference constructed without actual type information"
        }
        this.type = (superClass as ParameterizedType).actualTypeArguments[0]
        @Suppress("UNCHECKED_CAST")
        this.cls = if (type is Class<*>) type as Class<T> else (type as ParameterizedType).rawType as Class<T>
    }

    private constructor(cls: Class<T>) {
        this.type = cls
        this.cls = cls
    }

    fun asType() = type
    fun asClass() = cls

    fun cast(obj: Any): T = cls.cast(obj)

    override fun compareTo(other: TypeReference<T>): Int {
        return 0
    }

    companion object {

        /**
         * Reflect the [Class] type, if preserving generic types is not necessary
         */
        inline fun <reified T> reflect() = T::class.java

        fun <T> from(cls: Class<T>) = object : TypeReference<T>(cls) { }
    }
}
