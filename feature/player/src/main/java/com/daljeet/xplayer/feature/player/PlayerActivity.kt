package com.daljeet.xplayer.feature.player

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.AlertDialog.*
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
import android.util.Log
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
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TimeBar
import com.daljeet.xplayer.core.common.Utils
import com.daljeet.xplayer.core.common.extensions.convertToUTF8
import com.daljeet.xplayer.core.common.extensions.deleteFiles
import com.daljeet.xplayer.core.common.extensions.getFilenameFromUri
import com.daljeet.xplayer.core.common.extensions.getMediaContentUri
import com.daljeet.xplayer.core.common.extensions.getPath
import com.daljeet.xplayer.core.common.extensions.isDeviceTvBox
import com.daljeet.xplayer.core.common.extensions.subtitleCacheDir
import com.daljeet.xplayer.core.model.DecoderPriority
import com.daljeet.xplayer.core.model.ScreenOrientation
import com.daljeet.xplayer.core.model.ThemeConfig
import com.daljeet.xplayer.core.model.VideoZoom
import com.daljeet.xplayer.feature.player.databinding.ActivityPlayerBinding
import com.daljeet.xplayer.feature.player.databinding.AdUnifiedBinding
import com.daljeet.xplayer.feature.player.dialogs.PlaybackSpeedControlsDialogFragment
import com.daljeet.xplayer.feature.player.dialogs.TrackSelectionDialogFragment
import com.daljeet.xplayer.feature.player.dialogs.VideoZoomOptionsDialogFragment
import com.daljeet.xplayer.feature.player.dialogs.nameRes
import com.daljeet.xplayer.feature.player.extensions.audioSessionId
import com.daljeet.xplayer.feature.player.extensions.getCurrentTrackIndex
import com.daljeet.xplayer.feature.player.extensions.getLocalSubtitles
import com.daljeet.xplayer.feature.player.extensions.getSubtitleMime
import com.daljeet.xplayer.feature.player.extensions.isPortrait
import com.daljeet.xplayer.feature.player.extensions.isRendererAvailable
import com.daljeet.xplayer.feature.player.extensions.next
import com.daljeet.xplayer.feature.player.extensions.prettyPrintIntent
import com.daljeet.xplayer.feature.player.extensions.seekBack
import com.daljeet.xplayer.feature.player.extensions.seekForward
import com.daljeet.xplayer.feature.player.extensions.setImageDrawable
import com.daljeet.xplayer.feature.player.extensions.shouldFastSeek
import com.daljeet.xplayer.feature.player.extensions.switchTrack
import com.daljeet.xplayer.feature.player.extensions.toActivityOrientation
import com.daljeet.xplayer.feature.player.extensions.toSubtitle
import com.daljeet.xplayer.feature.player.extensions.toTypeface
import com.daljeet.xplayer.feature.player.extensions.togglePlayPause
import com.daljeet.xplayer.feature.player.extensions.toggleSystemBars
import com.daljeet.xplayer.feature.player.model.Subtitle
import com.daljeet.xplayer.feature.player.utils.BrightnessManager
import com.daljeet.xplayer.feature.player.utils.PlayerApi
import com.daljeet.xplayer.feature.player.utils.PlayerGestureHelper
import com.daljeet.xplayer.feature.player.utils.PlaylistManager
import com.daljeet.xplayer.feature.player.utils.VolumeManager
import com.daljeet.xplayer.feature.player.utils.toMillis
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.nio.charset.Charset
import com.daljeet.xplayer.core.ui.R as coreUiR


@SuppressLint("UnsafeOptInUsageError")
@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    lateinit var binding: ActivityPlayerBinding

    private val viewModel: PlayerViewModel by viewModels()
    private val currentContext = this
    private val applicationPreferences get() = viewModel.appPrefs.value
    private val playerPreferences get() = viewModel.playerPrefs.value

    private var playWhenReady = true
    private var isPlaybackFinished = false

    var isFileLoaded = false
    var isControlsLocked = false
    private var shouldFetchPlaylist = true
    private var isSubtitleLauncherHasUri = false
    private var isFirstFrameRendered = false
    private var isFrameRendered = false
    private var isPlayingOnScrubStart: Boolean = false
    private var previousScrubPosition = 0L
    private var scrubStartPosition: Long = -1L
    private var currentOrientation: Int? = null
    private var currentVideoOrientation: Int? = null
    var currentVideoSize: VideoSize? = null
    private var hideVolumeIndicatorJob: Job? = null
    private var hideBrightnessIndicatorJob: Job? = null
    private var hideInfoLayoutJob: Job? = null
    private var isStartedAdsShown = false

    private val shouldFastSeek: Boolean
        get() = playerPreferences.shouldFastSeek(player.duration)

    /**
     * Player
     */
    private lateinit var player: Player
    private lateinit var playerGestureHelper: PlayerGestureHelper
    private lateinit var playlistManager: PlaylistManager
    private lateinit var trackSelector: DefaultTrackSelector
    private var surfaceView: SurfaceView? = null
    private var mediaSession: MediaSession? = null
    private lateinit var playerApi: PlayerApi
    private lateinit var volumeManager: VolumeManager
    private lateinit var brightnessManager: BrightnessManager
    var loudnessEnhancer: LoudnessEnhancer? = null

    /**
     * Listeners
     */
    private val playbackStateListener: Player.Listener = playbackStateListener()
    private val subtitleFileLauncher = registerForActivityResult(OpenDocument()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            isSubtitleLauncherHasUri = true
            viewModel.externalSubtitles.add(uri)
        }
        playVideo(playlistManager.getCurrent() ?: intent.data!!)
    }

    /**
     * Player controller views
     */
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
    val ADMOB_AD_UNIT_ID = com.daljeet.xplayer.core.ui.R.string.native_id

    private var currentNativeAd: NativeAd? = null
    private lateinit var adView: AdView


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
        AppCompatDelegate.setDefaultNightMode(
            when (applicationPreferences.themeConfig) {
                ThemeConfig.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeConfig.OFF -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeConfig.ON -> AppCompatDelegate.MODE_NIGHT_YES
            }
        )

        if (applicationPreferences.useDynamicColors) {
            DynamicColors.applyToActivityIfAvailable(this)
        }

        // The window is always allowed to extend into the DisplayCutout areas on the short edges of the screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adView = AdView(this)
        binding.nativeAdFrame.addView(adView)
        adView.visibility = View.GONE
        loadBanner()
        // Initializing views
        audioTrackButton = binding.playerView.findViewById(R.id.btn_audio_track)
        backButton = binding.playerView.findViewById(R.id.back_button)
        exoContentFrameLayout = binding.playerView.findViewById(R.id.exo_content_frame)
        lockControlsButton = binding.playerView.findViewById(R.id.btn_lock_controls)
        nextButton = binding.playerView.findViewById(R.id.btn_play_next)
        playbackSpeedButton = binding.playerView.findViewById(R.id.btn_playback_speed)
        playerLockControls = binding.playerView.findViewById(R.id.player_lock_controls)
        playerUnlockControls = binding.playerView.findViewById(R.id.player_unlock_controls)
        playerCenterControls = binding.playerView.findViewById(R.id.player_center_controls)
        prevButton = binding.playerView.findViewById(R.id.btn_play_prev)
        screenRotateButton = binding.playerView.findViewById(R.id.screen_rotate)
        seekBar = binding.playerView.findViewById(R.id.exo_progress)
        subtitleTrackButton = binding.playerView.findViewById(R.id.btn_subtitle_track)
        unlockControlsButton = binding.playerView.findViewById(R.id.btn_unlock_controls)
        videoTitleTextView = binding.playerView.findViewById(R.id.video_name)
        videoZoomButton = binding.playerView.findViewById(R.id.btn_video_zoom)

        seekBar.addListener(object : TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
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

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                scrub(position)
                showPlayerInfo(
                    info = Utils.formatDurationMillis(position),
                    subInfo = "[${Utils.formatDurationMillisSign(position - scrubStartPosition)}]"
                )
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                hidePlayerInfo(0L)
                scrubStartPosition = -1L
                if (isPlayingOnScrubStart) {
                    player.play()
                }
            }
        })

        volumeManager =
            VolumeManager(audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager)
        brightnessManager = BrightnessManager(activity = this)
        playerGestureHelper = PlayerGestureHelper(
            viewModel = viewModel,
            activity = this,
            volumeManager = volumeManager,
            brightnessManager = brightnessManager
        )

        playlistManager = PlaylistManager()
        playerApi = PlayerApi(this)
    }

    override fun onStart() {
        if (playerPreferences.rememberPlayerBrightness) {
            brightnessManager.setBrightness(playerPreferences.playerBrightness)
        }
        createPlayer()
        setOrientation()
        initPlaylist()
        initializePlayerView()
        playVideo(playlistManager.getCurrent() ?: intent.data!!)
        super.onStart()
    }

    override fun onStop() {
        binding.volumeGestureLayout.visibility = View.GONE
        binding.brightnessGestureLayout.visibility = View.GONE
        currentOrientation = requestedOrientation
        releasePlayer()
        super.onStop()
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
            this.setParameters(
                this.buildUponParameters()
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

        try {
            if (player.canAdvertiseSession()) {
                mediaSession = MediaSession.Builder(this, player).build()
            }
            loudnessEnhancer =
                if (playerPreferences.shouldUseVolumeBoost) LoudnessEnhancer(player.audioSessionId) else null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        player.addListener(playbackStateListener)
        volumeManager.loudnessEnhancer = loudnessEnhancer
    }

    private fun setOrientation() {
        requestedOrientation =
            currentOrientation ?: playerPreferences.playerScreenOrientation.toActivityOrientation()
    }

    private fun initializePlayerView() {
        binding.playerView.apply {
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            player = this@PlayerActivity.player
            controllerShowTimeoutMs = playerPreferences.controllerAutoHideTimeout.toMillis
            setControllerVisibilityListener(
                PlayerView.ControllerVisibilityListener { visibility ->
                    toggleSystemBars(showBars = visibility == View.VISIBLE && !isControlsLocked)
                }
            )

            subtitleView?.apply {
                val captioningManager =
                    getSystemService(Context.CAPTIONING_SERVICE) as CaptioningManager
                val systemCaptionStyle =
                    CaptionStyleCompat.createFromCaptionStyle(captioningManager.userStyle)
                val userStyle = CaptionStyleCompat(
                    Color.WHITE,
                    Color.BLACK.takeIf { playerPreferences.subtitleBackground }
                        ?: Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                    Color.BLACK,
                    Typeface.create(
                        playerPreferences.subtitleFont.toTypeface(),
                        Typeface.BOLD.takeIf { playerPreferences.subtitleTextBold }
                            ?: Typeface.NORMAL
                    )
                )
                setStyle(systemCaptionStyle.takeIf { playerPreferences.useSystemCaptionStyle }
                    ?: userStyle)
                setApplyEmbeddedStyles(playerPreferences.applyEmbeddedStyles)
                setFixedTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    playerPreferences.subtitleTextSize.toFloat()
                )
            }
        }

        audioTrackButton.setOnClickListener {
            val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return@setOnClickListener
            if (!mappedTrackInfo.isRendererAvailable(C.TRACK_TYPE_AUDIO)) return@setOnClickListener

            TrackSelectionDialogFragment(
                type = C.TRACK_TYPE_AUDIO,
                tracks = player.currentTracks,
                onTrackSelected = { player.switchTrack(C.TRACK_TYPE_AUDIO, it) }
            ).show(supportFragmentManager, "TrackSelectionDialog")
        }

        subtitleTrackButton.setOnClickListener {
            val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return@setOnClickListener
            if (!mappedTrackInfo.isRendererAvailable(C.TRACK_TYPE_TEXT)) return@setOnClickListener

            TrackSelectionDialogFragment(
                type = C.TRACK_TYPE_TEXT,
                tracks = player.currentTracks,
                onTrackSelected = { player.switchTrack(C.TRACK_TYPE_TEXT, it) }
            ).show(supportFragmentManager, "TrackSelectionDialog")
        }

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

        playbackSpeedButton.setOnClickListener {
            PlaybackSpeedControlsDialogFragment(
                currentSpeed = player.playbackParameters.speed,
                onChange = {
                    viewModel.isPlaybackSpeedChanged = true
                    player.setPlaybackSpeed(it)
                }
            ).show(supportFragmentManager, "PlaybackSpeedSelectionDialog")
        }

        nextButton.setOnClickListener {
            if (playlistManager.hasNext()) {
                playlistManager.getCurrent()?.let { savePlayerState(it) }
                viewModel.resetAllToDefaults()
                playVideo(playlistManager.getNext()!!)
            }
        }
        prevButton.setOnClickListener {
            if (playlistManager.hasPrev()) {
                playlistManager.getCurrent()?.let { savePlayerState(it) }
                viewModel.resetAllToDefaults()
                playVideo(playlistManager.getPrev()!!)
            }
        }
        lockControlsButton.setOnClickListener {
            playerUnlockControls.visibility = View.INVISIBLE
            playerLockControls.visibility = View.VISIBLE
            isControlsLocked = true
            toggleSystemBars(showBars = false)
        }
        unlockControlsButton.setOnClickListener {
            playerLockControls.visibility = View.INVISIBLE
            playerUnlockControls.visibility = View.VISIBLE
            isControlsLocked = false
            binding.playerView.showController()
            toggleSystemBars(showBars = true)
        }
        videoZoomButton.setOnClickListener {
            val videoZoom = playerPreferences.playerVideoZoom.next()
            applyVideoZoom(videoZoom = videoZoom, showInfo = true)
        }

        videoZoomButton.setOnLongClickListener {
            VideoZoomOptionsDialogFragment(
                currentVideoZoom = playerPreferences.playerVideoZoom,
                onVideoZoomOptionSelected = { applyVideoZoom(videoZoom = it, showInfo = true) }
            ).show(supportFragmentManager, "VideoZoomOptionsDialog")
            true
        }
        screenRotateButton.setOnClickListener {
            requestedOrientation = when (resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
        backButton.setOnClickListener { finish() }
    }

    private fun initPlaylist() = lifecycleScope.launch(Dispatchers.IO) {
        val mediaUri = getMediaContentUri(intent.data!!)

        if (mediaUri != null) {
            val playlist = viewModel.getPlaylistFromUri(mediaUri)
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

        // Get all subtitles for current uri
        val apiSubs = if (isCurrentUriIsFromIntent) playerApi.getSubs() else emptyList()
        val localSubs = uri.getLocalSubtitles(currentContext, viewModel.externalSubtitles.toList())
        val externalSubs = viewModel.externalSubtitles.map { it.toSubtitle(currentContext) }

        // current uri as MediaItem with subs
        val subtitleStreams = createExternalSubtitleStreams(apiSubs + localSubs + externalSubs)
        val mediaStream = createMediaStream(uri).buildUpon()
            .setSubtitleConfigurations(subtitleStreams)
            .build()

        withContext(Dispatchers.Main) {
            surfaceView = SurfaceView(this@PlayerActivity).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            }
            player.setVideoSurfaceView(surfaceView)
            exoContentFrameLayout.addView(surfaceView, 0)

            if (isCurrentUriIsFromIntent && playerApi.hasTitle) {
                videoTitleTextView.text = playerApi.title
            } else {
                videoTitleTextView.text = getFilenameFromUri(uri)
            }

            // Set media and start player
            player.setMediaItem(mediaStream, viewModel.currentPlaybackPosition ?: C.TIME_UNSET)
            player.playWhenReady = playWhenReady
            player.prepare()
        }
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
            binding.playerView.keepScreenOn = isPlaying
            if (isPlaying) {
                adView.visibility = View.GONE
                startTimer()
            } else {
                adView.visibility = View.VISIBLE
                endTimer()
            }
            super.onIsPlayingChanged(isPlaying)
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            super.onAudioSessionIdChanged(audioSessionId)
            loudnessEnhancer?.release()

            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @SuppressLint("SourceLockedOrientationActivity")
        override fun onVideoSizeChanged(videoSize: VideoSize) {
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
            super.onVideoSizeChanged(videoSize)
        }

        override fun onPlayerError(error: PlaybackException) {
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
            super.onPlayerError(error)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    endTimer()
                    Timber.d("Player state: ENDED")
                    isPlaybackFinished = true
                    if (playlistManager.hasNext() && playerPreferences.autoplay) {
                        playlistManager.getCurrent()?.let { savePlayerState(it) }
                        playVideo(playlistManager.getNext()!!)
                    } else {
                        finish()
                    }
                }

                Player.STATE_READY -> {
                    Timber.d("Player state: READY")
                    Timber.d(playlistManager.toString())
                    isFrameRendered = true
                    isFileLoaded = true
                    if (!isStartedAdsShown) {
                        isStartedAdsShown = true
                        Utils.menuclick.value = "Player Activity"
                    }
                }

                Player.STATE_BUFFERING -> {
                    Timber.d("Player state: BUFFERING")
                }

                Player.STATE_IDLE -> {
                    Timber.d("Player state: IDLE")
                }
            }
            super.onPlaybackStateChanged(playbackState)
        }

        override fun onRenderedFirstFrame() {
            isFirstFrameRendered = true
            binding.playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            super.onRenderedFirstFrame()
        }

        override fun onTracksChanged(tracks: Tracks) {
            super.onTracksChanged(tracks)
            if (isFirstFrameRendered) return

            if (isSubtitleLauncherHasUri) {
                val textTracks = player.currentTracks.groups
                    .filter { it.type == C.TRACK_TYPE_TEXT && it.isSupported }
                viewModel.currentSubtitleTrackIndex = textTracks.size - 1
            }
            isSubtitleLauncherHasUri = false
            player.switchTrack(C.TRACK_TYPE_AUDIO, viewModel.currentAudioTrackIndex)
            player.switchTrack(C.TRACK_TYPE_TEXT, viewModel.currentSubtitleTrackIndex)
            player.setPlaybackSpeed(viewModel.currentPlaybackSpeed)
        }
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
        if (intent != null) {
            playlistManager.clearQueue()
            viewModel.resetAllToDefaults()
            setIntent(intent)
            prettyPrintIntent()
            shouldFetchPlaylist = true
            playVideo(playlistManager.getCurrent() ?: intent.data!!)

        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (!binding.playerView.isControllerFullyVisible || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    volumeManager.increaseVolume(playerPreferences.showSystemVolumePanel)
                    showVolumeGestureLayout()
                    return true
                }
            }

            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (!binding.playerView.isControllerFullyVisible || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    volumeManager.decreaseVolume(playerPreferences.showSystemVolumePanel)
                    showVolumeGestureLayout()
                    return true
                }
            }

            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_BUTTON_SELECT -> {
                when {
                    keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        player.pause()
                    }

                    keyCode == KeyEvent.KEYCODE_MEDIA_PLAY -> player.play()
                    player.isPlaying -> player.pause()
                    else -> player.play()
                }
                return true
            }

            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_SPACE -> {
                if (!binding.playerView.isControllerFullyVisible) {
                    binding.playerView.togglePlayPause()
                    return true
                }
            }

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
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
                    return true
                }
            }

            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
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
                    return true
                }
            }

            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (!binding.playerView.isControllerFullyVisible) {
                    binding.playerView.showController()
                    return true
                }
            }

            KeyEvent.KEYCODE_BACK -> {
                if (binding.playerView.isControllerFullyVisible && player.isPlaying && isDeviceTvBox()) {
                    binding.playerView.hideController()
                    return true
                }
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

    fun showVolumeGestureLayout() {
        hideVolumeIndicatorJob?.cancel()
        with(binding) {
            volumeGestureLayout.visibility = View.VISIBLE
            volumeProgressBar.max = volumeManager.maxVolume.times(100)
            volumeProgressBar.progress = volumeManager.currentVolume.times(100).toInt()
            volumeProgressText.text = volumeManager.volumePercentage.toString()
        }
    }

    fun showBrightnessGestureLayout() {
        hideBrightnessIndicatorJob?.cancel()
        with(binding) {
            brightnessGestureLayout.visibility = View.VISIBLE
            brightnessProgressBar.max = brightnessManager.maxBrightness.times(100).toInt()
            brightnessProgressBar.progress = brightnessManager.currentBrightness.times(100).toInt()
            brightnessProgressText.text = brightnessManager.brightnessPercentage.toString()
        }
    }

    fun showPlayerInfo(info: String, subInfo: String? = null) {
        hideInfoLayoutJob?.cancel()
        with(binding) {
            infoLayout.visibility = View.VISIBLE
            infoText.text = info
            infoSubtext.visibility = View.GONE.takeIf { subInfo == null } ?: View.VISIBLE
            infoSubtext.text = subInfo
        }
    }

    fun showTopInfo(info: String) {
        with(binding) {
            topInfoLayout.visibility = View.VISIBLE
            topInfoText.text = info
        }
    }

    fun hideVolumeGestureLayout(delayTimeMillis: Long = HIDE_DELAY_MILLIS) {
        if (binding.volumeGestureLayout.visibility != View.VISIBLE) return
        hideVolumeIndicatorJob = lifecycleScope.launch {
            delay(delayTimeMillis)
            binding.volumeGestureLayout.visibility = View.GONE
        }
    }

    fun hideBrightnessGestureLayout(delayTimeMillis: Long = HIDE_DELAY_MILLIS) {
        if (binding.brightnessGestureLayout.visibility != View.VISIBLE) return
        hideBrightnessIndicatorJob = lifecycleScope.launch {
            delay(delayTimeMillis)
            binding.brightnessGestureLayout.visibility = View.GONE
        }
        if (playerPreferences.rememberPlayerBrightness) {
            viewModel.setPlayerBrightness(window.attributes.screenBrightness)
        }
    }

    fun hidePlayerInfo(delayTimeMillis: Long = HIDE_DELAY_MILLIS) {
        if (binding.infoLayout.visibility != View.VISIBLE) return
        hideInfoLayoutJob = lifecycleScope.launch {
            delay(delayTimeMillis)
            binding.infoLayout.visibility = View.GONE
        }
    }

    fun hideTopInfo() {
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

    private fun loadd() {
        val builder = AdLoader.Builder(this, resources.getString(ADMOB_AD_UNIT_ID))

        builder.forNativeAd {
        }
    }

    private fun loadNativeAd() {
        val builder =
            AdLoader.Builder(
                this@PlayerActivity,
                resources.getString(com.daljeet.xplayer.core.ui.R.string.native_id)
            )

        builder.forNativeAd { nativeAd: NativeAd ->
            // OnUnifiedNativeAdLoadedListener implementation.
            // If this callback occurs after the activity is destroyed, you must call
            // destroy and return or you may get a memory leak.
            var activityDestroyed = false
            if (activityDestroyed || isFinishing || isChangingConfigurations) {
                nativeAd.destroy()
                return@forNativeAd
            }
            // You must call destroy on old ads when you are done with them,
            // otherwise you will have a memory leak.
            currentNativeAd?.destroy()
            currentNativeAd = nativeAd
            val unifiedAdBinding = AdUnifiedBinding.inflate(layoutInflater)
            populateNativeAdView(nativeAd, unifiedAdBinding)
            binding.nativeAdFrame.removeAllViews()
            binding.nativeAdFrame.addView(unifiedAdBinding.root)

            var videoOptions: VideoOptions = VideoOptions.Builder().setStartMuted(true).build();
            var nativeAdOption: NativeAdOptions =
                NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
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
        }
        )
    }

    private fun populateNativeAdView(nativeAd: NativeAd, unifiedAdBinding: AdUnifiedBinding) {
        val nativeAdView = unifiedAdBinding.root

        // Set the media view.
        nativeAdView.mediaView = unifiedAdBinding.adMedia

        // Set other ad assets.
        nativeAdView.headlineView = unifiedAdBinding.adHeadline
        nativeAdView.bodyView = unifiedAdBinding.adBody
        nativeAdView.callToActionView = unifiedAdBinding.adCallToAction
        nativeAdView.iconView = unifiedAdBinding.adAppIcon
        nativeAdView.priceView = unifiedAdBinding.adPrice
        nativeAdView.starRatingView = unifiedAdBinding.adStars
        nativeAdView.storeView = unifiedAdBinding.adStore
        nativeAdView.advertiserView = unifiedAdBinding.adAdvertiser

        // The headline and media content are guaranteed to be in every UnifiedNativeAd.
        unifiedAdBinding.adHeadline.text = nativeAd.headline
        nativeAd.mediaContent?.let { unifiedAdBinding.adMedia.setMediaContent(it) }

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            unifiedAdBinding.adBody.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adBody.visibility = View.VISIBLE
            unifiedAdBinding.adBody.text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            unifiedAdBinding.adCallToAction.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adCallToAction.visibility = View.VISIBLE
            unifiedAdBinding.adCallToAction.text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            unifiedAdBinding.adAppIcon.visibility = View.GONE
        } else {
            unifiedAdBinding.adAppIcon.setImageDrawable(nativeAd.icon?.drawable)
            unifiedAdBinding.adAppIcon.visibility = View.VISIBLE
        }

        if (nativeAd.price == null) {
            unifiedAdBinding.adPrice.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adPrice.visibility = View.VISIBLE
            unifiedAdBinding.adPrice.text = nativeAd.price
        }

        if (nativeAd.store == null) {
            unifiedAdBinding.adStore.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adStore.visibility = View.VISIBLE
            unifiedAdBinding.adStore.text = nativeAd.store
        }

        if (nativeAd.starRating == null) {
            unifiedAdBinding.adStars.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adStars.rating = nativeAd.starRating!!.toFloat()
            unifiedAdBinding.adStars.visibility = View.VISIBLE
        }

        if (nativeAd.advertiser == null) {
            unifiedAdBinding.adAdvertiser.visibility = View.INVISIBLE
        } else {
            unifiedAdBinding.adAdvertiser.text = nativeAd.advertiser
            unifiedAdBinding.adAdvertiser.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        nativeAdView.setNativeAd(nativeAd)

        // Get the video controller for the ad. One will always be provided, even if the ad doesn't
        // have a video asset.
        val mediaContent = nativeAd.mediaContent
        val vc = mediaContent?.videoController

        // Updates the UI to say whether or not this ad has a video asset.
        if (vc != null && mediaContent.hasVideoContent()) {
            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
            // VideoController will call methods on this object when events occur in the video
            // lifecycle.
            vc.videoLifecycleCallbacks =
                object : VideoController.VideoLifecycleCallbacks() {
                    override fun onVideoEnd() {
                        // Publishers should allow native ads to complete video playback before
                        // refreshing or replacing them with another ad in the same UI location.
                        /* mainActivityBinding.refreshButton.isEnabled = true
                         mainActivityBinding.videostatusText.text = "Video status: Video playback has ended."*/
                        super.onVideoEnd()
                    }
                }
        } else {
            /* mainActivityBinding.videostatusText.text = "Video status: Ad does not contain a video asset."
             mainActivityBinding.refreshButton.isEnabled = true*/
        }
    }

    override fun onDestroy() {
        adView.destroy()
        endTimer()
        super.onDestroy()
    }

    private fun loadBanner() {
        // This is an ad unit ID for a test ad. Replace with your own banner ad unit ID.
        adView.adUnitId = resources.getString(com.daljeet.xplayer.core.ui.R.string.banner_id)
        adView.setAdSize(adSize)

        // Create an ad request.
        val adRequest = AdRequest.Builder().build()

        // Start loading the ad in the background.
        adView.loadAd(adRequest)
    }

    override fun onResume() {
        super.onResume()
        adView.resume()
        endTimer()
    }

    private var timer: CountDownTimer? = null

    val VIDEO_AD_INTERVAL = 60000 * 30L
    val BANNER_AD_START_TIME = 60000 * 20L
    val BANNER_AD_END_TIME = 60000 * 21L
    var isTimerRunning = false

    fun startTimer() {
        if (isTimerRunning) {
            return
        }
        timer = object : CountDownTimer(VIDEO_AD_INTERVAL, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                isTimerRunning = true
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

            override fun onFinish() {
                isTimerRunning = false;
                adView.visibility = View.GONE
                player.pause()
                Utils.menuclick.value = "timer"
            }
        }
        timer?.start()
    }

    fun endTimer() {
        timer?.cancel()
        isTimerRunning = false
        timer = null
    }

    var manager: DownloadManager? = null

    override fun onPause() {
        super.onPause()
        endTimer()
    }

    fun download(uri: Uri) {

    }

    fun showDownloadDialog(uri: Uri) {
        val builder1: Builder = Builder(this)
        builder1.setMessage("Please Select Before Proceed")
        builder1.setCancelable(true)

        builder1.setPositiveButton(
            "Download"
        ) { dialog, id ->
            manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val uri =
                Uri.parse(uri.toString())
            val request = DownloadManager.Request(uri)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            val reference = manager!!.enqueue(request)

            dialog.cancel()
        }

        builder1.setNegativeButton(
            "Stream"
        ) { dialog, _ ->
            playVideo(uri)
            dialog.cancel()
        }

        val alert11: AlertDialog = builder1.create()
        alert11.show()
    }

}


