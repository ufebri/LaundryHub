package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.OrdersTable
import com.raylabs.laundryhub.backend.plugins.dbQuery
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class OrderRepository {

    suspend fun insert(order: OrderData): Boolean = dbQuery {
        val statement = OrdersTable.insertIgnore {
            it[id] = order.orderId
            it[name] = order.name
            it[phoneNumber] = order.phoneNumber
            it[packageName] = order.packageName
            it[priceKg] = order.priceKg
            it[totalPrice] = order.totalPrice
            it[paidStatus] = order.paidStatus
            it[paymentMethod] = order.paymentMethod
            it[remark] = order.remark
            it[weight] = order.weight
            it[orderDate] = order.orderDate
            it[dueDate] = order.dueDate
            it[isSynced] = false
        }
        statement.insertedCount > 0
    }

    suspend fun update(orderId: String, order: OrderData): Boolean = dbQuery {
        val updatedCount = OrdersTable.update({ OrdersTable.id eq orderId }) {
            it[name] = order.name
            it[phoneNumber] = order.phoneNumber
            it[packageName] = order.packageName
            it[priceKg] = order.priceKg
            it[totalPrice] = order.totalPrice
            it[paidStatus] = order.paidStatus
            it[paymentMethod] = order.paymentMethod
            it[remark] = order.remark
            it[weight] = order.weight
            it[orderDate] = order.orderDate
            it[dueDate] = order.dueDate
            it[isSynced] = false
        }
        updatedCount > 0
    }

    suspend fun getUnsyncedOrders(): List<OrderData> = dbQuery {
        OrdersTable.select { OrdersTable.isSynced eq false }.map {
            OrderData(
                orderId = it[OrdersTable.id],
                name = it[OrdersTable.name],
                phoneNumber = it[OrdersTable.phoneNumber],
                packageName = it[OrdersTable.packageName],
                priceKg = it[OrdersTable.priceKg],
                totalPrice = it[OrdersTable.totalPrice],
                paidStatus = it[OrdersTable.paidStatus],
                paymentMethod = it[OrdersTable.paymentMethod],
                remark = it[OrdersTable.remark],
                weight = it[OrdersTable.weight],
                orderDate = it[OrdersTable.orderDate],
                dueDate = it[OrdersTable.dueDate]
            )
        }
    }

    suspend fun markAsSynced(orderIds: List<String>): Boolean = dbQuery {
        if (orderIds.isEmpty()) return@dbQuery true
        val updatedCount = OrdersTable.update({ OrdersTable.id inList orderIds }) {
            it[isSynced] = true
        }
        updatedCount > 0
    }

    suspend fun delete(orderId: String): Boolean = dbQuery {
        val deletedCount = OrdersTable.deleteWhere { id eq orderId }
        deletedCount > 0
    }

    suspend fun insertAll(orders: List<OrderData>): Int = dbQuery {
        var insertedCount = 0
        for (order in orders) {
            val statement = OrdersTable.insertIgnore {
                it[id] = order.orderId
                it[name] = order.name
                it[phoneNumber] = order.phoneNumber
                it[packageName] = order.packageName
                it[priceKg] = order.priceKg
                it[totalPrice] = order.totalPrice
                it[paidStatus] = order.paidStatus
                it[paymentMethod] = order.paymentMethod
                it[remark] = order.remark
                it[weight] = order.weight
                it[orderDate] = order.orderDate
                it[dueDate] = order.dueDate
            }
            if (statement.insertedCount > 0) insertedCount++
        }
        insertedCount
    }

    suspend fun getAll(
        page: Int = 1, 
        size: Int = 50, 
        filter: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): List<OrderData> = dbQuery {
        val offset = ((page - 1) * size).toLong()
        val query = OrdersTable.selectAll()
        
        filter?.let {
            when (it) {
                "UNPAID" -> query.andWhere { OrdersTable.paidStatus eq "belum" }
                "PAID" -> query.andWhere { OrdersTable.paidStatus eq "lunas" }
                "QRIS" -> query.andWhere { OrdersTable.paymentMethod eq "qris" }
                "CASH" -> query.andWhere { OrdersTable.paymentMethod eq "cash" }
                else -> { /* No filter */ }
            }
        }
        
        query.orderBy(OrdersTable.id to SortOrder.DESC)
            .limit(size, offset = offset)
            .map {
                OrderData(
                    orderId = it[OrdersTable.id],
                    name = it[OrdersTable.name],
                    phoneNumber = it[OrdersTable.phoneNumber],
                    packageName = it[OrdersTable.packageName],
                    priceKg = it[OrdersTable.priceKg],
                    totalPrice = it[OrdersTable.totalPrice],
                    paidStatus = it[OrdersTable.paidStatus],
                    paymentMethod = it[OrdersTable.paymentMethod],
                    remark = it[OrdersTable.remark],
                    weight = it[OrdersTable.weight],
                    orderDate = it[OrdersTable.orderDate],
                    dueDate = it[OrdersTable.dueDate]
                )
            }
    }
}
