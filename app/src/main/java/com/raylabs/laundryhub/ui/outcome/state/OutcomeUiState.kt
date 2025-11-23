package com.raylabs.laundryhub.ui.outcome.state

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.ui.common.util.SectionState

data class OutcomeUiState(
    val lastOutcomeId: String? = null,
    val outcome: SectionState<List<DateListItemUI>> = SectionState(),
    val submitNewOutcome: SectionState<Boolean> = SectionState(),
    val updateOutcome: SectionState<Boolean> = SectionState(),
    val isEditMode: Boolean = false,

    //For Edit
    val outcomeID: String = "",
    val editOutcome: SectionState<OutcomeData> = SectionState(),


    //Form
    val name: String = "",
    val date: String = "",
    val price: String = "",
    val remark: String = "",
    val paymentStatus: String = "",

    //flag
    val isSubmitting: Boolean = false
)