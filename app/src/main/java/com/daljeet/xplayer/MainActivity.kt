package com.daljeet.xplayer

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.daljeet.xplayer.core.media.sync.MediaSynchronizer
import com.daljeet.xplayer.core.model.ThemeConfig
import com.daljeet.xplayer.core.ui.theme.NextPlayerTheme
import com.daljeet.xplayer.feature.player.PlayerActivity
import com.daljeet.xplayer.ui.DownloadScreen
import com.daljeet.xplayer.ui.MAIN_ROUTE
import com.daljeet.xplayer.ui.MainScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.util.Arrays
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var synchronizer: MediaSynchronizer

    private val viewModel: MainActivityViewModel by viewModels()
    private val COUNTER_TIME_MILLISECONDS = 5000L
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    var manager: DownloadManager? = null

    private val storagePermission = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_MEDIA_VIDEO
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Manifest.permission.READ_EXTERNAL_STORAGE
        else -> Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
    private var secondsRemaining: Long = 0L

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeMobileAdsSdk()
        if (intent?.data != null) {
            showDownloadDialog(intent.data!!)
        }

        var uiState: MainActivityUiState by mutableStateOf(MainActivityUiState.Loading)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    uiState = state
                }
            }
        }
        installSplashScreen()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // setting up the individual tabs
            val homeTab = TabBarItem(
                title = "Home",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                root = MAIN_ROUTE
            )

            val settingsTab = TabBarItem(
                title = "Downloads",
                selectedIcon = ImageVector.vectorResource(id = R.drawable.baseline_download_24),
                unselectedIcon = ImageVector.vectorResource(id = R.drawable.baseline_download_24),
                root = "Downloads"
            )

            // creating a list of all the tabs
            val tabBarItems = listOf(homeTab, settingsTab)

            NextPlayerTheme(
                darkTheme = shouldUseDarkTheme(uiState = uiState),
                highContrastDarkTheme = shouldUseHighContrastDarkTheme(uiState = uiState),
                dynamicColor = shouldUseDynamicTheming(uiState = uiState)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    val storagePermissionState =
                        rememberPermissionState(permission = storagePermission)
                    val lifecycleOwner = LocalLifecycleOwner.current

                    DisposableEffect(key1 = lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_START) {
                                storagePermissionState.launchPermissionRequest()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    LaunchedEffect(key1 = storagePermissionState.status.isGranted) {
                        if (storagePermissionState.status.isGranted) {
                            createDirectory()
                            synchronizer.startSync()
                        }
                    }

                    val mainNavController = rememberNavController()
                    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()

                    val bottomBarState = rememberSaveable { (mutableStateOf(true)) }

                    // Control TopBar and BottomBar
                    when (navBackStackEntry?.destination?.route) {
                        MAIN_ROUTE, "Downloads" -> {
                            // Show BottomBar and TopBar
                            bottomBarState.value = true
                        }
                        else -> {
                            bottomBarState.value = false
                        }
                    }

                    Scaffold(
                        bottomBar = {
                            if (bottomBarState.value) {
                                TabView(tabBarItems, mainNavController)
                            }
                        }
                    ) {
                        NavHost(navController = mainNavController, startDestination = MAIN_ROUTE) {
                            composable(MAIN_ROUTE) {
                                MainScreen(
                                    permissionState = storagePermissionState,
                                )
                            }
                            composable("Downloads") {
                                DownloadScreen(
                                    permissionState = storagePermissionState,
                                )
                            }
                        }
                    }
                }
            }
        }

        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onComplete)
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }
        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this) {}
        val config = RequestConfiguration.Builder()
            .setTestDeviceIds(Arrays.asList("1EB6EAD9715AB41F3696FCC31586020F")).build()
        MobileAds.setRequestConfiguration(config)
    }

    private fun createDirectory(): Boolean {
        val filepath = Environment.getExternalStorageDirectory()
        val dir = File(filepath.path + "/iPapkorn/")
        return if (!dir.exists()) {
            try {
                dir.mkdirs()
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } else {
            true
        }
    }

    private var onComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context?, intent: Intent?) {
            Toast.makeText(this@MainActivity, "Download Completed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let {
            showDownloadDialog(it)
        }
    }

    private fun showDownloadDialog(uri: Uri) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Please Select Before Proceed")
            .setCancelable(true)
            .setNegativeButton("Download") { dialog, _ ->
                (applicationContext as NextPlayerApplication).showRewardedAdIfLoaded()
                manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                val request = DownloadManager.Request(uri)
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "iPapkorn/" + getFileName(uri)
                )
                manager?.enqueue(request)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                dialog.cancel()
            }
            .setPositiveButton("Stream") { dialog, _ ->
                val intent = Intent(Intent.ACTION_VIEW, uri, this, PlayerActivity::class.java)
                startActivity(intent)
                dialog.cancel()
            }
        builder.create().show()
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    @Composable
    private fun shouldUseDarkTheme(uiState: MainActivityUiState): Boolean = when (uiState) {
        MainActivityUiState.Loading -> isSystemInDarkTheme()
        is MainActivityUiState.Success -> when (uiState.preferences.themeConfig) {
            ThemeConfig.SYSTEM -> isSystemInDarkTheme()
            ThemeConfig.OFF -> false
            ThemeConfig.ON -> true
        }
    }

    @Composable
    fun shouldUseHighContrastDarkTheme(uiState: MainActivityUiState): Boolean = when (uiState) {
        MainActivityUiState.Loading -> false
        is MainActivityUiState.Success -> uiState.preferences.useHighContrastDarkTheme
    }

    @Composable
    private fun shouldUseDynamicTheming(uiState: MainActivityUiState): Boolean = when (uiState) {
        MainActivityUiState.Loading -> false
        is MainActivityUiState.Success -> uiState.preferences.useDynamicColors
    }

    data class TabBarItem(
        val title: String,
        val selectedIcon: ImageVector,
        val unselectedIcon: ImageVector,
        val badgeAmount: Int? = null,
        val root: String,
    )

    @Composable
    fun TabView(tabBarItems: List<TabBarItem>, navController: NavController) {
        var selectedTabIndex by rememberSaveable {
            mutableStateOf(0)
        }

        NavigationBar(containerColor = Color.Transparent) {
            tabBarItems.forEachIndexed { index, tabBarItem ->
                NavigationBarItem(
                    selected = selectedTabIndex == index,
                    onClick = {
                        selectedTabIndex = index
                        navController.navigate(tabBarItem.root)
                    },
                    icon = {
                        TabBarIconView(
                            isSelected = selectedTabIndex == index,
                            selectedIcon = tabBarItem.selectedIcon,
                            unselectedIcon = tabBarItem.unselectedIcon,
                            title = tabBarItem.title,
                            badgeAmount = tabBarItem.badgeAmount
                        )
                    },
                    label = { Text(tabBarItem.title) }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TabBarIconView(
        isSelected: Boolean,
        selectedIcon: ImageVector,
        unselectedIcon: ImageVector,
        title: String,
        badgeAmount: Int? = null
    ) {
        BadgedBox(badge = { TabBarBadgeView(badgeAmount) }) {
            Icon(
                imageVector = if (isSelected) {
                    selectedIcon
                } else {
                    unselectedIcon
                },
                contentDescription = title
            )
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun TabBarBadgeView(count: Int? = null) {
        count?.let {
            Badge {
                Text(it.toString())
            }
        }
    }

    @Composable
    fun MoreView() {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Thing 1")
            Text("Thing 2")
            Text("Thing 3")
            Text("Thing 4")
            Text("Thing 5")
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        NextPlayerTheme {
            MoreView()
        }
    }
}
