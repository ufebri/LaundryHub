package com.raylabs.laundryhub.core.domain.usecase.update

import com.raylabs.laundryhub.core.domain.repository.UpdateCheckerRepository

class CheckAppUpdateUseCase(
    private val checker: UpdateCheckerRepository
) {
    suspend operator fun invoke(): Boolean = checker.checkAndPromptIfNeeded()
}