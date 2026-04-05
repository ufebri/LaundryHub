package com.raylabs.laundryhub.core.ads

import com.raylabs.laundryhub.BuildConfig

object AdMobConfig {
    private const val PLACEHOLDER_PREFIX = "YOUR_"

    val hasConfiguredAppId: Boolean
        get() = isConfiguredValue(BuildConfig.ADMOB_APP_ID)

    val hasConfiguredBannerAdUnit: Boolean
        get() = isConfiguredValue(BuildConfig.ADMOB_BANNER_AD_UNIT_ID)

    val bannerAdUnitId: String
        get() = BuildConfig.ADMOB_BANNER_AD_UNIT_ID

    internal fun isConfiguredValue(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        return !value.startsWith(PLACEHOLDER_PREFIX)
    }
}
