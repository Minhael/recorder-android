package me.minhael.design.android

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import me.minhael.design.sl.FstSerializer
import me.minhael.design.test.PropsTest
import org.junit.jupiter.api.Test

internal class AndroidPropsTest: PropsTest() {

    override val props = AndroidProps(
        InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("default", Application.MODE_PRIVATE),
        FstSerializer()
    )

    @Test
    fun dummy() {
    }
}