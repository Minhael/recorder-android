package me.minhael.design

interface Charsets {

    fun encode(string: String, charset: List<String> = DEFAULT_CHARSET): ByteArray
    fun decode(bytes: ByteArray, charset: List<String> = DEFAULT_CHARSET): String

    companion object {

        private val DEFAULT_CHARSET = listOf(
                "iso-8859-1",
                "iso-8859-2",
                "iso-8859-5",
                "iso-8859-6",
                "iso-8859-7",
                "iso-8859-8",
                "iso-8859-9"
        )
    }
}