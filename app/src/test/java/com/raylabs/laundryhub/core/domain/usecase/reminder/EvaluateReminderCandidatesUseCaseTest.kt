package com.raylabs.laundryhub.core.domain.usecase.reminder

import com.raylabs.laundryhub.core.domain.model.reminder.ReminderBucket
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderLocalState
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EvaluateReminderCandidatesUseCaseTest {

    private lateinit var useCase: EvaluateReminderCandidatesUseCase

    @Before
    fun setUp() {
        useCase = EvaluateReminderCandidatesUseCase()
    }

    @Test
    fun `maps due today and overdue orders into expected buckets`() {
        val nowMillis = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.APRIL, 8, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val transactions = listOf(
            transaction(orderId = "1", dueDate = "08/04/2026"),
            transaction(orderId = "2", dueDate = "06/04/2026"),
            transaction(orderId = "3", dueDate = "03/04/2026"),
            transaction(orderId = "4", dueDate = "01/04/2026"),
            transaction(orderId = "5", dueDate = "20/03/2026"),
            transaction(orderId = "6", dueDate = "10/03/2026"),
            transaction(orderId = "7", dueDate = "01/03/2026"),
            transaction(orderId = "8", dueDate = "10/04/2026")
        )

        val result = useCase(
            transactions = transactions,
            localStates = emptyMap(),
            nowMillis = nowMillis
        )

        assertEquals(7, result.size)
        assertEquals(ReminderBucket.DUE_TODAY, result.first { it.orderId == "1" }.bucket)
        assertEquals(ReminderBucket.OVERDUE_1_TO_2_DAYS, result.first { it.orderId == "2" }.bucket)
        assertEquals(ReminderBucket.OVERDUE_3_TO_6_DAYS, result.first { it.orderId == "3" }.bucket)
        assertEquals(ReminderBucket.OVERDUE_1_WEEK, result.first { it.orderId == "4" }.bucket)
        assertEquals(ReminderBucket.OVERDUE_2_WEEKS, result.first { it.orderId == "5" }.bucket)
        assertEquals(ReminderBucket.OVERDUE_3_WEEKS, result.first { it.orderId == "6" }.bucket)
        assertEquals(ReminderBucket.OVERDUE_1_MONTH_PLUS, result.first { it.orderId == "7" }.bucket)
        assertTrue(result.none { it.orderId == "8" })
    }

    @Test
    fun `filters resolved and snoozed reminder items`() {
        val nowMillis = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.APRIL, 8, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val tomorrowMillis = nowMillis + 24L * 60L * 60L * 1000L

        val transactions = listOf(
            transaction(orderId = "checked", dueDate = "07/04/2026"),
            transaction(orderId = "picked", dueDate = "07/04/2026"),
            transaction(orderId = "dismissed", dueDate = "07/04/2026"),
            transaction(orderId = "snoozed", dueDate = "07/04/2026"),
            transaction(orderId = "visible", dueDate = "07/04/2026")
        )

        val result = useCase(
            transactions = transactions,
            localStates = mapOf(
                "checked" to ReminderLocalState(checkedAtEpochMillis = nowMillis),
                "picked" to ReminderLocalState(assumedPickedUpAtEpochMillis = nowMillis),
                "dismissed" to ReminderLocalState(dismissedAtEpochMillis = nowMillis),
                "snoozed" to ReminderLocalState(snoozedUntilEpochMillis = tomorrowMillis)
            ),
            nowMillis = nowMillis
        )

        assertEquals(listOf("visible"), result.map { it.orderId })
    }

    @Test
    fun `ignores transactions with blank due date`() {
        val nowMillis = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.APRIL, 8, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val result = useCase(
            transactions = listOf(
                transaction(orderId = "blank", dueDate = "   "),
                transaction(orderId = "valid", dueDate = "07/04/2026")
            ),
            localStates = emptyMap(),
            nowMillis = nowMillis
        )

        assertEquals(listOf("valid"), result.map { it.orderId })
    }

    @Test
    fun `supports legacy due date formats used by the app`() {
        val nowMillis = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.APRIL, 8, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val result = useCase(
            transactions = listOf(
                transaction(orderId = "slash", dueDate = "07/04/2026"),
                transaction(orderId = "dash", dueDate = "07-04-2026"),
                transaction(orderId = "iso", dueDate = "2026-04-07")
            ),
            localStates = emptyMap(),
            nowMillis = nowMillis
        )

        assertEquals(listOf("dash", "iso", "slash"), result.map { it.orderId }.sorted())
        assertEquals(listOf("07/04/2026"), result.map { it.dueDate }.distinct())
    }

    private fun transaction(orderId: String, dueDate: String): TransactionData {
        return TransactionData(
            orderID = orderId,
            date = "01/04/2026",
            name = "Customer $orderId",
            weight = "2",
            pricePerKg = "12000",
            totalPrice = "24000",
            paymentStatus = "lunas",
            packageType = "Regular",
            remark = "",
            paymentMethod = "cash",
            phoneNumber = "08123",
            dueDate = dueDate
        )
    }
}
