package com.raylabs.laundryhub.core.data.repository

import android.content.Context
import com.raylabs.laundryhub.core.domain.repository.CacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class CacheRepositoryImpl @Inject constructor(
    private val context: Context
) : CacheRepository {

    override suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        val cacheDirs = buildList {
            add(context.cacheDir)
            context.externalCacheDirs?.forEach { add(it) }
        }
        cacheDirs.sumOf { it.sizeInBytes() }
    }

    override suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        val cacheDirs = buildList {
            add(context.cacheDir)
            context.externalCacheDirs?.forEach { add(it) }
        }

        cacheDirs.fold(true) { acc, dir ->
            acc && dir.clearContents()
        }
    }

    private fun File?.sizeInBytes(): Long {
        if (this == null || !exists()) return 0L
        if (isFile) return length()
        return listFiles()?.sumOf { it.sizeInBytes() } ?: 0L
    }

    private fun File?.clearContents(): Boolean {
        if (this == null || !exists()) return true
        var result = true
        listFiles()?.forEach { file ->
            result = result && file.deleteRecursively()
        }
        return result
    }
}
