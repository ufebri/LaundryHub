package com.raylabs.laundryhub.ui.outcome.state

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.paymentMethodOutcomeList
import com.raylabs.laundryhub.ui.common.util.SectionState

data class OutcomeUiState(
    val lastOutcomeId: String? = null,
    val outcome: SectionState<List<DateListItemUI>> = SectionState(),
    val submitNewOutcome: SectionState<Boolean> = SectionState(),
    val updateOutcome: SectionState<Boolean> = SectionState(),

    //For Edit
    val outcomeID: String = "",
    val editOutcome: SectionState<OutcomeData> = SectionState(),

    //Flag form
    val paymentOption: List<String> = paymentMethodOutcomeList,

    //Form
    val name: String = "",
    val date: String = "",
    val price: String = "",
    val remark: String = "",
    val paymentStatus: String = "",

    //flag
    val isSubmitting: Boolean = false,
    val isEditMode: Boolean = false,
)

val OutcomeUiState.isSubmitEnabled: Boolean
    get() = name.isNotBlank()
            && date.isNotBlank()
            && price.isNotBlank()
            && paymentStatus.isNotBlank()

val OutcomeUiState.isUpdateEnabled: Boolean
    get() = outcomeID.isNotBlank()
            && name.isNotBlank()
            && date.isNotBlank()
            && price.isNotBlank()
            && paymentStatus.isNotBlank()