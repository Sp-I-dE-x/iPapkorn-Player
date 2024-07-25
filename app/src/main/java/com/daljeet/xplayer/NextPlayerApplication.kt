package com.daljeet.xplayer

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.*
import com.daljeet.xplayer.core.common.Utils
import com.daljeet.xplayer.core.common.di.ApplicationScope
import com.daljeet.xplayer.core.data.repository.PreferencesRepository
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltAndroidApp
class NextPlayerApplication : Application(), Application.ActivityLifecycleCallbacks, LifecycleObserver {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    private lateinit var appOpenAdManager: AppOpenAdManager

    private val LOG_TAG = "AppOpenAdManager"
    private val AD_UNIT_ID = com.daljeet.xplayer.core.ui.R.string.app_open_id
    private val REWARDED_AD_UNIT_ID = com.daljeet.xplayer.core.ui.R.string.rewarded_id

    private var currentActivity: Activity? = null
    private var isStartupAdShown = false
    private var rewardedAd: RewardedAd? = null
    private var adRequest: AdRequest? = null

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        checkClicked()
        initializeMobileAds()
        initializePreferences()
        setupTimber()
    }

    private fun initializeMobileAds() {
        MobileAds.initialize(this) {}
        val config = RequestConfiguration.Builder()
            .setTestDeviceIds(Arrays.asList("1EB6EAD9715AB41F3696FCC31586020F")).build()
        MobileAds.setRequestConfiguration(config)
        appOpenAdManager = AppOpenAdManager()
        initRewardedAds()
    }

    private fun initializePreferences() {
        applicationScope.launch {
            preferencesRepository.applicationPreferences.first()
            preferencesRepository.playerPreferences.first()
        }
    }

    private fun setupTimber() {
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
        Log.d(LOG_TAG, "Load Ad from activity.")
        appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener)
    }

    fun loadAd(activity: Activity) {
        Log.d(LOG_TAG, "Load Ad please.")
        appOpenAdManager.loadAd(activity)
    }

    private inner class AppOpenAdManager {
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false

        fun loadAd(context: Context) {
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
                        Log.d(LOG_TAG, "Ad was loaded.")
                        appOpenAd = ad
                        isLoadingAd = false
                        if (!isStartupAdShown) {
                            Log.d(LOG_TAG, "Showing ad first time.")
                            currentActivity?.let { showAdIfAvailable(it) }
                            isStartupAdShown = true
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.d(LOG_TAG, loadAdError.message)
                        isLoadingAd = false
                    }
                })
        }

        private fun isAdAvailable(): Boolean {
            return appOpenAd != null
        }

        fun showAdIfAvailable(activity: Activity) {
            showAdIfAvailable(activity, object : OnShowAdCompleteListener {
                override fun onShowAdComplete() {}
            })
        }

        fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
            Log.d(LOG_TAG, "showAdIfAvailable")
            if (isShowingAd) {
                Log.d(LOG_TAG, "The app open ad is already showing.")
                return
            }
            if (!isAdAvailable()) {
                Log.d(LOG_TAG, "The app open ad is not ready yet.")
                onShowAdCompleteListener.onShowAdComplete()
                loadAd(activity)
                return
            }

            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(LOG_TAG, "Ad dismissed fullscreen content.")
                    appOpenAd = null
                    isShowingAd = false
                    onShowAdCompleteListener.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(LOG_TAG, adError.message)
                    appOpenAd = null
                    isShowingAd = false
                    onShowAdCompleteListener.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(LOG_TAG, "Ad showed fullscreen content.")
                }
            }
            isShowingAd = true
            appOpenAd?.show(activity)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    private fun initRewardedAds() {
        adRequest = AdRequest.Builder().build()
        loadRewardAd()
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                Log.d(LOG_TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(LOG_TAG, "Ad dismissed fullscreen content.")
                rewardedAd = null
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(LOG_TAG, "Ad failed to show fullscreen content.")
                rewardedAd = null
            }

            override fun onAdImpression() {
                Log.d(LOG_TAG, "Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
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
                getString(REWARDED_AD_UNIT_ID),
                it,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Timber.tag(LOG_TAG).d(adError.toString())
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
