package com.raylabs.laundryhub.ui.common.dummy.profile

import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.profile.state.ConnectedSpreadsheetItem
import com.raylabs.laundryhub.ui.profile.state.ProfileUiState
import com.raylabs.laundryhub.ui.profile.state.UserItem

val dummyProfileUiState = ProfileUiState(
    user = SectionState(
        data = UserItem(
            displayName = "Ray Febri",
            email = "rayfebri@example.com",
            photoUrl = null
        )
    ),
    logout = SectionState(data = false),
    showWhatsAppOption = true,
    connectedSpreadsheet = SectionState(
        data = ConnectedSpreadsheetItem(
            spreadsheetId = "1AbCdEfGhIjKlMnOpQrStUvWxYz1234567890",
            spreadsheetName = "LaundryHub Template",
            spreadsheetUrl = "https://docs.google.com/spreadsheets/d/1AbCdEfGhIjKlMnOpQrStUvWxYz1234567890/edit"
        )
    ),
    cacheSize = SectionState(data = 5_242_880L),
    clearCache = SectionState(data = false)
)
