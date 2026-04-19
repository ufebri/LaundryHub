package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageMutationValidationTest {

    @Test
    fun `validatePackageForSave requires package name`() {
        val result = validatePackageForSave(
            PackageData(price = "5000", name = "", duration = "3d", unit = "kg")
        )

        assertEquals("Package name is required.", result?.message)
    }

    @Test
    fun `validatePackageForSave requires package price`() {
        val result = validatePackageForSave(
            PackageData(price = "", name = "Regular", duration = "3d", unit = "kg")
        )

        assertEquals("Package price is required.", result?.message)
    }

    @Test
    fun `validatePackageForSave requires package duration`() {
        val result = validatePackageForSave(
            PackageData(price = "5000", name = "Regular", duration = "", unit = "kg")
        )

        assertEquals("Package duration is required.", result?.message)
    }

    @Test
    fun `validatePackageForSave requires package unit`() {
        val result = validatePackageForSave(
            PackageData(price = "5000", name = "Regular", duration = "3d", unit = "")
        )

        assertEquals("Package unit is required.", result?.message)
    }

    @Test
    fun `validatePackageForSave returns null for complete data`() {
        val result = validatePackageForSave(
            PackageData(price = "5000", name = "Regular", duration = "3d", unit = "kg")
        )

        assertNull(result)
    }

    @Test
    fun `hasDuplicatePackageName matches case insensitively and normalizes spacing`() {
        val result = hasDuplicatePackageName(
            packageName = " express   - 6h ",
            existingPackages = listOf(
                PackageData(
                    price = "10000",
                    name = "Express - 6H",
                    duration = "6h",
                    unit = "kg",
                    sheetRowIndex = 2
                )
            )
        )

        assertTrue(result)
    }

    @Test
    fun `hasDuplicatePackageName ignores excluded row`() {
        val result = hasDuplicatePackageName(
            packageName = "Express - 6H",
            existingPackages = listOf(
                PackageData(
                    price = "10000",
                    name = "Express - 6H",
                    duration = "6h",
                    unit = "kg",
                    sheetRowIndex = 4
                )
            ),
            excludingRowIndex = 4
        )

        assertFalse(result)
    }
}
