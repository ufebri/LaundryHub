package com.raylabs.laundryhub.core.domain.model.sheets

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OutcomeDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val sampleOutcome = OutcomeData(
        id = "out-1",
        date = "2026-06-01",
        purpose = "Soap",
        price = "15000",
        remark = "None",
        payment = "qris"
    )

    @Test
    fun testToOutcomeList() {
        val map = mapOf(
            "id" to "out-99",
            "date" to "2026-06-02",
            "keperluan" to "Hanger",
            "price" to "25000",
            "remark" to "Plastic hanger",
            "payment" to "cash"
        )

        val outcome = map.toOutcomeList()
        assertEquals("out-99", outcome.id)
        assertEquals("2026-06-02", outcome.date)
        assertEquals("Hanger", outcome.purpose)
        assertEquals("25000", outcome.price)
        assertEquals("Plastic hanger", outcome.remark)
        assertEquals("cash", outcome.payment)
    }

    @Test
    fun testOutcomeSheetValues() {
        val sheetValues = sampleOutcome.toSheetValues()
        assertEquals(1, sheetValues.size)
        assertEquals("out-1", sheetValues[0][0])
        assertEquals("2026-06-01", sheetValues[0][1])
        assertEquals("Soap", sheetValues[0][2])
        assertEquals("15000", sheetValues[0][3])
        assertEquals("None", sheetValues[0][4])
        assertEquals("qris", sheetValues[0][5])

        // Blank date fallback
        val blankDateOutcome = sampleOutcome.copy(date = "")
        val blankValues = blankDateOutcome.toSheetValues()
        assertTrue(blankValues[0][1].isNotBlank())
    }

    @Test
    fun testOutcomeUpdateSheetValues() {
        val updateValues = sampleOutcome.toUpdateSheetValues("2026-05-30")
        assertEquals("2026-06-01", updateValues[0][1])

        val blankDateOutcome = sampleOutcome.copy(date = "")
        val blankUpdateValues = blankDateOutcome.toUpdateSheetValues("2026-05-30")
        assertEquals("2026-05-30", blankUpdateValues[0][1])

        val blankUpdateValuesFallback = blankDateOutcome.toUpdateSheetValues("")
        assertTrue(blankUpdateValuesFallback[0][1].isNotBlank())
    }

    @Test
    fun testPaymentTypeEnum() {
        // fromValue
        assertEquals(PaymentType.QRIS, PaymentType.fromValue("qris"))
        assertEquals(PaymentType.CASH, PaymentType.fromValue("cash"))
        assertEquals(PaymentType.PERSONAL, PaymentType.fromValue("personal"))
        assertNull(PaymentType.fromValue("invalid"))

        // fromDescription
        assertEquals(PaymentType.QRIS, PaymentType.fromDescription(PAID_BY_QRIS))
        assertEquals(PaymentType.CASH, PaymentType.fromDescription(PAID_BY_CASH))
        assertEquals(PaymentType.PERSONAL, PaymentType.fromDescription(PAID_BY_PERSONAL))
        assertNull(PaymentType.fromDescription("invalid"))

        // Values/Description helper values
        assertEquals("qris", PaymentType.QRIS.value)
        assertEquals(PAID_BY_QRIS, PaymentType.QRIS.description)
    }

    @Test
    fun testOutcomePaidDescriptionHelpers() {
        assertEquals(PAID_BY_QRIS, sampleOutcome.paidDescription())
        assertEquals(PAID_BY_CASH, sampleOutcome.copy(payment = "cash").paidDescription())
        assertEquals("", sampleOutcome.copy(payment = "invalid").paidDescription())

        assertEquals("qris", getPaymentValueFromDescription(PAID_BY_QRIS))
        assertEquals("cash", getPaymentValueFromDescription(PAID_BY_CASH))
        assertEquals("personal", getPaymentValueFromDescription(PAID_BY_PERSONAL))
        assertEquals("", getPaymentValueFromDescription("invalid"))
    }

    @Test
    fun testSerialization() {
        val serialized = json.encodeToString(sampleOutcome)
        val deserialized = json.decodeFromString<OutcomeData>(serialized)
        assertEquals(sampleOutcome, deserialized)

        val response = CreateOutcomeResponse("SUCCESS", "Created successfully", "out-1")
        val serializedRes = json.encodeToString(response)
        val deserializedRes = json.decodeFromString<CreateOutcomeResponse>(serializedRes)
        assertEquals(response, deserializedRes)
    }
}
