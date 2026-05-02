package com.raylabs.laundryhub.core.di

import com.raylabs.laundryhub.core.data.repository.GoogleSheetRepositoryImpl
import com.raylabs.laundryhub.core.data.repository.SpreadsheetValidationRepositoryImpl
import com.raylabs.laundryhub.core.data.service.GoogleSheetsAuthorizationManager
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetIdProvider
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetValidationRepository
import com.raylabs.laundryhub.core.domain.usecase.settings.ValidateSpreadsheetUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.*
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.*
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.*
import com.raylabs.laundryhub.shared.network.HttpClientProvider
import com.raylabs.laundryhub.shared.network.api.GoogleSheetsApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GSheetModule {

    @Provides
    @Singleton
    fun provideGoogleSheetsApiClient(): GoogleSheetsApiClient {
        return GoogleSheetsApiClient(HttpClientProvider.createClient())
    }

    @Provides
    @Singleton
    fun provideGoogleSheetRepository(
        apiClient: GoogleSheetsApiClient,
        authManager: GoogleSheetsAuthorizationManager,
        spreadsheetIdProvider: SpreadsheetIdProvider
    ): GoogleSheetRepository {
        return GoogleSheetRepositoryImpl(apiClient, authManager, spreadsheetIdProvider)
    }

    @Provides
    @Singleton
    fun provideReadSpreadsheetDataUseCase(repository: GoogleSheetRepository) = ReadSpreadsheetDataUseCase(repository)

    @Provides
    @Singleton
    fun provideReadGrossDataUseCase(repository: GoogleSheetRepository) = ReadGrossDataUseCase(repository)

    @Provides
    @Singleton
    fun provideReadIncomeDataUseCase(repository: GoogleSheetRepository) = ReadIncomeTransactionUseCase(repository)

    @Provides
    @Singleton
    fun provideReadPackageUseCase(repository: GoogleSheetRepository) = ReadPackageUseCase(repository)

    @Provides
    @Singleton
    fun provideGetOtherPackageUseCase(repository: GoogleSheetRepository) = GetOtherPackageUseCase(repository)

    @Provides
    @Singleton
    fun provideSubmitPackageUseCase(repository: GoogleSheetRepository) = SubmitPackageUseCase(repository)

    @Provides
    @Singleton
    fun provideUpdatePackageUseCase(repository: GoogleSheetRepository) = UpdatePackageUseCase(repository)

    @Provides
    @Singleton
    fun provideDeletePackageUseCase(repository: GoogleSheetRepository) = DeletePackageUseCase(repository)

    @Provides
    @Singleton
    fun provideGetLastOrderIdUseCase(repository: GoogleSheetRepository) = GetLastOrderIdUseCase(repository)

    @Provides
    @Singleton
    fun provideSubmitOrderUseCase(repository: GoogleSheetRepository) = SubmitOrderUseCase(repository)

    @Provides
    @Singleton
    fun provideUpdateOrderUseCase(repository: GoogleSheetRepository) = UpdateOrderUseCase(repository)

    @Provides
    @Singleton
    fun provideGetOrderUseCase(repository: GoogleSheetRepository) = GetOrderUseCase(repository)

    @Provides
    @Singleton
    fun provideDeleteOrderUseCase(repository: GoogleSheetRepository) = DeleteOrderUseCase(repository)

    @Provides
    @Singleton
    fun provideGetLastOutcomeIdUseCase(repository: GoogleSheetRepository) = GetLastOutcomeIdUseCase(repository)

    @Provides
    @Singleton
    fun provideUpdateOutcomeUseCase(repository: GoogleSheetRepository) = UpdateOutcomeUseCase(repository)

    @Provides
    @Singleton
    fun provideSubmitOutcomeUseCase(repository: GoogleSheetRepository) = SubmitOutcomeUseCase(repository)

    @Provides
    @Singleton
    fun provideReadOutcomeDataUseCase(repository: GoogleSheetRepository) = ReadOutcomeTransactionUseCase(repository)

    @Provides
    @Singleton
    fun provideGetOutcomeUseCase(repository: GoogleSheetRepository) = GetOutcomeUseCase(repository)

    @Provides
    @Singleton
    fun provideDeleteOutcomeUseCase(repository: GoogleSheetRepository) = DeleteOutcomeUseCase(repository)

    @Provides
    @Singleton
    fun provideSpreadsheetValidationRepository(
        apiClient: GoogleSheetsApiClient,
        authManager: GoogleSheetsAuthorizationManager
    ): SpreadsheetValidationRepository = SpreadsheetValidationRepositoryImpl(apiClient, authManager)

    @Provides
    @Singleton
    fun provideValidateSpreadsheetUseCase(
        repository: SpreadsheetValidationRepository
    ): ValidateSpreadsheetUseCase = ValidateSpreadsheetUseCase(repository)
}
