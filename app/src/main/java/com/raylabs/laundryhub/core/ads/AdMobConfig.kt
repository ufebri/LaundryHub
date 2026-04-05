package com.raylabs.laundryhub.core.ads

import com.raylabs.laundryhub.BuildConfig

object AdMobConfig {
    private const val PLACEHOLDER_PREFIX = "YOUR_"

    val hasConfiguredAppId: Boolean
        get() = BuildConfig.ADMOB_APP_ID.isConfiguredValue()

    val hasConfiguredBannerAdUnit: Boolean
        get() = BuildConfig.ADMOB_BANNER_AD_UNIT_ID.isConfiguredValue()

    val bannerAdUnitId: String
        get() = BuildConfig.ADMOB_BANNER_AD_UNIT_ID

    private fun String?.isConfiguredValue(): Boolean {
        if (this.isNullOrBlank()) return false
        return !startsWith(PLACEHOLDER_PREFIX)
    }
}
