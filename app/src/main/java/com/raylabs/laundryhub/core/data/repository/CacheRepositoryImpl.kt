package com.raylabs.laundryhub.core.data.repository

import android.content.Context
import android.os.Environment
import com.raylabs.laundryhub.core.domain.repository.CacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CacheRepositoryImpl(
    private val context: Context,
    private val externalStorageStateProvider: (File) -> String = Environment::getExternalStorageState
) : CacheRepository {

    override suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        cacheDirectories(includeReadOnlyExternal = true).sumOf { it.sizeInBytes() }
    }

    override suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        cacheDirectories(includeReadOnlyExternal = false).fold(true) { acc, dir ->
            acc && dir.clearContents()
        }
    }

    private fun cacheDirectories(includeReadOnlyExternal: Boolean): List<File> = buildList {
        add(context.cacheDir)
        context.externalCacheDirs
            ?.filterNotNull()
            ?.distinctBy(File::getAbsolutePath)
            ?.filter { it.isAccessibleExternalCache(includeReadOnlyExternal) }
            ?.forEach(::add)
    }

    // `externalCacheDirs` is app-specific, but we still guard against unmounted/read-only storage.
    private fun File.isAccessibleExternalCache(includeReadOnlyExternal: Boolean): Boolean {
        val allowedStates = if (includeReadOnlyExternal) {
            setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
        } else {
            setOf(Environment.MEDIA_MOUNTED)
        }

        return runCatching {
            externalStorageStateProvider(this) in allowedStates
        }.getOrDefault(false)
    }

    private fun File?.sizeInBytes(): Long {
        return try {
            if (this == null || !exists()) return 0L
            if (isFile) return length()
            listFiles()?.sumOf { it.sizeInBytes() } ?: 0L
        } catch (_: SecurityException) {
            0L
        }
    }

    private fun File?.clearContents(): Boolean {
        return try {
            if (this == null || !exists()) return true
            var result = true
            listFiles()?.forEach { file ->
                result = result && file.deleteRecursively()
            }
            result
        } catch (_: SecurityException) {
            false
        }
    }
}
