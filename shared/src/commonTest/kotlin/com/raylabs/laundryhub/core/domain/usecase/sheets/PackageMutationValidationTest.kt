package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PackageMutationValidationTest {

    @Test
    fun validatePackageForSave_returnsError_whenFieldsAreBlank() {
        val basePackage = PackageData(name = "", price = "7000", duration = "1d", unit = "kg")
        assertEquals("Package name is required.", validatePackageForSave(basePackage)?.message)

        assertEquals("Package price is required.", validatePackageForSave(basePackage.copy(name = "Test", price = ""))?.message)
        assertEquals("Package duration is required.", validatePackageForSave(basePackage.copy(name = "Test", duration = ""))?.message)
        assertEquals("Package unit is required.", validatePackageForSave(basePackage.copy(name = "Test", unit = ""))?.message)
    }

    @Test
    fun validatePackageForSave_returnsNull_whenAllFieldsArePresent() {
        val validPackage = PackageData(name = "Test", price = "7000", duration = "1d", unit = "kg")
        assertNull(validatePackageForSave(validPackage))
    }

    @Test
    fun hasDuplicatePackageName_detectsDuplicates() {
        val existing = listOf(
            PackageData(name = "Reguler", price = "3000", duration = "3d", unit = "kg", sheetRowIndex = 2, id = 1),
            PackageData(name = "Express", price = "7000", duration = "1d", unit = "kg", sheetRowIndex = 3, id = 2)
        )

        assertTrue(hasDuplicatePackageName("Reguler", existing))
        assertTrue(hasDuplicatePackageName("reguler ", existing))
        assertTrue(hasDuplicatePackageName(" EXPRESS ", existing))
        assertFalse(hasDuplicatePackageName("New Package", existing))
    }

    @Test
    fun hasDuplicatePackageName_respectsExclusions() {
        val existing = listOf(
            PackageData(name = "Reguler", price = "3000", duration = "3d", unit = "kg", sheetRowIndex = 2, id = 1),
            PackageData(name = "Express", price = "7000", duration = "1d", unit = "kg", sheetRowIndex = 3, id = 2)
        )

        // Exclude by row index
        assertFalse(hasDuplicatePackageName("Reguler", existing, excludingRowIndex = 2))
        
        // Exclude by id
        assertFalse(hasDuplicatePackageName("Express", existing, excludingId = 2))

        // No exclusion
        assertTrue(hasDuplicatePackageName("Reguler", existing, excludingRowIndex = 3))
    }
}
