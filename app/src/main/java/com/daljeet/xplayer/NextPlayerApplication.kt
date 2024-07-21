package com.daljeet.xplayer

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.daljeet.xplayer.core.common.Utils
import com.daljeet.xplayer.core.common.di.ApplicationScope
import com.daljeet.xplayer.core.data.repository.PreferencesRepository
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Arrays
import javax.inject.Inject


@HiltAndroidApp
class NextPlayerApplication : Application(), Application.ActivityLifecycleCallbacks,
    LifecycleObserver {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    private lateinit var appOpenAdManager: AppOpenAdManager

    private val LOG_TAG = "AppOpenAdManager"
    private val AD_UNIT_ID = com.daljeet.xplayer.core.ui.R.string.app_open_id

    private var currentActivity: Activity? = null

    private var boolIsStarupAdshow = false;

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        checkClicked()
        MobileAds.initialize(this) {
        }
        val config= RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("1EB6EAD9715AB41F3696FCC31586020F")).build()

        MobileAds.setRequestConfiguration(config)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()
        initRewardedAds()
        // Preloading updated preferences to ensure that the player uses the latest preferences set by the user.
        // This resolves the issue where player use default preferences upon launching the app from a cold start.
        // See [the corresponding issue for more info](https://github.com/anilbeesetti/nextplayer/issues/392)
        applicationScope.launch {
            preferencesRepository.applicationPreferences.first()
            preferencesRepository.playerPreferences.first()
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        Log.d(LOG_TAG, "On Move Foreground.")
        currentActivity?.let { appOpenAdManager.showAdIfAvailable(it) }

    }

    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication
        // class.
        Log.d(LOG_TAG, "Load Ad from activity.")

        appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener)
    }

    fun loadAd(activity: Activity) {
        Log.d(LOG_TAG, "Load Ad please.")

        // We wrap the loadAd to enforce that other classes only interact with MyApplication
        // class.
        appOpenAdManager.loadAd(activity)
    }

    private inner class AppOpenAdManager {
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false

        /** Request an ad. */
        fun loadAd(context: Context) {
            // Do not load ad if there is an unused ad or one is already loading.
            if (isLoadingAd || isAdAvailable()) {
                return
            }

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context, getString(AD_UNIT_ID), request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {

                    override fun onAdLoaded(ad: AppOpenAd) {
                        // Called when an app open ad has loaded.
                        Log.d(LOG_TAG, "Ad was loaded.")
                        appOpenAd = ad
                        isLoadingAd = false
                        if (!boolIsStarupAdshow) {
                            Log.d(LOG_TAG, "Showing ad first time.")

                            currentActivity?.let { showAdIfAvailable(it) }
                            boolIsStarupAdshow = true
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Called when an app open ad has failed to load.
                        Log.d(LOG_TAG, loadAdError.message)
                        isLoadingAd = false;
                    }
                })
        }

        /** Check if ad exists and can be shown. */
        private fun isAdAvailable(): Boolean {
            return appOpenAd != null
        }

        fun showAdIfAvailable(activity: Activity) {
            showAdIfAvailable(
                activity,
                object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                        // Empty because the user will go back to the activity that shows the ad.
                    }
                }
            )
        }

        fun showAdIfAvailable(
            activity: Activity,
            onShowAdCompleteListener: OnShowAdCompleteListener
        ) {
            Log.d(LOG_TAG, "showAdIfAvailable")

            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) {
                Log.d(LOG_TAG, "The app open ad is already showing.")
                return
            }

            // If the app open ad is not available yet, invoke the callback then load the ad.
            if (!isAdAvailable()) {
                Log.d(LOG_TAG, "The app open ad is not ready yet.")
                onShowAdCompleteListener.onShowAdComplete()
                loadAd(activity)
                return
            }

            appOpenAd?.setFullScreenContentCallback(
                object : FullScreenContentCallback() {

                    override fun onAdDismissedFullScreenContent() {
                        // Called when full screen content is dismissed.
                        // Set the reference to null so isAdAvailable() returns false.
                        Log.d(LOG_TAG, "Ad dismissed fullscreen content.")
                        appOpenAd = null
                        isShowingAd = false

                        onShowAdCompleteListener.onShowAdComplete()
                        loadAd(activity)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        // Called when fullscreen content failed to show.
                        // Set the reference to null so isAdAvailable() returns false.
                        Log.d(LOG_TAG, adError.message)
                        appOpenAd = null
                        isShowingAd = false

                        onShowAdCompleteListener.onShowAdComplete()
                        loadAd(activity)
                    }

                    override fun onAdShowedFullScreenContent() {
                        // Called when fullscreen content is shown.
                        Log.d(LOG_TAG, "Ad showed fullscreen content.")
                    }
                })
            isShowingAd = true
            appOpenAd?.show(activity)
        }
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
        // Updating the currentActivity only when an ad is not showing.
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(p0: Activity) {
    }

    override fun onActivityPaused(p0: Activity) {
    }

    override fun onActivityStopped(p0: Activity) {
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
    }

    override fun onActivityDestroyed(p0: Activity) {
    }

    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    private var rewardedAd: RewardedAd? = null
    var adRequest: AdRequest? = null
    private fun initRewardedAds() {
        adRequest = AdRequest.Builder().build()
        loadRewardAd()
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(LOG_TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                Log.d(LOG_TAG, "Ad dismissed fullscreen content.")
                rewardedAd = null
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                // Called when ad fails to show.
                Log.e(LOG_TAG, "Ad failed to show fullscreen content.")
                rewardedAd = null
            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(LOG_TAG, "Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(LOG_TAG, "Ad showed fullscreen content.")
            }
        }

    }

    fun loadRewardAd() {
        if (adRequest == null) {
            initRewardedAds()
        }
        adRequest?.let {
            RewardedAd.load(
                this,
                resources.getString(com.daljeet.xplayer.core.ui.R.string.rewarded_id),
                it,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Timber.tag(LOG_TAG).d(adError?.toString())
                        rewardedAd = null
                    }

                    override fun onAdLoaded(ad: RewardedAd) {
                        Log.d(LOG_TAG, "Reward Ad was loaded.")
                        rewardedAd = ad
                    }
                })
        }
    }

    fun showRewardedAdIfLoaded() {
        rewardedAd?.let { ad ->
            currentActivity?.let {
                ad.show(it, OnUserEarnedRewardListener { rewardItem ->
                    // Handle the reward.
                    val rewardAmount = rewardItem.amount
                    val rewardType = rewardItem.type
                    Log.d(LOG_TAG, "User earned the reward.")
                })
            }
        } ?: run {
            Log.d(LOG_TAG, "The rewarded ad wasn't ready yet.")
        }
        loadRewardAd()
    }


    private fun checkClicked() {
        val uiScope = CoroutineScope(Dispatchers.Main)
        uiScope.launch {
            Utils.menuclick.observeForever {
                showRewardedAdIfLoaded()
            }
        }
    }

}