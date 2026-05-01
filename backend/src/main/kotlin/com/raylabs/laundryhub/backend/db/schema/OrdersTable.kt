package com.raylabs.laundryhub.backend.db.schema

import org.jetbrains.exposed.sql.Table

object OrdersTable : Table("orders") {
    val id = varchar("id", 100)
    val name = varchar("name", 255)
    val phoneNumber = varchar("phone_number", 50)
    val packageName = varchar("package_name", 100)
    val priceKg = varchar("price_kg", 50)
    val totalPrice = varchar("total_price", 50)
    val paidStatus = varchar("paid_status", 50)
    val paymentMethod = varchar("payment_method", 50)
    val remark = text("remark")
    val weight = varchar("weight", 50)
    val orderDate = varchar("order_date", 50)
    val dueDate = varchar("due_date", 50)

    override val primaryKey = PrimaryKey(id)
}
