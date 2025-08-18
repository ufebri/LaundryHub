package com.raylabs.laundryhub.core.domain.repository

interface UpdateCheckerRepository {
    suspend fun checkAndPromptIfNeeded(): Boolean
}