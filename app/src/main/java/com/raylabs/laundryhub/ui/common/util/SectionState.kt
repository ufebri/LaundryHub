package com.raylabs.laundryhub.ui.common.util

data class SectionState<T>(
    val isLoading: Boolean = false,
    val data: T? = null,
    val errorMessage: String? = null
)

fun <T> SectionState<T>.loading() = copy(isLoading = true, errorMessage = null)
fun <T> SectionState<T>.success(data: T) = copy(isLoading = false, data = data, errorMessage = null)
fun <T> SectionState<T>.error(message: String?) =
    copy(isLoading = false, data = null, errorMessage = message)
