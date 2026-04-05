package com.raylabs.laundryhub.ui.component

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.core.ads.AdMobConfig
import java.util.concurrent.atomic.AtomicInteger

@Stable
class InlineAdaptiveBannerAdState internal constructor(
    private val placementName: String
) {
    private val stateId = nextBannerStateId.incrementAndGet()
    private var adViewState = mutableStateOf<AdView?>(null)
    private var shouldShowState = mutableStateOf(AdMobConfig.hasConfiguredBannerAdUnit)
    private var configuredWidth = 0
    private var lastAttemptAtMs = 0L
    private var lastFailureCode: Int? = null

    val debugLabel: String
        get() = "placement=$placementName stateId=$stateId"

    val adView: AdView?
        get() = adViewState.value

    val shouldShowBanner: Boolean
        get() = shouldShowState.value

    fun ensureAdView(context: Context, adWidth: Int) {
        if (!AdMobConfig.hasConfiguredBannerAdUnit) {
            Log.d(TAG, "Skip ensureAdView $debugLabel reason=config_missing")
            return
        }
        if (adWidth <= 0) {
            Log.d(TAG, "Skip ensureAdView $debugLabel reason=invalid_width width=$adWidth")
            return
        }
        if (!shouldShowState.value) {
            val now = System.currentTimeMillis()
            val isNoFillFailure = lastFailureCode == LoadAdErrorCode.NO_FILL
            val retryReady = isNoFillFailure && now - lastAttemptAtMs >= NO_FILL_RETRY_MS
            if (!retryReady) {
                Log.d(
                    TAG,
                    "Skip ensureAdView $debugLabel reason=hidden_after_failure lastFailureCode=$lastFailureCode msSinceAttempt=${now - lastAttemptAtMs}"
                )
                return
            }
            Log.d(
                TAG,
                "Retry after no fill $debugLabel msSinceAttempt=${now - lastAttemptAtMs}"
            )
            shouldShowState.value = true
        }
        if (adViewState.value != null && configuredWidth == adWidth) {
            Log.d(TAG, "Reuse existing AdView $debugLabel width=$adWidth")
            return
        }

        Log.d(
            TAG,
            "Create or replace AdView $debugLabel previousWidth=$configuredWidth newWidth=$adWidth hadExisting=${adViewState.value != null}"
        )
        adViewState.value?.destroy()
        configuredWidth = adWidth
        shouldShowState.value = true
        lastAttemptAtMs = System.currentTimeMillis()
        lastFailureCode = null

        adViewState.value = AdView(context).apply {
            adUnitId = AdMobConfig.bannerAdUnitId
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth))
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    lastFailureCode = null
                    Log.d(TAG, "Banner loaded $debugLabel width=$adWidth")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Banner impression $debugLabel width=$adWidth")
                }

                override fun onAdOpened() {
                    Log.d(TAG, "Banner opened $debugLabel")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Banner clicked $debugLabel")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "Banner closed $debugLabel")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    lastAttemptAtMs = System.currentTimeMillis()
                    lastFailureCode = loadAdError.code
                    Log.w(
                        TAG,
                        "Banner failed $debugLabel code=${loadAdError.code} message=${loadAdError.message}"
                    )
                    adViewState.value?.destroy()
                    shouldShowState.value = false
                    adViewState.value = null
                }
            }
            Log.d(TAG, "Loading banner request $debugLabel width=$adWidth")
            loadAd(AdRequest.Builder().build())
        }
    }

    fun onHostResume() {
        Log.d(TAG, "Host resume $debugLabel hasAdView=${adViewState.value != null}")
        adViewState.value?.resume()
    }

    fun onHostPause() {
        Log.d(TAG, "Host pause $debugLabel hasAdView=${adViewState.value != null}")
        adViewState.value?.pause()
    }

    fun release(reason: String) {
        Log.d(TAG, "Release AdView $debugLabel reason=$reason hadAdView=${adViewState.value != null}")
        adViewState.value?.destroy()
        adViewState.value = null
        configuredWidth = 0
    }
}

@Composable
fun rememberInlineAdaptiveBannerAdState(
    placementName: String
): InlineAdaptiveBannerAdState {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember(placementName) { InlineAdaptiveBannerAdState(placementName) }

    DisposableEffect(lifecycleOwner, state) {
        Log.d(TAG, "Attach banner lifecycle observer ${state.debugLabel}")
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> state.onHostResume()
                Lifecycle.Event.ON_PAUSE -> state.onHostPause()
                Lifecycle.Event.ON_DESTROY -> state.release(reason = "lifecycle_destroy")
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            Log.d(TAG, "Dispose banner lifecycle observer ${state.debugLabel}")
            lifecycleOwner.lifecycle.removeObserver(observer)
            state.release(reason = "composition_dispose")
        }
    }

    return state
}

@Composable
fun InlineAdaptiveBannerAd(
    placementName: String,
    modifier: Modifier = Modifier
) {
    val state = rememberInlineAdaptiveBannerAdState(placementName)
    InlineAdaptiveBannerAd(state = state, modifier = modifier)
}

@Composable
fun InlineAdaptiveBannerAd(
    state: InlineAdaptiveBannerAdState,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        InlineAdaptiveBannerPreview(modifier = modifier)
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val adWidth = maxWidth.value.toInt().coerceAtLeast(1)
        LaunchedEffect(state, adWidth) {
            Log.d(TAG, "Inline slot active ${state.debugLabel} slotWidth=$adWidth")
        }
        DisposableEffect(state, adWidth) {
            Log.d(TAG, "Attach inline slot ${state.debugLabel} slotWidth=$adWidth")
            onDispose {
                Log.d(TAG, "Dispose inline slot ${state.debugLabel} slotWidth=$adWidth")
            }
        }
        state.ensureAdView(context = context, adWidth = adWidth)

        if (state.shouldShowBanner && state.adView != null) {
            val adView = state.adView ?: return@BoxWithConstraints
            val previousParent = adView.parent as? ViewGroup
            if (previousParent != null) {
                Log.d(TAG, "Detach AdView from previous parent ${state.debugLabel}")
                previousParent.removeView(adView)
            }

            AndroidView(
                factory = { adView },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            Log.d(
                TAG,
                "Banner not rendered ${state.debugLabel} shouldShow=${state.shouldShowBanner} hasAdView=${state.adView != null}"
            )
        }
    }
}

@Composable
private fun InlineAdaptiveBannerPreview(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(72.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.ad_preview_label),
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private const val TAG = "InlineAdaptiveBanner"
private const val NO_FILL_RETRY_MS = 30_000L
private val nextBannerStateId = AtomicInteger(0)

private object LoadAdErrorCode {
    const val NO_FILL = 3
}
