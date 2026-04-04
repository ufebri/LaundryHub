package com.raylabs.laundryhub.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class GSheetRepositoryErrorHandlingTest {

    @Test
    fun `handleReadSheetResponseException maps drive api disabled message`() {
        val result = GSheetRepositoryErrorHandling.handleReadSheetResponseException(
            Exception(
                "403 Forbidden. Google Drive API has not been used in project 655099386324 before or it is disabled. Enable it by visiting https://console.developers.google.com/apis/api/drive.googleapis.com/overview?project=655099386324 then retry."
            )
        )

        assertEquals(
            GSheetRepositoryErrorHandling.DRIVE_API_NOT_ENABLED_MESSAGE,
            result.message
        )
    }
}
