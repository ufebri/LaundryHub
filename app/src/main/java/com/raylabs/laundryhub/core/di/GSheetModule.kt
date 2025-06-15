package com.raylabs.laundryhub.core.di

import android.content.Context
import com.raylabs.laundryhub.core.data.repository.GoogleSheetRepositoryImpl
import com.raylabs.laundryhub.core.data.service.GoogleSheetService
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.core.domain.usecase.sheets.CreateSpreadsheetDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.GetLastOrderIdUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.GetOtherPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadInventoryUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadOrderStatusUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadSpreadsheetDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.SubmitOrderUseCase
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
    fun provideCreateSpreadsheetDataUseCase(
        repository: GoogleSheetRepository
    ): CreateSpreadsheetDataUseCase {
        return CreateSpreadsheetDataUseCase(repository)
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
    fun provideReadOrderStatusUseCase(
        repository: GoogleSheetRepository
    ): ReadOrderStatusUseCase = ReadOrderStatusUseCase(repository)

    @Provides
    @ViewModelScoped
    fun provideReadInventoryUseCase(repository: GoogleSheetRepository): ReadInventoryUseCase =
        ReadInventoryUseCase(repository)

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
}
