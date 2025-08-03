package com.raylabs.laundryhub.ui.onboarding.state

import com.raylabs.laundryhub.R
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingPageTest {

    @Test
    fun `getListOnboardingPage contains correct number of items`() {
        assertEquals(3, getListOnboardingPage.size)
    }

    @Test
    fun `first onboarding page has correct data`() {
        val page = getListOnboardingPage[0]
        assertEquals("Pantau Order Lebih Mudah", page.title)
        assertEquals(
            "Lihat semua pesanan masuk, status, dan riwayat transaksi dalam satu aplikasi.",
            page.description
        )
        assertEquals(R.raw.lottie_tracking_order, page.lottieAsset)
    }

    @Test
    fun `second onboarding page has correct data`() {
        val page = getListOnboardingPage[1]
        assertEquals("Kelola Mesin & Paket", page.title)
        assertEquals(
            "Atur data mesin, jenis layanan, dan harga paket secara fleksibel.",
            page.description
        )
        assertEquals(R.raw.lottie_manage_machine, page.lottieAsset)
    }

    @Test
    fun `third onboarding page has correct data`() {
        val page = getListOnboardingPage[2]
        assertEquals("Otomatisasi Laporan", page.title)
        assertEquals(
            "Semua data transaksi otomatis tercatat. Cek laporan harian dan mingguan kapan saja.",
            page.description
        )
        assertEquals(R.raw.lottie_report, page.lottieAsset)
    }
}