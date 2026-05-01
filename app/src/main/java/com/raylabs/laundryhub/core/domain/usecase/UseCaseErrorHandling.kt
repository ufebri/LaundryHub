package com.raylabs.laundryhub.core.domain.usecase

import com.raylabs.laundryhub.shared.util.Resource

object UseCaseErrorHandling {

    val handleNotFoundID = Resource.Error("Failed to retrieve ID.")

    val handleFailRetry = Resource.Error("Failed after 3 attempts.")

    val handleFailedSubmit = Resource.Error("Failed to submit data")
}