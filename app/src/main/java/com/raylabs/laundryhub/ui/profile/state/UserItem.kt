package com.raylabs.laundryhub.ui.profile.state

import com.raylabs.laundryhub.core.domain.model.auth.User

data class UserItem(
    val displayName: String,
    val email: String,
    val photoUrl: String? = null
)

fun User.toUI(): UserItem {
    return UserItem(
        displayName = "${this.displayName}",
        email = "${this.email}",
        photoUrl = "${this.urlPhoto}"
    )
}