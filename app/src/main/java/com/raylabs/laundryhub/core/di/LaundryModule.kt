package com.raylabs.laundryhub.core.di

import com.raylabs.laundryhub.core.data.repository.LaundryRepositoryImpl
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.core.domain.usecase.sheets.*
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.*
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LaundryModule {

    @Provides
    @Singleton
    fun provideLaundryRepository(): LaundryRepository {
        return LaundryRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideReadSpreadsheetDataUseCase(repository: LaundryRepository) = ReadSpreadsheetDataUseCase(repository)

    @Provides
    @Singleton
    fun provideReadGrossDataUseCase(repository: LaundryRepository) = ReadGrossDataUseCase(repository)

    @Provides
    @Singleton
    fun provideReadIncomeDataUseCase(repository: LaundryRepository) = ReadIncomeTransactionUseCase(repository)

    @Provides
    @Singleton
    fun provideReadPackageUseCase(repository: LaundryRepository) = ReadPackageUseCase(repository)

    @Provides
    @Singleton
    fun provideGetOtherPackageUseCase(repository: LaundryRepository) = GetOtherPackageUseCase(repository)

    @Provides
    @Singleton
    fun provideSubmitPackageUseCase(repository: LaundryRepository) = SubmitPackageUseCase(repository)

    @Provides
    @Singleton
    fun provideUpdatePackageUseCase(repository: LaundryRepository) = UpdatePackageUseCase(repository)

    @Provides
    @Singleton
    fun provideDeletePackageUseCase(repository: LaundryRepository) = DeletePackageUseCase(repository)

    @Provides
    @Singleton
    fun provideGetLastOrderIdUseCase(repository: LaundryRepository) = GetLastOrderIdUseCase(repository)

    @Provides
    @Singleton
    fun provideSubmitOrderUseCase(repository: LaundryRepository) = SubmitOrderUseCase(repository)

    @Provides
    @Singleton
    fun provideUpdateOrderUseCase(repository: LaundryRepository) = UpdateOrderUseCase(repository)

    @Provides
    @Singleton
    fun provideGetOrderUseCase(repository: LaundryRepository) = GetOrderUseCase(repository)

    @Provides
    @Singleton
    fun provideDeleteOrderUseCase(repository: LaundryRepository) = DeleteOrderUseCase(repository)

    @Provides
    @Singleton
    fun provideGetLastOutcomeIdUseCase(repository: LaundryRepository) = GetLastOutcomeIdUseCase(repository)

    @Provides
    @Singleton
    fun provideUpdateOutcomeUseCase(repository: LaundryRepository) = UpdateOutcomeUseCase(repository)

    @Provides
    @Singleton
    fun provideSubmitOutcomeUseCase(repository: LaundryRepository) = SubmitOutcomeUseCase(repository)

    @Provides
    @Singleton
    fun provideReadOutcomeDataUseCase(repository: LaundryRepository) = ReadOutcomeTransactionUseCase(repository)

    @Provides
    @Singleton
    fun provideGetOutcomeUseCase(repository: LaundryRepository) = GetOutcomeUseCase(repository)

    @Provides
    @Singleton
    fun provideDeleteOutcomeUseCase(repository: LaundryRepository) = DeleteOutcomeUseCase(repository)
}
