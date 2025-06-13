package com.raylabs.laundryhub.core.domain.model.sheets

data class OrderData(
    val orderId: String,
    val name: String,
    val phoneNumber: String,
    val packageName: String,
    val priceKg: String,
    val totalPrice: String,
    val paidStatus: String,
    val paymentMethod: String,
    val remark: String,
)