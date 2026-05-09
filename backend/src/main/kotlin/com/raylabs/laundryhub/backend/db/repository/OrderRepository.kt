package com.raylabs.laundryhub.backend.db.repository

import com.raylabs.laundryhub.backend.db.schema.OrdersTable
import com.raylabs.laundryhub.backend.plugins.dbQuery
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
            .selectAll()
            .map { it[OrdersTable.id] }
            .maxByOrNull { it.toIntOrNull() ?: Int.MIN_VALUE }
            ?: "0"
    }

    suspend fun getNextId(): String = dbQuery {
        OrdersTable
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

        OrdersTable
            .selectAll()
            .map { it.toOrderData() }
            .asSequence()
            .filter { it.matchesFilter(filter) }
            .filter { it.matchesDateRange(startDate, endDate) }
            .filter { it.matchesSearch(searchQuery) }
            .sortedWith(orderComparator(sort))
            .drop(offset)
            .take(size)
            .toList()
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
        return when (filter) {
            null, "", "ALL" -> true
            "TODAY" -> isSameDay(orderDate, Date())
            "UNPAID" -> paidStatus.equals("belum", ignoreCase = true) || paidStatus.isBlank()
            "PAID" -> paidStatus.equals("lunas", ignoreCase = true)
            "QRIS" -> paymentMethod.equals("qris", ignoreCase = true)
            "CASH" -> paymentMethod.equals("cash", ignoreCase = true)
            else -> true
        }
    }

    private fun OrderData.matchesDateRange(startDate: String?, endDate: String?): Boolean {
        if (startDate.isNullOrBlank() && endDate.isNullOrBlank()) return true

        val orderTime = parseSupportedDate(orderDate)?.time ?: return false
        val startTime = startDate?.takeIf { it.isNotBlank() }?.let { parseSupportedDate(it)?.time }
        val endTime = endDate?.takeIf { it.isNotBlank() }?.let { parseSupportedDate(it)?.endOfDayTime() }

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
            "ORDER_DATE_ASC" -> compareBy<OrderData> { parseSupportedDate(it.orderDate)?.time ?: Long.MAX_VALUE }
                .thenBy { it.orderId.toIntOrNull() ?: Int.MAX_VALUE }
            "ORDER_DATE_DESC" -> compareByDescending<OrderData> { parseSupportedDate(it.orderDate)?.time ?: Long.MIN_VALUE }
                .then(idDesc)
            "DUE_DATE_ASC" -> compareBy<OrderData> { parseSupportedDate(it.dueDate)?.time ?: Long.MAX_VALUE }
                .thenBy { it.orderId.toIntOrNull() ?: Int.MAX_VALUE }
            "DUE_DATE_DESC" -> compareByDescending<OrderData> { parseSupportedDate(it.dueDate)?.time ?: Long.MIN_VALUE }
                .then(idDesc)
            else -> idDesc
        }
    }

    private fun isSameDay(left: String, right: Date): Boolean {
        val parsed = parseSupportedDate(left) ?: return false
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

    private fun parseSupportedDate(value: String?): Date? {
        val sanitized = value?.trim().orEmpty()
        if (sanitized.isBlank()) return null

        val formats = listOf(
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm",
            "dd-MM-yyyy HH:mm",
            "yyyy-MM-dd HH:mm"
        )

        return formats.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.getDefault()).apply {
                    isLenient = false
                }.parse(sanitized)
            }.getOrNull()
        }
    }
}
