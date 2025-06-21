package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
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
class GetOtherPackageUseCaseTest {
    private lateinit var repository: GoogleSheetRepository
    private lateinit var useCase: GetOtherPackageUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = GetOtherPackageUseCase(repository)
    }

    @Test
    fun `returns error if remarkResult is error`() = runTest {
        whenever(repository.readOtherPackage()).thenReturn(Resource.Error("err1"))
        whenever(repository.readPackageData()).thenReturn(Resource.Success(listOf()))
        val result = useCase.invoke()
        assertTrue(result is Resource.Error)
        assertEquals("err1", (result as Resource.Error).message)
    }

    @Test
    fun `returns error if packageResult is error`() = runTest {
        whenever(repository.readOtherPackage()).thenReturn(Resource.Success(listOf("A")))
        whenever(repository.readPackageData()).thenReturn(Resource.Error("err2"))
        val result = useCase.invoke()
        assertTrue(result is Resource.Error)
        assertEquals("err2", (result as Resource.Error).message)
    }

    @Test
    fun `returns empty if no other packages`() = runTest {
        whenever(repository.readOtherPackage()).thenReturn(Resource.Success(listOf("A", "B")))
        whenever(repository.readPackageData()).thenReturn(Resource.Success(listOf(
            PackageData("1", "A", "", ""),
            PackageData("2", "B", "", "")
        )))
        val result = useCase.invoke()
        assertTrue(result is Resource.Empty)
    }

    @Test
    fun `returns success with filtered other packages`() = runTest {
        whenever(repository.readOtherPackage()).thenReturn(Resource.Success(listOf("A", "B", "C", "", "C", "  D  ")))
        whenever(repository.readPackageData()).thenReturn(Resource.Success(listOf(
            PackageData("1", "A", "", ""),
            PackageData("2", "B", "", "")
        )))
        val result = useCase.invoke()
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(listOf("C", "D"), data)
    }
}

