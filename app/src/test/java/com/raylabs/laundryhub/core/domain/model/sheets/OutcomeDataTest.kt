package com.raylabs.laundryhub.core.domain.model.sheets

import com.raylabs.laundryhub.ui.common.util.DateUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OutcomeDataTest {

    @Test
    fun `toOutcomeList maps missing values to empty strings`() {
        val result = emptyMap<String, String>().toOutcomeList()

        assertEquals(
            OutcomeData(
                id = "",
                date = "",
                purpose = "",
                price = "",
                remark = "",
                payment = ""
            ),
            result
        )
    }

    @Test
    fun `toSheetValues uses fallback current date when outcome date is blank`() {
        val row = sampleOutcomeData()
            .copy(date = "")
            .toSheetValues()
            .single()

        assertEquals(DateUtil.getTodayDate(DateUtil.STANDARD_DATE_FORMATED), row[1])
    }

    @Test
    fun `toUpdateSheetValues uses existing date when outcome date is blank`() {
        val row = sampleOutcomeData()
            .copy(date = "")
            .toUpdateSheetValues(existingDate = "12/04/2026")
            .single()

        assertEquals("12/04/2026", row[1])
    }

    @Test
    fun `payment type companion maps known values and descriptions`() {
        assertEquals(PaymentType.CASH, PaymentType.fromValue("cash"))
        assertEquals(PaymentType.QRIS, PaymentType.fromDescription(PAID_BY_QRIS))
        assertNull(PaymentType.fromValue("unknown"))
        assertNull(PaymentType.fromDescription("Unknown"))
    }

    @Test
    fun `paidDescription and payment value mapping stay symmetric`() {
        val outcome = sampleOutcomeData().copy(payment = "cash")

        assertEquals(PAID_BY_CASH, outcome.paidDescription())
        assertEquals("qris", getPaymentValueFromDescription(PAID_BY_QRIS))
        assertEquals("", getPaymentValueFromDescription("Other"))
    }

    private fun sampleOutcomeData(): OutcomeData {
        return OutcomeData(
            id = "OUT-1",
            date = "11/04/2026",
            purpose = "Soap",
            price = "18000",
            remark = "Restock",
            payment = "cash"
        )
    }
}
