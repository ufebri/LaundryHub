package com.raylabs.laundryhub.core.data.config

interface RemoteConfigSource {
    suspend fun refresh(force: Boolean): Boolean

    fun getString(key: String): String

    fun getBoolean(key: String): Boolean

    fun getLong(key: String): Long
}
