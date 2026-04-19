package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.DeleteOrderUseCase
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteOrderUseCaseTest {

    private lateinit var repository: GoogleSheetRepository
    private lateinit var useCase: DeleteOrderUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = DeleteOrderUseCase(repository)
    }

    @Test
    fun `invoke returns success when repository deleteOrder is successful`() = runTest {
        whenever(repository.deleteOrder("ORD1")).thenReturn(Resource.Success(true))

        val result = useCase(orderId = "ORD1")

        assertTrue(result is Resource.Success)
        assertEquals(true, (result as Resource.Success).data)
    }

    @Test
    fun `invoke returns error when repository deleteOrder fails`() = runTest {
        whenever(repository.deleteOrder("ORD1")).thenReturn(Resource.Error("Failed"))

        val result = useCase(orderId = "ORD1")

        assertTrue(result is Resource.Error)
        assertEquals("Failed", (result as Resource.Error).message)
    }

    @Test
    fun `invoke returns default error when repository deleteOrder returns null`() = runTest {
        whenever(repository.deleteOrder("ORD1")).thenReturn(null)

        val result = useCase(orderId = "ORD1")

        assertTrue(result is Resource.Error)
        assertEquals("Failed to submit data", (result as Resource.Error).message)
    }
}
