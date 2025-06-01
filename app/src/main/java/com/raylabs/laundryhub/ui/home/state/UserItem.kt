package com.raylabs.laundryhub.ui.home.state

import com.raylabs.laundryhub.core.domain.model.auth.User

data class UserItem(
    val displayName: String
)

fun User.toUI(): UserItem {
    return UserItem(displayName = this.displayName ?: "Guest")
}
