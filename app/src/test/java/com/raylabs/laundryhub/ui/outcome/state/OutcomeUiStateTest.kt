package com.raylabs.laundryhub.ui.outcome.state

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutcomeUiStateTest {

    @Test
    fun `isSubmitEnabled true when all fields filled`() {
        val state = OutcomeUiState(
            name = "Pay rent",
            date = "01/01/2025",
            price = "1000",
            paymentStatus = "Paid by Cash"
        )

        assertTrue(state.isSubmitEnabled)
    }

    @Test
    fun `isSubmitEnabled false when any field blank`() {
        val state = OutcomeUiState(
            name = "",
            date = "01/01/2025",
            price = "1000",
            paymentStatus = "Paid by Cash"
        )

        assertFalse(state.isSubmitEnabled)
    }

    @Test
    fun `isUpdateEnabled true when outcomeID and fields filled`() {
        val state = OutcomeUiState(
            outcomeID = "10",
            name = "Update",
            date = "01/01/2025",
            price = "2000",
            paymentStatus = "Paid by Cash"
        )

        assertTrue(state.isUpdateEnabled)
    }

    @Test
    fun `isUpdateEnabled false when id missing`() {
        val state = OutcomeUiState(
            outcomeID = "",
            name = "Update",
            date = "01/01/2025",
            price = "2000",
            paymentStatus = "Paid by Cash"
        )

        assertFalse(state.isUpdateEnabled)
    }
}
