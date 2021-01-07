package me.minhael.android

import android.content.SharedPreferences
import me.minhael.design.Props
import me.minhael.design.Serializer
import me.minhael.design.Store
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.locks.ReentrantLock

/**
 * Implement [Props] by Android shared preference
 */
class AndroidProps constructor(
    private val preferences: SharedPreferences,
    private val serializer: Serializer
) : Props {

    private val lock = ReentrantLock(true)

    override fun has(key: String): Boolean {
        lock.lock()
        return try {
            preferences.contains(key)
        } finally {
            lock.unlock()
        }
    }

    override fun clear() {
        lock.lock()
        return try {
            preferences.edit().clear().apply()
        } finally {
            lock.unlock()
        }
    }

    override fun <T> commit(callable: (Store) -> T): T {
        return preferences.edit().let {
            val rt = callable(Session(preferences, it, serializer))

            lock.lock()
            try {
                it.apply()
            } finally {
                lock.unlock()
            }

            rt
        }
    }

    override fun <T : Any> get(key: String, defValue: T): T {
        lock.lock()
        return try {
            getValue(serializer, preferences, key, defValue)
        } finally {
            lock.unlock()
        }
    }

    override fun <T : Any> put(key: String, value: T) {
        lock.lock()
        try {
            preferences.edit().apply {
                setValue(serializer, this, key, value)
                apply()
            }
        } finally {
            lock.unlock()
        }
    }

    private class Session(
        private val preferences: SharedPreferences,
        private val editor: SharedPreferences.Editor,
        private val serializer: Serializer
    ) : Store {

        override fun <T : Any> get(key: String, defValue: T): T {
            return getValue(serializer, preferences, key, defValue)
        }

        override fun <T : Any> put(key: String, value: T) {
            setValue(
                serializer,
                editor,
                key,
                value
            )
        }

        override fun has(key: String): Boolean {
            return preferences.contains(key)
        }

        override fun clear() {
            editor.clear()
        }
    }

    companion object {

        private fun <T : Any> setValue(
            serializer: Serializer,
            editor: SharedPreferences.Editor,
            key: String,
            value: T
        ): T {
            return value
                .also {
                    when {
                        ByteArray::class.isInstance(value) -> editor.putString(
                            key,
                            (value as ByteArray).base64()
                        )
                        Boolean::class.isInstance(value) -> editor.putBoolean(key, value as Boolean)
                        Int::class.isInstance(value) -> editor.putLong(key, (value as Int).toLong())
                        Long::class.isInstance(value) -> editor.putLong(key, value as Long)
                        String::class.isInstance(value) -> editor.putString(key, value as String)
                        else -> ByteArrayOutputStream().use {
                            serializer.serialize(value, it)
                            it.toByteArray()
                        }.apply {
                            editor.putString(key, this.base64())
                        }
                    }
                }
        }

        private fun <T : Any> getValue(
            serializer: Serializer,
            preferences: SharedPreferences,
            key: String,
            defValue: T
        ): T {
            return when {
                ByteArray::class.isInstance(defValue) -> preferences
                    .getString(key, null)
                    ?.base64() ?: defValue
                Boolean::class.isInstance(defValue) -> preferences
                    .getBoolean(key, defValue as Boolean)
                Int::class.isInstance(defValue) -> preferences
                    .getLong(key, (defValue as Int).toLong()).toInt()
                Long::class.isInstance(defValue) -> preferences.getLong(key, defValue as Long)
                String::class.isInstance(defValue) -> preferences
                    .getString(key, defValue as String)
                else -> preferences.getString(key, null)
                    ?.let { value ->
                        ByteArrayInputStream(value.base64()).use {
                            serializer.deserialize(it, defValue::class.java)
                        }
                    }
            }.let {
                defValue::class.java.cast(it) ?: defValue
            }
        }
    }
}