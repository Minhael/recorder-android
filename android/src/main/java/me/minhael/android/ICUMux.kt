package me.minhael.android

import com.ibm.icu.charset.CharsetICU
import com.ibm.icu.charset.CharsetSelector
import com.ibm.icu.text.CharsetDetector
import com.ibm.icu.text.UnicodeSet
import me.minhael.design.Charsets
import java.nio.charset.Charset

/**
 * To encode2Bytes & decode2Str string to byte arrays with correct charset
 */
class ICUMux private constructor() : Charsets {
    override fun encode(string: String, charset: List<String>): ByteArray {
        return CharsetSelector(charset, UnicodeSet.EMPTY, CharsetICU.ROUNDTRIP_SET).selectForString(string).let {
            if (it.isNotEmpty())
                string.toByteArray(Charset.forName(it[0]))
            else
                string.toByteArray()
        }
    }

    override fun decode(bytes: ByteArray, charset: List<String>): String {
        return try {
            CharsetDetector()
                    .setText(bytes)
                    .detectAll()
                    .filter {
                        if (charset.isNotEmpty())
                            it.name.toLowerCase() in charset
                        else
                            true
                    }
                    .let {
                        if (it.isNotEmpty())
                            it[0].string
                        else
                            String(bytes)
                    }
        } catch (e: Throwable) {
            String(bytes)
        }
    }

    companion object {
        @JvmStatic
        val INSTANCE = ICUMux()
    }
}