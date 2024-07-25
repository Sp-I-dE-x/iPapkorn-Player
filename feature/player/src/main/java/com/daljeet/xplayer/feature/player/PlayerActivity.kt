package com.daljeet.xplayer.feature.player

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.KeyEvent
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.WindowManager
import android.view.accessibility.CaptioningManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.*
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TimeBar
import com.daljeet.xplayer.core.common.Utils
import com.daljeet.xplayer.core.common.extensions.*
import com.daljeet.xplayer.core.model.*
import com.daljeet.xplayer.feature.player.databinding.ActivityPlayerBinding
import com.daljeet.xplayer.feature.player.databinding.AdUnifiedBinding
import com.daljeet.xplayer.feature.player.dialogs.*
import com.daljeet.xplayer.feature.player.extensions.*
import com.daljeet.xplayer.feature.player.model.Subtitle
import com.daljeet.xplayer.feature.player.utils.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import kotlinx.coroutines.*
import timber.log.Timber
import java.nio.charset.Charset
import com.daljeet.xplayer.core.ui.R as coreUiR

@SuppressLint("UnsafeOptInUsageError")
@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding

    private val viewModel: PlayerViewModel by viewModels()
    private val applicationPreferences get() = viewModel.appPrefs.value
    private val playerPreferences get() = viewModel.playerPrefs.value

    private var playWhenReady = true
    private var isPlaybackFinished = false

    private var isFileLoaded = false
    private var isControlsLocked = false
    private var shouldFetchPlaylist = true
    private var isSubtitleLauncherHasUri = false
    private var isFirstFrameRendered = false
    private var isFrameRendered = false
    private var isPlayingOnScrubStart: Boolean = false
    private var previousScrubPosition = 0L
    private var scrubStartPosition: Long = -1L
    private var currentOrientation: Int? = null
    private var currentVideoOrientation: Int? = null
    private var currentVideoSize: VideoSize? = null
    private var hideVolumeIndicatorJob: Job? = null
    private var hideBrightnessIndicatorJob: Job? = null
    private var hideInfoLayoutJob: Job? = null
    private var isStartedAdsShown = false

    private val shouldFastSeek: Boolean
        get() = playerPreferences.shouldFastSeek(player.duration)

    private lateinit var player: Player
    private lateinit var playerGestureHelper: PlayerGestureHelper
    private lateinit var playlistManager: PlaylistManager
    private lateinit var trackSelector: DefaultTrackSelector
    private var surfaceView: SurfaceView? = null
    private var mediaSession: MediaSession? = null
    private lateinit var playerApi: PlayerApi
    private lateinit var volumeManager: VolumeManager
    private lateinit var brightnessManager: BrightnessManager
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val playbackStateListener: Player.Listener = playbackStateListener()
    private val subtitleFileLauncher = registerForActivityResult(OpenDocument()) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            isSubtitleLauncherHasUri = true
            viewModel.externalSubtitles.add(it)
            playVideo(playlistManager.getCurrent() ?: intent.data!!)
        }
    }

    private lateinit var audioTrackButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var exoContentFrameLayout: AspectRatioFrameLayout
    private lateinit var lockControlsButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var playbackSpeedButton: ImageButton
    private lateinit var playerLockControls: FrameLayout
    private lateinit var playerUnlockControls: FrameLayout
    private lateinit var playerCenterControls: LinearLayout
    private lateinit var prevButton: ImageButton
    private lateinit var screenRotateButton: ImageButton
    private lateinit var seekBar: TimeBar
    private lateinit var subtitleTrackButton: ImageButton
    private lateinit var unlockControlsButton: ImageButton
    private lateinit var videoTitleTextView: TextView
    private lateinit var videoZoomButton: ImageButton
    private lateinit var adView: AdView

    private var currentNativeAd: NativeAd? = null

    private val adSize: AdSize
        get() {
            val display = windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)

            val density = outMetrics.density
            var adWidthPixels = binding.nativeAdFrame.width.toFloat()
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prettyPrintIntent()
        setNightMode()
        applyDynamicColors()
        setupWindowAttributes()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdView()
        initializeViews()
        setupPlayerControls()
        initializeManagers()
        playerGestureHelper = PlayerGestureHelper(viewModel, this, volumeManager, brightnessManager)
        playlistManager = PlaylistManager()
        playerApi = PlayerApi(this)
    }

    override fun onStart() {
        super.onStart()
        applyBrightnessSetting()
        createPlayer()
        setOrientation()
        initPlaylist()
        initializePlayerView()
        playVideo(playlistManager.getCurrent() ?: intent.data!!)
    }

    override fun onStop() {
        super.onStop()
        binding.volumeGestureLayout.visibility = View.GONE
        binding.brightnessGestureLayout.visibility = View.GONE
        currentOrientation = requestedOrientation
        releasePlayer()
    }

    private fun setNightMode() {
        AppCompatDelegate.setDefaultNightMode(
            when (applicationPreferences.themeConfig) {
                ThemeConfig.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeConfig.OFF -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeConfig.ON -> AppCompatDelegate.MODE_NIGHT_YES
            }
        )
    }

    private fun applyDynamicColors() {
        if (applicationPreferences.useDynamicColors) {
            DynamicColors.applyToActivityIfAvailable(this)
        }
    }

    private fun setupWindowAttributes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun setupAdView() {
        adView = AdView(this)
        binding.nativeAdFrame.addView(adView)
        adView.visibility = View.GONE
        loadBanner()
    }

    private fun initializeViews() {
        with(binding.playerView) {
            audioTrackButton = findViewById(R.id.btn_audio_track)
            backButton = findViewById(R.id.back_button)
            exoContentFrameLayout = findViewById(R.id.exo_content_frame)
            lockControlsButton = findViewById(R.id.btn_lock_controls)
            nextButton = findViewById(R.id.btn_play_next)
            playbackSpeedButton = findViewById(R.id.btn_playback_speed)
            playerLockControls = findViewById(R.id.player_lock_controls)
            playerUnlockControls = findViewById(R.id.player_unlock_controls)
            playerCenterControls = findViewById(R.id.player_center_controls)
            prevButton = findViewById(R.id.btn_play_prev)
            screenRotateButton = findViewById(R.id.screen_rotate)
            seekBar = findViewById(R.id.exo_progress)
            subtitleTrackButton = findViewById(R.id.btn_subtitle_track)
            unlockControlsButton = findViewById(R.id.btn_unlock_controls)
            videoTitleTextView = findViewById(R.id.video_name)
            videoZoomButton = findViewById(R.id.btn_video_zoom)
        }
    }

    private fun setupPlayerControls() {
        seekBar.addListener(object : TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                handleScrubStart(position)
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                handleScrubMove(position)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                handleScrubStop()
            }
        })

        audioTrackButton.setOnClickListener { showTrackSelectionDialog(C.TRACK_TYPE_AUDIO) }
        subtitleTrackButton.setOnClickListener { showTrackSelectionDialog(C.TRACK_TYPE_TEXT) }
        subtitleTrackButton.setOnLongClickListener {
            subtitleFileLauncher.launch(
                arrayOf(
                    MimeTypes.APPLICATION_SUBRIP,
                    MimeTypes.APPLICATION_TTML,
                    MimeTypes.TEXT_VTT,
                    MimeTypes.TEXT_SSA,
                    MimeTypes.BASE_TYPE_APPLICATION + "/octet-stream",
                    MimeTypes.BASE_TYPE_TEXT + "/*"
                )
            )
            true
        }

        playbackSpeedButton.setOnClickListener { showPlaybackSpeedDialog() }
        nextButton.setOnClickListener { handleNextButton() }
        prevButton.setOnClickListener { handlePrevButton() }
        lockControlsButton.setOnClickListener { lockPlayerControls() }
        unlockControlsButton.setOnClickListener { unlockPlayerControls() }
        videoZoomButton.setOnClickListener { applyNextVideoZoom() }
        videoZoomButton.setOnLongClickListener {
            showVideoZoomOptionsDialog()
            true
        }
        screenRotateButton.setOnClickListener { toggleScreenOrientation() }
        backButton.setOnClickListener { finish() }
    }

    private fun initializeManagers() {
        volumeManager = VolumeManager(getSystemService(Context.AUDIO_SERVICE) as AudioManager)
        brightnessManager = BrightnessManager(this)
    }

    private fun applyBrightnessSetting() {
        if (playerPreferences.rememberPlayerBrightness) {
            brightnessManager.setBrightness(playerPreferences.playerBrightness)
        }
    }

    private fun createPlayer() {
        Timber.d("Creating player")

        val renderersFactory = NextRenderersFactory(applicationContext)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(
                when (playerPreferences.decoderPriority) {
                    DecoderPriority.DEVICE_ONLY -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                    DecoderPriority.PREFER_DEVICE -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                    DecoderPriority.PREFER_APP -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                }
            )

        trackSelector = DefaultTrackSelector(applicationContext).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredAudioLanguage(playerPreferences.preferredAudioLanguage)
                    .setPreferredTextLanguage(playerPreferences.preferredSubtitleLanguage)
            )
        }

        player = ExoPlayer.Builder(applicationContext)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(getAudioAttributes(), playerPreferences.requireAudioFocus)
            .setHandleAudioBecomingNoisy(playerPreferences.pauseOnHeadsetDisconnect)
            .build()

        setupMediaSession()
        player.addListener(playbackStateListener)
        volumeManager.loudnessEnhancer = loudnessEnhancer
    }

    private fun setupMediaSession() {
        try {
            if (player.canAdvertiseSession()) {
                mediaSession = MediaSession.Builder(this, player).build()
            }
            loudnessEnhancer =
                if (playerPreferences.shouldUseVolumeBoost) LoudnessEnhancer(player.audioSessionId) else null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setOrientation() {
        requestedOrientation =
            currentOrientation ?: playerPreferences.playerScreenOrientation.toActivityOrientation()
    }

    private fun initializePlayerView() {
        with(binding.playerView) {
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            player = this@PlayerActivity.player
            controllerShowTimeoutMs = playerPreferences.controllerAutoHideTimeout.toMillis
            setControllerVisibilityListener { visibility ->
                toggleSystemBars(visibility == View.VISIBLE && !isControlsLocked)
            }
            setupSubtitleView()
        }
    }

    private fun setupSubtitleView() {
        binding.playerView.subtitleView?.apply {
            val captioningManager = getSystemService(Context.CAPTIONING_SERVICE) as CaptioningManager
            val systemCaptionStyle = CaptionStyleCompat.createFromCaptionStyle(captioningManager.userStyle)
            val userStyle = createUserCaptionStyle()
            setStyle(systemCaptionStyle.takeIf { playerPreferences.useSystemCaptionStyle } ?: userStyle)
            setApplyEmbeddedStyles(playerPreferences.applyEmbeddedStyles)
            setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, playerPreferences.subtitleTextSize.toFloat())
        }
    }

    private fun createUserCaptionStyle(): CaptionStyleCompat {
        return CaptionStyleCompat(
            Color.WHITE,
            if (playerPreferences.subtitleBackground) Color.BLACK else Color.TRANSPARENT,
            Color.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
            Color.BLACK,
            Typeface.create(
                playerPreferences.subtitleFont.toTypeface(),
                if (playerPreferences.subtitleTextBold) Typeface.BOLD else Typeface.NORMAL
            )
        )
    }

    private fun handleScrubStart(position: Long) {
        if (player.isPlaying) {
            isPlayingOnScrubStart = true
            player.pause()
        }
        isFrameRendered = true
        scrubStartPosition = player.currentPosition
        previousScrubPosition = player.currentPosition
        scrub(position)
        showPlayerInfo(
            info = Utils.formatDurationMillis(position),
            subInfo = "[${Utils.formatDurationMillisSign(position - scrubStartPosition)}]"
        )
    }

    private fun handleScrubMove(position: Long) {
        scrub(position)
        showPlayerInfo(
            info = Utils.formatDurationMillis(position),
            subInfo = "[${Utils.formatDurationMillisSign(position - scrubStartPosition)}]"
        )
    }

    private fun handleScrubStop() {
        hidePlayerInfo(0L)
        scrubStartPosition = -1L
        if (isPlayingOnScrubStart) {
            player.play()
        }
    }

    private fun scrub(position: Long) {
        if (isFrameRendered) {
            isFrameRendered = false
            if (position > previousScrubPosition) {
                player.seekForward(position, shouldFastSeek)
            } else {
                player.seekBack(position, shouldFastSeek)
            }
            previousScrubPosition = position
        }
    }

    private fun showTrackSelectionDialog(type: Int) {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return
        if (!mappedTrackInfo.isRendererAvailable(type)) return

        TrackSelectionDialogFragment(
            type = type,
            tracks = player.currentTracks,
            onTrackSelected = { player.switchTrack(type, it) }
        ).show(supportFragmentManager, "TrackSelectionDialog")
    }

    private fun showPlaybackSpeedDialog() {
        PlaybackSpeedControlsDialogFragment(
            currentSpeed = player.playbackParameters.speed,
            onChange = {
                viewModel.isPlaybackSpeedChanged = true
                player.setPlaybackSpeed(it)
            }
        ).show(supportFragmentManager, "PlaybackSpeedSelectionDialog")
    }

    private fun handleNextButton() {
        if (playlistManager.hasNext()) {
            playlistManager.getCurrent()?.let { savePlayerState(it) }
            viewModel.resetAllToDefaults()
            playVideo(playlistManager.getNext()!!)
        }
    }

    private fun handlePrevButton() {
        if (playlistManager.hasPrev()) {
            playlistManager.getCurrent()?.let { savePlayerState(it) }
            viewModel.resetAllToDefaults()
            playVideo(playlistManager.getPrev()!!)
        }
    }

    private fun lockPlayerControls() {
        playerUnlockControls.visibility = View.INVISIBLE
        playerLockControls.visibility = View.VISIBLE
        isControlsLocked = true
        toggleSystemBars(false)
    }

    private fun unlockPlayerControls() {
        playerLockControls.visibility = View.INVISIBLE
        playerUnlockControls.visibility = View.VISIBLE
        isControlsLocked = false
        binding.playerView.showController()
        toggleSystemBars(true)
    }

    private fun applyNextVideoZoom() {
        val videoZoom = playerPreferences.playerVideoZoom.next()
        applyVideoZoom(videoZoom = videoZoom, showInfo = true)
    }

    private fun showVideoZoomOptionsDialog() {
        VideoZoomOptionsDialogFragment(
            currentVideoZoom = playerPreferences.playerVideoZoom,
            onVideoZoomOptionSelected = { applyVideoZoom(videoZoom = it, showInfo = true) }
        ).show(supportFragmentManager, "VideoZoomOptionsDialog")
    }

    private fun toggleScreenOrientation() {
        requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    private fun initPlaylist() = lifecycleScope.launch(Dispatchers.IO) {
        intent.data?.let {
            val playlist = viewModel.getPlaylistFromUri(getMediaContentUri(it)!!)
            playlistManager.setPlaylist(playlist)
        }
    }

    private fun playVideo(uri: Uri) = lifecycleScope.launch(Dispatchers.IO) {
        playlistManager.updateCurrent(uri)
        val isCurrentUriIsFromIntent = intent.data == uri

        viewModel.updateState(getPath(uri))
        if (isCurrentUriIsFromIntent && playerApi.hasPosition) {
            viewModel.currentPlaybackPosition = playerApi.position?.toLong()
        }

        val subtitleStreams = createExternalSubtitleStreams(getSubtitles(uri, isCurrentUriIsFromIntent))
        val mediaStream = createMediaStream(uri).buildUpon().setSubtitleConfigurations(subtitleStreams).build()

        withContext(Dispatchers.Main) {
            setupSurfaceView()
            videoTitleTextView.text = if (isCurrentUriIsFromIntent && playerApi.hasTitle) playerApi.title else getFilenameFromUri(uri)
            player.setMediaItem(mediaStream, viewModel.currentPlaybackPosition ?: C.TIME_UNSET)
            player.playWhenReady = playWhenReady
            player.prepare()
        }
    }

    private fun setupSurfaceView() {
        surfaceView = SurfaceView(this@PlayerActivity).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        player.setVideoSurfaceView(surfaceView)
        exoContentFrameLayout.addView(surfaceView, 0)
    }

    private fun getSubtitles(uri: Uri, isCurrentUriIsFromIntent: Boolean): List<Subtitle> {
        val apiSubs = if (isCurrentUriIsFromIntent) playerApi.getSubs() else emptyList()
        val localSubs = uri.getLocalSubtitles(this, viewModel.externalSubtitles.toList())
        val externalSubs = viewModel.externalSubtitles.map { it.toSubtitle(this) }
        return apiSubs + localSubs + externalSubs
    }

    private fun releasePlayer() {
        Timber.d("Releasing player")
        subtitleCacheDir.deleteFiles()
        playWhenReady = player.playWhenReady
        playlistManager.getCurrent()?.let { savePlayerState(it) }
        player.removeListener(playbackStateListener)
        player.release()
        mediaSession?.release()
        mediaSession = null
    }

    private fun playbackStateListener() = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            handleIsPlayingChanged(isPlaying)
            super.onIsPlayingChanged(isPlaying)
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            super.onAudioSessionIdChanged(audioSessionId)
            setupLoudnessEnhancer(audioSessionId)
        }

        @SuppressLint("SourceLockedOrientationActivity")
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            handleVideoSizeChanged(videoSize)
            super.onVideoSizeChanged(videoSize)
        }

        override fun onPlayerError(error: PlaybackException) {
            handlePlayerError(error)
            super.onPlayerError(error)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            handlePlaybackStateChanged(playbackState)
            super.onPlaybackStateChanged(playbackState)
        }

        override fun onRenderedFirstFrame() {
            isFirstFrameRendered = true
            binding.playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            super.onRenderedFirstFrame()
        }

        override fun onTracksChanged(tracks: Tracks) {
            handleTracksChanged()
            super.onTracksChanged(tracks)
        }
    }

    private fun handleIsPlayingChanged(isPlaying: Boolean) {
        binding.playerView.keepScreenOn = isPlaying
        if (isPlaying) {
            adView.visibility = View.GONE
            startTimer()
        } else {
            adView.visibility = View.VISIBLE
            endTimer()
        }
    }

    private fun setupLoudnessEnhancer(audioSessionId: Int) {
        loudnessEnhancer?.release()
        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleVideoSizeChanged(videoSize: VideoSize) {
        currentVideoSize = videoSize
        applyVideoZoom(videoZoom = playerPreferences.playerVideoZoom, showInfo = false)

        if (currentOrientation != null) return

        if (playerPreferences.playerScreenOrientation == ScreenOrientation.VIDEO_ORIENTATION) {
            currentVideoOrientation = if (videoSize.isPortrait) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            requestedOrientation = currentVideoOrientation!!
        }
    }

    private fun handlePlayerError(error: PlaybackException) {
        Timber.e(error)
        val alertDialog = MaterialAlertDialogBuilder(this@PlayerActivity)
            .setTitle(getString(coreUiR.string.error_playing_video))
            .setMessage(error.message ?: getString(coreUiR.string.unknown_error))
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                if (playlistManager.hasNext()) playVideo(playlistManager.getNext()!!) else finish()
            }
            .create()

        alertDialog.show()
    }

    private fun handlePlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_ENDED -> {
                endTimer()
                isPlaybackFinished = true
                if (playlistManager.hasNext() && playerPreferences.autoplay) {
                    playlistManager.getCurrent()?.let { savePlayerState(it) }
                    playVideo(playlistManager.getNext()!!)
                } else {
                    finish()
                }
            }

            Player.STATE_READY -> {
                isFrameRendered = true
                isFileLoaded = true
                if (!isStartedAdsShown) {
                    isStartedAdsShown = true
                    Utils.menuclick.value = "Player Activity"
                }
            }

            Player.STATE_BUFFERING -> Timber.d("Player state: BUFFERING")
            Player.STATE_IDLE -> Timber.d("Player state: IDLE")
        }
    }

    private fun handleTracksChanged() {
        if (isFirstFrameRendered) return

        if (isSubtitleLauncherHasUri) {
            val textTracks = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT && it.isSupported }
            viewModel.currentSubtitleTrackIndex = textTracks.size - 1
        }
        isSubtitleLauncherHasUri = false
        player.switchTrack(C.TRACK_TYPE_AUDIO, viewModel.currentAudioTrackIndex)
        player.switchTrack(C.TRACK_TYPE_TEXT, viewModel.currentSubtitleTrackIndex)
        player.setPlaybackSpeed(viewModel.currentPlaybackSpeed)
    }

    override fun finish() {
        if (playerApi.shouldReturnResult) {
            val result = playerApi.getResult(
                isPlaybackFinished = isPlaybackFinished,
                duration = player.duration,
                position = player.currentPosition
            )
            setResult(Activity.RESULT_OK, result)
        }
        super.finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            playlistManager.clearQueue()
            viewModel.resetAllToDefaults()
            setIntent(it)
            prettyPrintIntent()
            shouldFetchPlaylist = true
            playVideo(playlistManager.getCurrent() ?: it.data!!)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_DPAD_UP -> {
                handleVolumeUpKey()
                return true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                handleVolumeDownKey()
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_BUTTON_SELECT -> {
                handlePlayPauseKey(keyCode)
                return true
            }

            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_SPACE -> {
                handlePlayPauseKey(keyCode)
                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                handleRewindKey()
                return true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                handleFastForwardKey()
                return true
            }

            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                handleEnterKey()
                return true
            }

            KeyEvent.KEYCODE_BACK -> {
                handleBackKey()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                hideVolumeGestureLayout()
                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                hidePlayerInfo()
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun getAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
    }

    private fun showVolumeGestureLayout() {
        hideVolumeIndicatorJob?.cancel()
        with(binding) {
            volumeGestureLayout.visibility = View.VISIBLE
            volumeProgressBar.max = volumeManager.maxVolume.times(100)
            volumeProgressBar.progress = volumeManager.currentVolume.times(100).toInt()
            volumeProgressText.text = volumeManager.volumePercentage.toString()
        }
    }

    private fun showBrightnessGestureLayout() {
        hideBrightnessIndicatorJob?.cancel()
        with(binding) {
            brightnessGestureLayout.visibility = View.VISIBLE
            brightnessProgressBar.max = brightnessManager.maxBrightness.times(100).toInt()
            brightnessProgressBar.progress = brightnessManager.currentBrightness.times(100).toInt()
            brightnessProgressText.text = brightnessManager.brightnessPercentage.toString()
        }
    }

    private fun showPlayerInfo(info: String, subInfo: String? = null) {
        hideInfoLayoutJob?.cancel()
        with(binding) {
            infoLayout.visibility = View.VISIBLE
            infoText.text = info
            infoSubtext.visibility = if (subInfo == null) View.GONE else View.VISIBLE
            infoSubtext.text = subInfo
        }
    }

    private fun showTopInfo(info: String) {
        with(binding) {
            topInfoLayout.visibility = View.VISIBLE
            topInfoText.text = info
        }
    }

    private fun hideVolumeGestureLayout(delayTimeMillis: Long = HIDE_DELAY_MILLIS) {
        if (binding.volumeGestureLayout.visibility != View.VISIBLE) return
        hideVolumeIndicatorJob = lifecycleScope.launch {
            delay(delayTimeMillis)
            binding.volumeGestureLayout.visibility = View.GONE
        }
    }

    private fun hideBrightnessGestureLayout(delayTimeMillis: Long = HIDE_DELAY_MILLIS) {
        if (binding.brightnessGestureLayout.visibility != View.VISIBLE) return
        hideBrightnessIndicatorJob = lifecycleScope.launch {
            delay(delayTimeMillis)
            binding.brightnessGestureLayout.visibility = View.GONE
        }
        if (playerPreferences.rememberPlayerBrightness) {
            viewModel.setPlayerBrightness(window.attributes.screenBrightness)
        }
    }

    private fun hidePlayerInfo(delayTimeMillis: Long = HIDE_DELAY_MILLIS) {
        if (binding.infoLayout.visibility != View.VISIBLE) return
        hideInfoLayoutJob = lifecycleScope.launch {
            delay(delayTimeMillis)
            binding.infoLayout.visibility = View.GONE
        }
    }

    private fun hideTopInfo() {
        binding.topInfoLayout.visibility = View.GONE
    }

    private fun savePlayerState(uri: Uri) {
        if (isFirstFrameRendered) {
            viewModel.saveState(
                path = getPath(uri),
                position = player.currentPosition,
                duration = player.duration,
                audioTrackIndex = player.getCurrentTrackIndex(C.TRACK_TYPE_AUDIO),
                subtitleTrackIndex = player.getCurrentTrackIndex(C.TRACK_TYPE_TEXT),
                playbackSpeed = player.playbackParameters.speed
            )
        }
        isFirstFrameRendered = false
    }

    private fun createMediaStream(uri: Uri) = MediaItem.Builder()
        .setMediaId(uri.toString())
        .setUri(uri)
        .build()

    private fun createExternalSubtitleStreams(subtitles: List<Subtitle>): List<MediaItem.SubtitleConfiguration> {
        return subtitles.map {
            val charset = if (with(playerPreferences.subtitleTextEncoding) {
                    isNotEmpty() && Charset.isSupported(this)
                }) {
                Charset.forName(playerPreferences.subtitleTextEncoding)
            } else {
                null
            }
            MediaItem.SubtitleConfiguration.Builder(
                convertToUTF8(
                    uri = it.uri,
                    charset = charset
                )
            ).apply {
                setId(it.uri.toString())
                setMimeType(it.uri.getSubtitleMime())
                setLabel(it.name)
                if (it.isSelected) setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            }.build()
        }
    }

    private fun resetExoContentFrameWidthAndHeight() {
        exoContentFrameLayout.layoutParams.width = LayoutParams.MATCH_PARENT
        exoContentFrameLayout.layoutParams.height = LayoutParams.MATCH_PARENT
        exoContentFrameLayout.scaleX = 1.0f
        exoContentFrameLayout.scaleY = 1.0f
        exoContentFrameLayout.requestLayout()
    }

    private fun applyVideoZoom(videoZoom: VideoZoom, showInfo: Boolean) {
        viewModel.setVideoZoom(videoZoom)
        resetExoContentFrameWidthAndHeight()
        when (videoZoom) {
            VideoZoom.BEST_FIT -> {
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                videoZoomButton.setImageDrawable(this, coreUiR.drawable.ic_fit_screen)
            }

            VideoZoom.STRETCH -> {
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                videoZoomButton.setImageDrawable(this, coreUiR.drawable.ic_aspect_ratio)
            }

            VideoZoom.CROP -> {
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                videoZoomButton.setImageDrawable(this, coreUiR.drawable.ic_crop_landscape)
            }

            VideoZoom.HUNDRED_PERCENT -> {
                currentVideoSize?.let {
                    exoContentFrameLayout.layoutParams.width = it.width
                    exoContentFrameLayout.layoutParams.height = it.height
                    exoContentFrameLayout.requestLayout()
                }
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                videoZoomButton.setImageDrawable(this, coreUiR.drawable.ic_width_wide)
            }
        }
        if (showInfo) {
            lifecycleScope.launch {
                binding.infoLayout.visibility = View.VISIBLE
                binding.infoText.text = getString(videoZoom.nameRes())
                delay(HIDE_DELAY_MILLIS)
                binding.infoLayout.visibility = View.GONE
            }
        }
    }

    companion object {
        const val HIDE_DELAY_MILLIS = 1000L
    }

    private fun loadBanner() {
        adView.adUnitId = resources.getString(com.daljeet.xplayer.core.ui.R.string.banner_id)
        adView.setAdSize(adSize)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    override fun onResume() {
        super.onResume()
        adView.resume()
        endTimer()
    }

    override fun onDestroy() {
        adView.destroy()
        endTimer()
        super.onDestroy()
    }

    private fun handleVolumeUpKey() {
        if (!binding.playerView.isControllerFullyVisible || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            volumeManager.increaseVolume(playerPreferences.showSystemVolumePanel)
            showVolumeGestureLayout()
        }
    }

    private fun handleVolumeDownKey() {
        if (!binding.playerView.isControllerFullyVisible || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumeManager.decreaseVolume(playerPreferences.showSystemVolumePanel)
            showVolumeGestureLayout()
        }
    }

    private fun handlePlayPauseKey(keyCode: Int) {
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PAUSE -> player.pause()
            KeyEvent.KEYCODE_MEDIA_PLAY -> player.play()
            player.isPlaying -> player.pause()
            else -> player.play()
        }
    }

    private fun handleRewindKey() {
        if (!binding.playerView.isControllerFullyVisible || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
            val pos = player.currentPosition
            if (scrubStartPosition == -1L) {
                scrubStartPosition = pos
            }
            val position = (pos - 10_000).coerceAtLeast(0L)
            player.seekBack(position, shouldFastSeek)
            showPlayerInfo(
                info = Utils.formatDurationMillis(position),
                subInfo = "[${Utils.formatDurationMillisSign(position - scrubStartPosition)}]"
            )
        }
    }

    private fun handleFastForwardKey() {
        if (!binding.playerView.isControllerFullyVisible || keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            val pos = player.currentPosition
            if (scrubStartPosition == -1L) {
                scrubStartPosition = pos
            }

            val position = (pos + 10_000).coerceAtMost(player.duration)
            player.seekForward(position, shouldFastSeek)
            showPlayerInfo(
                info = Utils.formatDurationMillis(position),
                subInfo = "[${Utils.formatDurationMillisSign(position - scrubStartPosition)}]"
            )
        }
    }

    private fun handleEnterKey() {
        if (!binding.playerView.isControllerFullyVisible) {
            binding.playerView.showController()
        }
    }

    private fun handleBackKey() {
        if (binding.playerView.isControllerFullyVisible && player.isPlaying && isDeviceTvBox()) {
            binding.playerView.hideController()
        }
    }

    var manager: DownloadManager? = null

    fun showDownloadDialog(uri: Uri) {
        val builder1: AlertDialog.Builder = AlertDialog.Builder(this)
        builder1.setMessage("Please Select Before Proceed")
        builder1.setCancelable(true)

        builder1.setPositiveButton("Download") { dialog, _ ->
            manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(uri)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            manager!!.enqueue(request)
            dialog.cancel()
        }

        builder1.setNegativeButton("Stream") { dialog, _ ->
            playVideo(uri)
            dialog.cancel()
        }

        val alert11: AlertDialog = builder1.create()
        alert11.show()
    }

    private fun populateNativeAdView(nativeAd: NativeAd, unifiedAdBinding: AdUnifiedBinding) {
        val nativeAdView = unifiedAdBinding.root

        nativeAdView.mediaView = unifiedAdBinding.adMedia
        nativeAdView.headlineView = unifiedAdBinding.adHeadline
        nativeAdView.bodyView = unifiedAdBinding.adBody
        nativeAdView.callToActionView = unifiedAdBinding.adCallToAction
        nativeAdView.iconView = unifiedAdBinding.adAppIcon
        nativeAdView.priceView = unifiedAdBinding.adPrice
        nativeAdView.starRatingView = unifiedAdBinding.adStars
        nativeAdView.storeView = unifiedAdBinding.adStore
        nativeAdView.advertiserView = unifiedAdBinding.adAdvertiser

        unifiedAdBinding.adHeadline.text = nativeAd.headline
        nativeAd.mediaContent?.let { unifiedAdBinding.adMedia.setMediaContent(it) }

        unifiedAdBinding.adBody.apply {
            visibility = if (nativeAd.body == null) View.INVISIBLE else View.VISIBLE
            text = nativeAd.body
        }

        unifiedAdBinding.adCallToAction.apply {
            visibility = if (nativeAd.callToAction == null) View.INVISIBLE else View.VISIBLE
            text = nativeAd.callToAction
        }

        unifiedAdBinding.adAppIcon.apply {
            visibility = if (nativeAd.icon == null) View.GONE else View.VISIBLE
            setImageDrawable(nativeAd.icon?.drawable)
        }

        unifiedAdBinding.adPrice.apply {
            visibility = if (nativeAd.price == null) View.INVISIBLE else View.VISIBLE
            text = nativeAd.price
        }

        unifiedAdBinding.adStore.apply {
            visibility = if (nativeAd.store == null) View.INVISIBLE else View.VISIBLE
            text = nativeAd.store
        }

        unifiedAdBinding.adStars.apply {
            visibility = if (nativeAd.starRating == null) View.INVISIBLE else View.VISIBLE
            rating = nativeAd.starRating!!.toFloat()
        }

        unifiedAdBinding.adAdvertiser.apply {
            visibility = if (nativeAd.advertiser == null) View.INVISIBLE else View.VISIBLE
            text = nativeAd.advertiser
        }

        nativeAdView.setNativeAd(nativeAd)

        nativeAd.mediaContent?.videoController?.let { vc ->
            if (nativeAd.mediaContent.hasVideoContent()) {
                vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                    override fun onVideoEnd() {
                        super.onVideoEnd()
                    }
                }
            }
        }
    }

    private fun loadNativeAd() {
        val builder = AdLoader.Builder(this@PlayerActivity, resources.getString(coreUiR.string.native_id))

        builder.forNativeAd { nativeAd ->
            var activityDestroyed = false
            if (activityDestroyed || isFinishing || isChangingConfigurations) {
                nativeAd.destroy()
                return@forNativeAd
            }
            currentNativeAd?.destroy()
            currentNativeAd = nativeAd
            val unifiedAdBinding = AdUnifiedBinding.inflate(layoutInflater)
            populateNativeAdView(nativeAd, unifiedAdBinding)
            binding.nativeAdFrame.removeAllViews()
            binding.nativeAdFrame.addView(unifiedAdBinding.root)

            val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
            val nativeAdOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
            val adLoader = builder.withAdListener(object : AdListener() {
                override fun onAdClicked() {
                    super.onAdClicked()
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                }
            }).build()

            adLoader.loadAd(AdRequest.Builder().build())
        }.withAdListener(object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
            }
        })
    }

    override fun onPause() {
        super.onPause()
        endTimer()
    }

    private var timer: CountDownTimer? = null

    private val VIDEO_AD_INTERVAL = 60000 * 30L
    private val BANNER_AD_START_TIME = 60000 * 20L
    private val BANNER_AD_END_TIME = 60000 * 21L
    private var isTimerRunning = false

    private fun startTimer() {
        if (isTimerRunning) return

        timer = object : CountDownTimer(VIDEO_AD_INTERVAL, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                isTimerRunning = true
                handleTimerTick(millisUntilFinished)
            }

            override fun onFinish() {
                handleTimerFinish()
            }
        }
        timer?.start()
    }

    private fun handleTimerTick(millisUntilFinished: Long) {
        if (millisUntilFinished in BANNER_AD_END_TIME..BANNER_AD_START_TIME) {
            if (adView.visibility == View.GONE) {
                adView.visibility = View.VISIBLE
            }
        }
        if (millisUntilFinished <= 20000L) {
            if (adView.visibility == View.VISIBLE) {
                adView.visibility = View.GONE
            }
        }
    }

    private fun handleTimerFinish() {
        isTimerRunning = false
        adView.visibility = View.GONE
        player.pause()
        Utils.menuclick.value = "timer"
    }

    private fun endTimer() {
        timer?.cancel()
        isTimerRunning = false
        timer = null
    }
}
