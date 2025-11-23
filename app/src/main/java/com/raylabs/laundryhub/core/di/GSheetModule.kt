package com.raylabs.laundryhub.core.di

import android.content.Context
import com.raylabs.laundryhub.core.data.repository.GoogleSheetRepositoryImpl
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.core.domain.usecase.sheets.GetOtherPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadSpreadsheetDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.GetLastOrderIdUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.GetOrderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.SubmitOrderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.UpdateOrderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.GetLastOutcomeIdUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.GetOutcomeUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.ReadOutcomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.SubmitOutcomeUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.UpdateOutcomeUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object GSheetModule {

    @Provides
    @ViewModelScoped
    fun provideGoogleSheetRepository(
        service: GoogleSheetService
    ): GoogleSheetRepository {
        return GoogleSheetRepositoryImpl(service)
    }

    @Provides
    @ViewModelScoped
    fun provideReadSpreadsheetDataUseCase(
        repository: GoogleSheetRepository
    ): ReadSpreadsheetDataUseCase {
        return ReadSpreadsheetDataUseCase(repository)
    }

    @Provides
    @ViewModelScoped
    fun provideReadIncomeDataUseCase(
        repository: GoogleSheetRepository
    ): ReadIncomeTransactionUseCase = ReadIncomeTransactionUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideReadPackageUseCase(repository: GoogleSheetRepository): ReadPackageUseCase =
        ReadPackageUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideGetOtherPackageUseCase(repository: GoogleSheetRepository): GetOtherPackageUseCase =
        GetOtherPackageUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideGetLastOrderIdUseCase(repository: GoogleSheetRepository): GetLastOrderIdUseCase =
        GetLastOrderIdUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideSubmitOrderUseCase(repository: GoogleSheetRepository): SubmitOrderUseCase =
        SubmitOrderUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideGoogleSheetService(@ApplicationContext context: Context): GoogleSheetService {
        // Inject aplikasi context langsung ke GoogleSheetService
        return GoogleSheetService(context)
    }

    @Provides
    @ViewModelScoped
    fun provideUpdateOrderUseCase(repository: GoogleSheetRepository): UpdateOrderUseCase {
        return UpdateOrderUseCase(repository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetOrderUseCase(repository: GoogleSheetRepository): GetOrderUseCase {
        return GetOrderUseCase(repository)
    }


    //Outcome
    @Provides
    @ViewModelScoped
    fun provideGetLastOutcomeIdUseCase(repository: GoogleSheetRepository): GetLastOutcomeIdUseCase =
        GetLastOutcomeIdUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideUpdateOutcomeUseCase(repository: GoogleSheetRepository): UpdateOutcomeUseCase {
        return UpdateOutcomeUseCase(repository)
    }

    @Provides
    @ViewModelScoped
    fun provideSubmitOutcomeUseCase(repository: GoogleSheetRepository): SubmitOutcomeUseCase =
        SubmitOutcomeUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideReadOutcomeDataUseCase(
        repository: GoogleSheetRepository
    ): ReadOutcomeTransactionUseCase = ReadOutcomeTransactionUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideGetOutcomeUseCase(repository: GoogleSheetRepository): GetOutcomeUseCase {
        return GetOutcomeUseCase(repository)
    }
}
