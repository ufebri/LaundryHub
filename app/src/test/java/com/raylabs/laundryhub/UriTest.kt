package com.raylabs.laundryhub
import android.net.Uri
import org.junit.Test
class UriTest {
    @Test
    fun testUri() {
        val uri = Uri.parse("https://google.com")
        println(uri)
    }
}
