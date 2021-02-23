package me.minhael.design.android

import android.content.ContentResolver
import androidx.test.platform.app.InstrumentationRegistry
import me.minhael.design.fs.FileSystem
import me.minhael.design.test.FileSystemTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class AndroidFSTest : FileSystemTest {

    override val subject = InstrumentationRegistry.getInstrumentation().targetContext.let { context ->
        val dir = context.cacheDir
        val root = File(dir, "test")

        if (!root.isDirectory)
            root.delete()
        if (!root.exists())
            root.mkdirs()

        AndroidFS.base(context, root)
    }
    override val input = listOf(
        FileSystem.Meta("", "file1.txt", "text/plain", 34L) to "Quick Brown Fox Jump Over Lazy Dog",
        FileSystem.Meta("", "file2.txt", "text/plain", 12L) to "Hello World!"
    )

    @Test
    fun testAccessorSchemes() {
        assertEquals(
            listOf(ContentResolver.SCHEME_ANDROID_RESOURCE, ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_FILE),
            subject.accessor().schemes
        )
    }
}