package com.raylabs.laundryhub.ui.preview

import androidx.lifecycle.ViewModel
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PreviewViewModel : ViewModel() {

    private val _financialSummary = MutableStateFlow<Resource<List<SpreadsheetData>>>(
        Resource.Success(
            listOf(
                SpreadsheetData("Total Income", "Rp 3.500.000"),
                SpreadsheetData("Total Outcome", "Rp 1.200.000"),
                SpreadsheetData("Net Profit", "Rp 2.300.000")
            )
        )
    )

    private val _incomeTransaction = MutableStateFlow<Resource<List<TransactionData>>>(
        Resource.Success(
            listOf(
                TransactionData(
                    "1",
                    "2025-05-18",
                    "Laundry Express",
                    "10",
                    "100000",
                    "Paid",
                    "Normal",
                    "Clean",
                    "Cash"
                ),
                TransactionData(
                    "1",
                    "2025-05-19",
                    "Laundry Deluxe",
                    "15",
                    "150000",
                    "Unpaid",
                    "Premium",
                    "Ironed",
                    "Transfer"
                )
            )
        )
    )

}
