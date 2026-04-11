package com.raylabs.laundryhub

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.raylabs.laundryhub.core.ads.AdMobConfig
import com.raylabs.laundryhub.core.reminder.ReminderNotificationConfig
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class LaundryHubApp : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        ReminderNotificationConfig.ensureChannel(this)
        if (!AdMobConfig.hasConfiguredAppId) {
            Log.d(TAG, "Skipping Mobile Ads initialization because the current build uses an AdMob placeholder or blank app ID")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@LaundryHubApp) {}
        }
    }

    private companion object {
        const val TAG = "LaundryHubApp"
    }
}
