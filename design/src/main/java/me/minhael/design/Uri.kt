package me.minhael.design

import java.io.InputStream
import java.io.OutputStream
import java.net.URI

object Uri {

    interface Accessor {

        val schemes: List<String>

        fun readFrom(uri: String): InputStream
        fun writeTo(uri: String): OutputStream
    }

    class Resolver(
        private vararg val accessors: Accessor
    ): Accessor {

        override val schemes = accessors.map { it.schemes }.flatten()

        override fun readFrom(uri: String): InputStream {
            return selectAccessor(uri).readFrom(uri)
        }

        override fun writeTo(uri: String): OutputStream {
            return selectAccessor(uri).writeTo(uri)
        }

        private fun selectAccessor(uri: String): Accessor {
            return URI.create(uri).scheme
                ?.let { scheme ->
                    accessors.find { accessor -> accessor.schemes.any { it.equals(scheme, true) } }
                }
                ?: throw UnsupportedOperationException("Invalid URI: $uri")
        }
    }
}