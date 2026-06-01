package com.raylabs.laundryhub.shared.network.model.sheets

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class GoogleSheetsModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testValueRange() {
        val model = ValueRange("Sheet1!A1", "ROWS", listOf(listOf("value1")))
        assertEquals("Sheet1!A1", model.range)
        assertEquals("ROWS", model.majorDimension)
        assertEquals(listOf(listOf("value1")), model.values)

        val serialized = json.encodeToString(model)
        val deserialized = json.decodeFromString<ValueRange>(serialized)
        assertEquals(model, deserialized)
    }

    @Test
    fun testUpdateValuesResponse() {
        val valueRange = ValueRange("Sheet1!A1", "ROWS", listOf(listOf("value1")))
        val model = UpdateValuesResponse("ssId", "Sheet1!A1", 1, 1, 1, valueRange)
        
        assertEquals("ssId", model.spreadsheetId)
        assertEquals("Sheet1!A1", model.updatedRange)
        assertEquals(1, model.updatedRows)
        assertEquals(1, model.updatedColumns)
        assertEquals(1, model.updatedCells)
        assertEquals(valueRange, model.updatedData)

        val serialized = json.encodeToString(model)
        val deserialized = json.decodeFromString<UpdateValuesResponse>(serialized)
        assertEquals(model, deserialized)
    }

    @Test
    fun testAppendValuesResponse() {
        val valueRange = ValueRange("Sheet1!A1", "ROWS", listOf(listOf("value1")))
        val updateResponse = UpdateValuesResponse("ssId", "Sheet1!A1", 1, 1, 1, valueRange)
        val model = AppendValuesResponse("ssId", "Sheet1!A1:A5", updateResponse)

        assertEquals("ssId", model.spreadsheetId)
        assertEquals("Sheet1!A1:A5", model.tableRange)
        assertEquals(updateResponse, model.updates)

        val serialized = json.encodeToString(model)
        val deserialized = json.decodeFromString<AppendValuesResponse>(serialized)
        assertEquals(model, deserialized)
    }

    @Test
    fun testClearValuesResponse() {
        val model = ClearValuesResponse("ssId", "Sheet1!A1")
        assertEquals("ssId", model.spreadsheetId)
        assertEquals("Sheet1!A1", model.clearedRange)

        val serialized = json.encodeToString(model)
        val deserialized = json.decodeFromString<ClearValuesResponse>(serialized)
        assertEquals(model, deserialized)
    }

    @Test
    fun testClearValuesRequest() {
        val model = ClearValuesRequest("dummy")
        assertEquals("dummy", model.dummy)

        val serialized = json.encodeToString(model)
        val deserialized = json.decodeFromString<ClearValuesRequest>(serialized)
        assertEquals(model, deserialized)
    }

    @Test
    fun testBatchUpdateValuesRequestAndResponse() {
        val valueRange = ValueRange("Sheet1!A1", "ROWS", listOf(listOf("value1")))
        val request = BatchUpdateValuesRequest("RAW", listOf(valueRange))
        assertEquals("RAW", request.valueInputOption)
        assertEquals(listOf(valueRange), request.data)

        val reqSerialized = json.encodeToString(request)
        val reqDeserialized = json.decodeFromString<BatchUpdateValuesRequest>(reqSerialized)
        assertEquals(request, reqDeserialized)

        val updateResponse = UpdateValuesResponse("ssId", "Sheet1!A1", 1, 1, 1, valueRange)
        val response = BatchUpdateValuesResponse("ssId", 1, 1, 1, 1, listOf(updateResponse))
        assertEquals("ssId", response.spreadsheetId)
        assertEquals(1, response.totalUpdatedRows)
        assertEquals(1, response.totalUpdatedColumns)
        assertEquals(1, response.totalUpdatedCells)
        assertEquals(1, response.totalUpdatedSheets)
        assertEquals(listOf(updateResponse), response.responses)

        val resSerialized = json.encodeToString(response)
        val resDeserialized = json.decodeFromString<BatchUpdateValuesResponse>(resSerialized)
        assertEquals(response, resDeserialized)
    }

    @Test
    fun testBatchClearValuesRequestAndResponse() {
        val request = BatchClearValuesRequest(listOf("Sheet1!A1"))
        assertEquals(listOf("Sheet1!A1"), request.ranges)

        val reqSerialized = json.encodeToString(request)
        val reqDeserialized = json.decodeFromString<BatchClearValuesRequest>(reqSerialized)
        assertEquals(request, reqDeserialized)

        val response = BatchClearValuesResponse("ssId", listOf("Sheet1!A1"))
        assertEquals("ssId", response.spreadsheetId)
        assertEquals(listOf("Sheet1!A1"), response.clearedRanges)

        val resSerialized = json.encodeToString(response)
        val resDeserialized = json.decodeFromString<BatchClearValuesResponse>(resSerialized)
        assertEquals(response, resDeserialized)
    }

    @Test
    fun testBatchUpdateSpreadsheetRequestAndResponse() {
        val range = DimensionRange(1, "ROWS", 0, 5)
        assertEquals(1, range.sheetId)
        assertEquals("ROWS", range.dimension)
        assertEquals(0, range.startIndex)
        assertEquals(5, range.endIndex)

        val deleteReq = DeleteDimensionRequest(range)
        assertEquals(range, deleteReq.range)

        val requestItem = Request(deleteReq)
        assertEquals(deleteReq, requestItem.deleteDimension)

        val batchReq = BatchUpdateSpreadsheetRequest(listOf(requestItem))
        assertEquals(listOf(requestItem), batchReq.requests)

        val reqSerialized = json.encodeToString(batchReq)
        val reqDeserialized = json.decodeFromString<BatchUpdateSpreadsheetRequest>(reqSerialized)
        assertEquals(batchReq, reqDeserialized)

        val replyItem = Response("reply")
        assertEquals("reply", replyItem.dummy)

        val batchRes = BatchUpdateSpreadsheetResponse("ssId", listOf(replyItem))
        assertEquals("ssId", batchRes.spreadsheetId)
        assertEquals(listOf(replyItem), batchRes.replies)

        val resSerialized = json.encodeToString(batchRes)
        val resDeserialized = json.decodeFromString<BatchUpdateSpreadsheetResponse>(resSerialized)
        assertEquals(batchRes, resDeserialized)
    }
}
