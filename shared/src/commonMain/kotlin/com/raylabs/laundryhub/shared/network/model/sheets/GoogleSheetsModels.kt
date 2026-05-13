package com.raylabs.laundryhub.shared.network.model.sheets

import kotlinx.serialization.Serializable

@Serializable
data class ValueRange(
    val range: String? = null,
    val majorDimension: String? = null,
    val values: List<List<String>>? = null
)

@Serializable
data class UpdateValuesResponse(
    val spreadsheetId: String? = null,
    val updatedRange: String? = null,
    val updatedRows: Int? = null,
    val updatedColumns: Int? = null,
    val updatedCells: Int? = null,
    val updatedData: ValueRange? = null
)

@Serializable
data class AppendValuesResponse(
    val spreadsheetId: String? = null,
    val tableRange: String? = null,
    val updates: UpdateValuesResponse? = null
)

@Serializable
data class ClearValuesResponse(
    val spreadsheetId: String? = null,
    val clearedRange: String? = null
)

@Serializable
data class ClearValuesRequest(
    val dummy: String? = null
)

@Serializable
data class BatchUpdateSpreadsheetRequest(
    val requests: List<Request>
)

@Serializable
data class Request(
    val deleteDimension: DeleteDimensionRequest? = null
)

@Serializable
data class DeleteDimensionRequest(
    val range: DimensionRange
)

@Serializable
data class DimensionRange(
    val sheetId: Int,
    val dimension: String,
    val startIndex: Int,
    val endIndex: Int
)

@Serializable
data class BatchUpdateSpreadsheetResponse(
    val spreadsheetId: String? = null,
    val replies: List<Response>? = null
)

@Serializable
data class Response(
    val dummy: String? = null
)
