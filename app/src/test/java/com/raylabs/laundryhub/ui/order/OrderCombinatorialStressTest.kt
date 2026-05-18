package com.raylabs.laundryhub.ui.order

import com.raylabs.laundryhub.core.domain.usecase.settings.ObserveShowWhatsAppSettingUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadPackageUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.GetOrderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.SubmitOrderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.UpdateOrderUseCase
import com.raylabs.laundryhub.ui.order.state.isSubmitEnabled
import com.raylabs.laundryhub.ui.order.state.toOrderData
import com.raylabs.laundryhub.ui.profile.inventory.state.PackageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class OrderCombinatorialStressTest(
    private val name: String,
    private val phone: String,
    private val selectedPackage: PackageItem?,
    private val price: String,
    private val paymentMethod: String,
    private val orderDate: String
) {

    private val testDispatcher = StandardTestDispatcher()
    private val mockSubmitOrderUseCase: SubmitOrderUseCase = mock()
    private val mockPackageListUseCase: ReadPackageUseCase = mock()
    private val mockGetOrderByIdUseCase: GetOrderUseCase = mock()
    private val mockUpdateOrderUseCase: UpdateOrderUseCase = mock()
    private val mockObserveShowWhatsAppSettingUseCase: ObserveShowWhatsAppSettingUseCase = mock()

    private lateinit var vm: OrderViewModel

    companion object {
        private val results = mutableListOf<TestResult>()

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: name={0}, phone={1}, pkg={2}, price={3}, pay={4}, date={5}")
        fun data(): Collection<Array<Any?>> {
            val names = listOf("", "Valid Name", "A".repeat(31))
            val phones = listOf("", "812345678", "0812345678")
            val packages = listOf(null, PackageItem("Reg", "5000", "3d"))
            val prices = listOf("", "0", "10000")
            val paymentMethods = listOf("", "Paid by Cash")
            val orderDates = listOf("", "01/01/2025")

            val combinations = mutableListOf<Array<Any?>>()
            for (n in names) {
                for (ph in phones) {
                    for (pkg in packages) {
                        for (pr in prices) {
                            for (pm in paymentMethods) {
                                for (od in orderDates) {
                                    combinations.add(arrayOf(n, ph, pkg, pr, pm, od))
                                }
                            }
                        }
                    }
                }
            }
            return combinations
        }

        @AfterClass
        @JvmStatic
        fun printReport() {
            println("\n=== Order Combinatorial Stress Test Report ===")
            println("Total combinations tested: ${results.size}")
            println("Successful submissions: ${results.count { it.isSubmitEnabled && it.mappedCorrectly }}")
            println("Failed validations: ${results.count { !it.isSubmitEnabled }}")
            
            println("\n--- Detailed Calculation Matrix (Sample) ---")
            results.take(20).forEach { 
                println("Input: [${it.name}, ${it.price}, ${it.packageName}] -> Enabled: ${it.isSubmitEnabled}, Weight: ${it.weight}")
            }

            println("\n--- Edge Case Discoveries ---")
            val longNames = results.filter { it.name.length > 30 }
            if (longNames.isNotEmpty()) {
                println("Found ${longNames.size} cases with name > 30 chars. UI should enforce 30-char limit as ViewModel doesn't truncate.")
            }
            
            val zeroPrices = results.filter { it.price == "0" && it.isSubmitEnabled }
            if (zeroPrices.isNotEmpty()) {
                println("Warning: Found ${zeroPrices.size} cases where price is '0' but submission is enabled.")
            }

            val defaultDates = results.filter { it.orderDate == com.raylabs.laundryhub.ui.common.util.DateUtil.getTodayDate("dd/MM/yyyy") && it.isSubmitEnabled }
            if (defaultDates.isNotEmpty()) {
                println("Note: ${defaultDates.size} cases used the default 'Today' date.")
            }

            val cleanedPhones = results.filter { it.phoneInput.startsWith("0") && !it.phoneOutput.startsWith("0") }
            if (cleanedPhones.isNotEmpty()) {
                println("Verification: ${cleanedPhones.size} cases correctly cleaned leading zero from phone numbers.")
            }
            println("==============================================\n")
        }
    }

    data class TestResult(
        val name: String,
        val price: String,
        val packageName: String,
        val isSubmitEnabled: Boolean,
        val weight: String,
        val orderDate: String,
        val mappedCorrectly: Boolean,
        val phoneInput: String,
        val phoneOutput: String
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(mockObserveShowWhatsAppSettingUseCase.invoke()).thenReturn(flowOf(true))
        vm = OrderViewModel(
            mockSubmitOrderUseCase,
            mockPackageListUseCase,
            mockGetOrderByIdUseCase,
            mockUpdateOrderUseCase,
            mockObserveShowWhatsAppSettingUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test add order combination`() = runTest {
        // Apply inputs to ViewModel
        vm.updateField("name", name)
        vm.onPhoneChanged(phone)
        selectedPackage?.let { vm.onPackageSelected(it) }
        vm.onPriceChanged(price)
        vm.updateField("paymentMethod", paymentMethod)
        if (orderDate.isNotEmpty()) {
            vm.onOrderDateSelected(orderDate)
        }

        val state = vm.uiState.value
        val isEnabled = state.isSubmitEnabled
        
        // Expected isSubmitEnabled logic:
        // name.isNotBlank() && selectedPackage != null && price.isNotBlank() && paymentMethod.isNotBlank()
        val expectedEnabled = name.isNotBlank() 
                && selectedPackage != null 
                && price.isNotBlank() 
                && paymentMethod.isNotBlank()
        
        assertEquals("Submit enabled mismatch for $name, $price, ${selectedPackage?.name}", expectedEnabled, isEnabled)

        // Test mapping to OrderData
        val orderData = state.toOrderData("test-id")
        val mappedCorrectly = orderData.name == name && 
                orderData.totalPrice == price &&
                orderData.packageName == (selectedPackage?.name ?: "")

        results.add(TestResult(
            name = name,
            price = price,
            packageName = selectedPackage?.name ?: "None",
            isSubmitEnabled = isEnabled,
            weight = state.weight,
            orderDate = state.orderDate,
            mappedCorrectly = mappedCorrectly,
            phoneInput = phone,
            phoneOutput = state.phone
        ))
    }
}
