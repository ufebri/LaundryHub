package com.raylabs.laundryhub.core.data.repository

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.io.path.createTempDirectory

@OptIn(ExperimentalCoroutinesApi::class)
class CacheRepositoryImplTest {

    private val tempDirs = mutableListOf<File>()

    @After
    fun tearDown() {
        tempDirs.forEach(File::deleteRecursively)
        tempDirs.clear()
    }

    @Test
    fun `getCacheSizeBytes ignores unmounted external cache`() = runTest {
        val internalCache = createCacheDirWithFile("internal.tmp", "1234")
        val externalCache = createCacheDirWithFile("external.tmp", "123456")
        val context = mock<Context>()
        whenever(context.cacheDir).thenReturn(internalCache)
        whenever(context.externalCacheDirs).thenReturn(arrayOf(externalCache))
        val repository = CacheRepositoryImpl(context) { directory ->
            if (directory == externalCache) Environment.MEDIA_UNMOUNTED else Environment.MEDIA_UNKNOWN
        }

        assertEquals(4L, repository.getCacheSizeBytes())
    }

    @Test
    fun `getCacheSizeBytes includes read only external cache`() = runTest {
        val internalCache = createCacheDirWithFile("internal.tmp", "1234")
        val externalCache = createCacheDirWithFile("external.tmp", "123456")
        val context = mock<Context>()
        whenever(context.cacheDir).thenReturn(internalCache)
        whenever(context.externalCacheDirs).thenReturn(arrayOf(externalCache))
        val repository = CacheRepositoryImpl(context) { directory ->
            if (directory == externalCache) {
                Environment.MEDIA_MOUNTED_READ_ONLY
            } else {
                Environment.MEDIA_UNKNOWN
            }
        }

        assertEquals(10L, repository.getCacheSizeBytes())
    }

    @Test
    fun `clearCache clears internal and mounted external cache`() = runTest {
        val internalCache = createCacheDirWithFile("internal.tmp", "1234")
        val externalCache = createCacheDirWithFile("external.tmp", "123456")
        val context = mock<Context>()
        whenever(context.cacheDir).thenReturn(internalCache)
        whenever(context.externalCacheDirs).thenReturn(arrayOf(externalCache))
        val repository = CacheRepositoryImpl(context) { directory ->
            if (directory == externalCache) Environment.MEDIA_MOUNTED else Environment.MEDIA_UNKNOWN
        }

        assertTrue(repository.clearCache())
        assertTrue(internalCache.exists())
        assertTrue(externalCache.exists())
        assertTrue(internalCache.listFiles().isNullOrEmpty())
        assertTrue(externalCache.listFiles().isNullOrEmpty())
        assertFalse(File(internalCache, "internal.tmp").exists())
        assertFalse(File(externalCache, "external.tmp").exists())
    }

    private fun createCacheDirWithFile(fileName: String, content: String): File {
        val directory = createTempDirectory("cache-repository-test").toFile()
        tempDirs += directory
        File(directory, fileName).writeText(content)
        return directory
    }
}
