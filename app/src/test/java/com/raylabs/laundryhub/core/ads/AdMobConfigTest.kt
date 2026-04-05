package com.raylabs.laundryhub.core.ads

import com.raylabs.laundryhub.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdMobConfigTest {

    @Test
    fun `isConfiguredValue returns false for null blank and placeholder values`() {
        assertFalse(AdMobConfig.isConfiguredValue(null))
        assertFalse(AdMobConfig.isConfiguredValue(""))
        assertFalse(AdMobConfig.isConfiguredValue("   "))
        assertFalse(AdMobConfig.isConfiguredValue("YOUR_APP_ID"))
        assertFalse(AdMobConfig.isConfiguredValue("YOUR_BANNER_ID"))
    }

    @Test
    fun `isConfiguredValue returns true for real values`() {
        assertTrue(AdMobConfig.isConfiguredValue("ca-app-pub-3940256099942544~3347511713"))
        assertTrue(AdMobConfig.isConfiguredValue("ca-app-pub-3940256099942544/6300978111"))
    }

    @Test
    fun `public properties mirror BuildConfig values`() {
        assertEquals(BuildConfig.ADMOB_BANNER_AD_UNIT_ID, AdMobConfig.bannerAdUnitId)
        assertEquals(
            AdMobConfig.isConfiguredValue(BuildConfig.ADMOB_APP_ID),
            AdMobConfig.hasConfiguredAppId
        )
        assertEquals(
            AdMobConfig.isConfiguredValue(BuildConfig.ADMOB_BANNER_AD_UNIT_ID),
            AdMobConfig.hasConfiguredBannerAdUnit
        )
    }
}
