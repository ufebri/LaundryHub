package com.raylabs.laundryhub.core.domain.model.fcm

import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenRequest(
    val token: String
)
