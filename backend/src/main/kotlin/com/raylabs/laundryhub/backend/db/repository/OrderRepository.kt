package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.OrdersTable
import com.raylabs.laundryhub.backend.plugins.dbQuery
import com.raylabs.laundryhub.backend.util.parseSupportedLaundryDate
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.isPaidStatusValue
import com.raylabs.laundryhub.core.domain.model.sheets.isUnpaidStatusValue
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.lowerCase
import java.util.Calendar
import java.util.Date

class OrderRepository {

    suspend fun insertWithNextId(order: OrderData): OrderData? = dbQuery {
        TransactionManager.current().exec("SELECT pg_advisory_xact_lock($ORDER_ID_ALLOCATION_LOCK_KEY)")
        val nextId = OrdersTable
            .slice(OrdersTable.id)
            .selectAll()
            .mapNotNull { it[OrdersTable.id].toIntOrNull() }
            .maxOrNull()
            ?.plus(1)
            ?.toString()
            ?: "0"
        val createdOrder = order.copy(orderId = nextId)
        val statement = OrdersTable.insertIgnore {
            it[id] = createdOrder.orderId
            it[name] = createdOrder.name
            it[phoneNumber] = createdOrder.phoneNumber
            it[packageName] = createdOrder.packageName
            it[priceKg] = createdOrder.priceKg
            it[totalPrice] = createdOrder.totalPrice
            it[paidStatus] = createdOrder.paidStatus
            it[paymentMethod] = createdOrder.paymentMethod
            it[remark] = createdOrder.remark
            it[weight] = createdOrder.weight
            it[orderDate] = createdOrder.orderDate
            it[dueDate] = createdOrder.dueDate
            it[isSynced] = false
        }
        if (statement.insertedCount > 0) createdOrder else null
    }

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

    suspend fun upsert(order: OrderData): Boolean = dbQuery {
        // Cek apakah order sudah ada
        val existing = OrdersTable.select { OrdersTable.id eq order.orderId }.singleOrNull()
        if (existing != null) {
            // Update
            val updatedCount = OrdersTable.update({ OrdersTable.id eq order.orderId }) {
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
                it[isSynced] = true // Data ini ditarik dari Sheets, jadi sudah tersinkronisasi
            }
            updatedCount > 0
        } else {
            // Insert
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
                it[isSynced] = true // Data ini ditarik dari Sheets
            }
            statement.insertedCount > 0
        }
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
                it[isSynced] = true
            }
            if (statement.insertedCount > 0) insertedCount++
        }
        insertedCount
    }

    suspend fun getById(orderId: String): OrderData? = dbQuery {
        OrdersTable
            .select { OrdersTable.id eq orderId }
            .singleOrNull()
            ?.toOrderData()
    }

    suspend fun getLatestId(): String = dbQuery {
        OrdersTable
            .slice(OrdersTable.id)
            .selectAll()
            .map { it[OrdersTable.id] }
            .maxByOrNull { it.toIntOrNull() ?: Int.MIN_VALUE }
            ?: "0"
    }

    suspend fun getNextId(): String = dbQuery {
        OrdersTable
            .slice(OrdersTable.id)
            .selectAll()
            .mapNotNull { it[OrdersTable.id].toIntOrNull() }
            .maxOrNull()
            ?.plus(1)
            ?.toString()
            ?: "0"
    }

    suspend fun getAll(
        page: Int = 1, 
        size: Int = 50, 
        filter: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        searchQuery: String? = null,
        sort: String? = null
    ): List<OrderData> = dbQuery {
        val offset = ((page - 1) * size).coerceAtLeast(0)

        // Date filtering and TODAY filter requires parsing multiple custom string formats,
        // so we use standard Kotlin JVM filtering as a fallback for accuracy.
        val isDateFiltering = !startDate.isNullOrBlank() || !endDate.isNullOrBlank() || filter?.uppercase() == "TODAY"

        val conditions = mutableListOf<org.jetbrains.exposed.sql.Op<Boolean>>()

        // Apply simple payment/status filter in SQL
        when (filter?.uppercase()) {
            "UNPAID" -> {
                conditions.add((OrdersTable.paidStatus.lowerCase() inList listOf("unpaid", "belum", "")) or (OrdersTable.paidStatus.isNull()))
            }
            "PAID" -> {
                conditions.add(OrdersTable.paidStatus.lowerCase() inList listOf("lunas", "paid", "paid by cash", "paid by qris"))
            }
            "QRIS" -> {
                conditions.add(OrdersTable.paymentMethod.lowerCase() inList listOf("qris", "paid by qris"))
            }
            "CASH" -> {
                conditions.add(OrdersTable.paymentMethod.lowerCase() inList listOf("cash", "paid by cash"))
            }
        }

        // Apply simple text search filter in SQL
        if (!searchQuery.isNullOrBlank()) {
            val q = searchQuery.trim().lowercase()
            conditions.add(
                (OrdersTable.name.lowerCase() like "%$q%") or
                (OrdersTable.id.lowerCase() like "%$q%") or
                (OrdersTable.phoneNumber.lowerCase() like "%$q%")
            )
        }

        var queryExpression: org.jetbrains.exposed.sql.Op<Boolean>? = null
        for (cond in conditions) {
            queryExpression = if (queryExpression == null) cond else queryExpression and cond
        }

        if (!isDateFiltering) {
            // Ultra-fast SQL level paging & sorting (O(page size) database query)
            val query = if (queryExpression != null) {
                OrdersTable.select { queryExpression }
            } else {
                OrdersTable.selectAll()
            }

            val sortOrder = when (sort) {
                "ORDER_DATE_ASC" -> OrdersTable.id to SortOrder.ASC
                "ORDER_DATE_DESC" -> OrdersTable.id to SortOrder.DESC
                "DUE_DATE_ASC" -> OrdersTable.dueDate to SortOrder.ASC
                "DUE_DATE_DESC" -> OrdersTable.dueDate to SortOrder.DESC
                else -> OrdersTable.id to SortOrder.DESC
            }

            query.orderBy(sortOrder)
                .limit(size, offset.toLong())
                .map { it.toOrderData() }
        } else {
            // Safe fallback with date range filtering and TODAY filtering in JVM memory
            val query = if (queryExpression != null) {
                OrdersTable.select { queryExpression }
            } else {
                OrdersTable.selectAll()
            }

            query.orderBy(OrdersTable.id to SortOrder.DESC)
                .map { it.toOrderData() }
                .asSequence()
                .filter { it.matchesFilter(filter) }
                .filter { it.matchesDateRange(startDate, endDate) }
                .sortedWith(orderComparator(sort))
                .drop(offset)
                .take(size)
                .toList()
        }
    }

    private fun ResultRow.toOrderData(): OrderData {
        return OrderData(
            orderId = this[OrdersTable.id],
            name = this[OrdersTable.name],
            phoneNumber = this[OrdersTable.phoneNumber],
            packageName = this[OrdersTable.packageName],
            priceKg = this[OrdersTable.priceKg],
            totalPrice = this[OrdersTable.totalPrice],
            paidStatus = this[OrdersTable.paidStatus],
            paymentMethod = this[OrdersTable.paymentMethod],
            remark = this[OrdersTable.remark],
            weight = this[OrdersTable.weight],
            orderDate = this[OrdersTable.orderDate],
            dueDate = this[OrdersTable.dueDate]
        )
    }

    private fun OrderData.matchesFilter(filter: String?): Boolean {
        return when (filter?.uppercase()) {
            null, "", "ALL" -> true
            "TODAY" -> isSameDay(orderDate, Date())
            "UNPAID" -> isUnpaidStatusValue(paidStatus, treatBlankAsUnpaid = true)
            "PAID" -> isPaidStatusValue(paidStatus)
            "QRIS" -> paymentMethod.equals("qris", ignoreCase = true)
            "CASH" -> paymentMethod.equals("cash", ignoreCase = true)
            else -> true
        }
    }

    private fun OrderData.matchesDateRange(startDate: String?, endDate: String?): Boolean {
        if (startDate.isNullOrBlank() && endDate.isNullOrBlank()) return true

        val orderTime = parseSupportedLaundryDate(orderDate)?.time ?: return false
        val startTime = startDate?.takeIf { it.isNotBlank() }?.let { parseSupportedLaundryDate(it)?.time }
        val endTime = endDate?.takeIf { it.isNotBlank() }?.let { parseSupportedLaundryDate(it)?.endOfDayTime() }

        return (startTime == null || orderTime >= startTime) &&
            (endTime == null || orderTime <= endTime)
    }

    private fun OrderData.matchesSearch(searchQuery: String?): Boolean {
        val query = searchQuery?.trim().orEmpty()
        if (query.isBlank()) return true

        return name.contains(query, ignoreCase = true) ||
            orderId.contains(query, ignoreCase = true) ||
            phoneNumber.contains(query, ignoreCase = true)
    }

    private fun orderComparator(sort: String?): Comparator<OrderData> {
        val idDesc = compareByDescending<OrderData> { it.orderId.toIntOrNull() ?: Int.MIN_VALUE }
        return when (sort) {
            "ORDER_DATE_ASC" -> compareBy<OrderData> { parseSupportedLaundryDate(it.orderDate)?.time ?: Long.MAX_VALUE }
                .thenBy { it.orderId.toIntOrNull() ?: Int.MAX_VALUE }
            "ORDER_DATE_DESC" -> compareByDescending<OrderData> { parseSupportedLaundryDate(it.orderDate)?.time ?: Long.MIN_VALUE }
                .then(idDesc)
            "DUE_DATE_ASC" -> compareBy<OrderData> { parseSupportedLaundryDate(it.dueDate)?.time ?: Long.MAX_VALUE }
                .thenBy { it.orderId.toIntOrNull() ?: Int.MAX_VALUE }
            "DUE_DATE_DESC" -> compareByDescending<OrderData> { parseSupportedLaundryDate(it.dueDate)?.time ?: Long.MIN_VALUE }
                .then(idDesc)
            else -> idDesc
        }
    }

    private fun isSameDay(left: String, right: Date): Boolean {
        val parsed = parseSupportedLaundryDate(left) ?: return false
        val leftCalendar = Calendar.getInstance().apply { time = parsed }
        val rightCalendar = Calendar.getInstance().apply { time = right }

        return leftCalendar.get(Calendar.YEAR) == rightCalendar.get(Calendar.YEAR) &&
            leftCalendar.get(Calendar.DAY_OF_YEAR) == rightCalendar.get(Calendar.DAY_OF_YEAR)
    }

    private fun Date.endOfDayTime(): Long {
        return Calendar.getInstance().apply {
            time = this@endOfDayTime
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}

private const val ORDER_ID_ALLOCATION_LOCK_KEY = 53319041
