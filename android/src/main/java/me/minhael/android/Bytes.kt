package me.minhael.android

import org.spongycastle.util.encoders.Base64
import java.security.MessageDigest
import kotlin.experimental.xor
import kotlin.math.min

fun ByteArray.xor(bytes: ByteArray) = xor(bytes, 0, bytes.size)

fun ByteArray.xor(bytes: ByteArray, offset: Int, length: Int): ByteArray {
    return this.zip(bytes.copyOfRange(offset, offset + length)) { a, b -> a xor b }.toByteArray()
}

fun ByteArray.toHexChars() = HexUtils.readCharArray(this)!!
fun ByteArray.toHexStr() = HexUtils.readString(this)!!

fun ByteArray.startsWith(prefix: ByteArray) = ByteArrayUtils.compare(this, prefix, min(this.size, prefix.size)) == 0

fun String.toHexBytes() = HexUtils.writeString(this)!!

fun CharArray.asBytes() = this.map { it.toByte() }.toByteArray()

fun ByteArray.decode2Str() = ICUMux.INSTANCE.decode(this)
fun String.encode2Bytes() = ICUMux.INSTANCE.encode(this)

fun String.base64() = Base64.decode(this)
fun ByteArray.base64() = String(Base64.encode(this))

fun Byte.toUnsignedInt() = this.toInt() and 0xff

fun ByteArray.digest(digest: MessageDigest = MessageDigest.getInstance("sha-256")): ByteArray {
    return digest.digest(this)
}