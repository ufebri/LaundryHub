package com.raylabs.laundryhub.ui.onboarding.state

import androidx.annotation.RawRes
import com.raylabs.laundryhub.R

data class OnboardingPage(
    val title: String,
    val description: String,
    @RawRes val lottieAsset: Int
)

val getListOnboardingPage: List<OnboardingPage> = listOf(
    OnboardingPage(
        title = "Pantau Order Lebih Mudah",
        description = "Lihat semua pesanan masuk, status, dan riwayat transaksi dalam satu aplikasi.",
        lottieAsset = R.raw.lottie_tracking_order // Gambar animasi dashboard/monitor
    ),
    OnboardingPage(
        title = "Kelola Mesin & Paket",
        description = "Atur data mesin, jenis layanan, dan harga paket secara fleksibel.",
        lottieAsset = R.raw.lottie_manage_machine // Gambar animasi mesin laundry
    ),
    OnboardingPage(
        title = "Otomatisasi Laporan",
        description = "Semua data transaksi otomatis tercatat. Cek laporan harian dan mingguan kapan saja.",
        lottieAsset = R.raw.lottie_report // Gambar animasi laporan keuangan
    )
)