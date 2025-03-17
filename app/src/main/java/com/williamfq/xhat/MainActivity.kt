package com.williamfq.xhat

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.auth.FirebaseAuth
import com.williamfq.domain.location.LocationTracker
import com.williamfq.xhat.ui.Navigation.AppNavigation
import com.williamfq.xhat.ui.Navigation.NavigationState
import com.williamfq.xhat.ui.Navigation.Screen
import com.williamfq.xhat.ui.theme.XhatTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var locationTracker: LocationTracker
    private lateinit var permissionsViewModel: PermissionsViewModel
    private var permissionsGranted by mutableStateOf(false)
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var navigationState: NavigationState
    private var permissionRequestCount = 0
    private val maxPermissionRequests = 3

    companion object {
        private const val PERMISSIONS_GRANTED_TIME = "permissions_granted_time"
        private const val PROFILE_SETUP_COMPLETE = "profile_setup_complete"
        private const val TEST_DEVICE_ID = "TEST-DEVICE-ID"
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsViewModel.updatePermissionsStatus(permissions)
        permissionRequestCount++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionsViewModel = ViewModelProvider(this)[PermissionsViewModel::class.java]
        navigationState = NavigationState()
        sharedPreferences = getSharedPreferences("xhat_preferences", MODE_PRIVATE)

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
        }

        permissionsViewModel.initializePermissionsLauncher(permissionLauncher::launch)

        lifecycleScope.launch(Dispatchers.IO) {
            createWebViewCacheDirInBackground()
            initializeAdMob()
            withContext(Dispatchers.Main) {
                initializeApp()
            }
        }

        observePermissions()
    }

    private fun observePermissions() {
        lifecycleScope.launch {
            permissionsViewModel.permissionsGranted.collect { granted ->
                permissionsGranted = granted
                if (granted) {
                    onAllPermissionsGranted()
                } else if (permissionRequestCount < maxPermissionRequests) {
                    showPermissionRequiredDialog()
                } else {
                    showPermissionSettingsDialog()
                }
            }
        }
    }

    private suspend fun createWebViewCacheDirInBackground() = withContext(Dispatchers.IO) {
        val cacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
        if (!cacheDir.exists()) {
            try {
                if (cacheDir.mkdirs()) {
                    Timber.d("Directorio de caché WebView creado: ${cacheDir.absolutePath}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creando directorio de caché WebView")
            }
        }
    }

    private fun initializeAdMob() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (BuildConfig.DEBUG) {
                    val configuration = RequestConfiguration.Builder()
                        .setTestDeviceIds(listOf(TEST_DEVICE_ID))
                        .build()
                    MobileAds.setRequestConfiguration(configuration)
                }
                withContext(Dispatchers.Main) {
                    MobileAds.initialize(this@MainActivity) { initializationStatus ->
                        initializationStatus.adapterStatusMap.forEach { (adapter, status) ->
                            Timber.d("AdMob Adapter $adapter: ${status.description} (Latencia: ${status.latency}ms)")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error en la inicialización de AdMob")
            }
        }
    }

    private fun initializeApp() {
        val permissionsGrantedTime = sharedPreferences.getLong(PERMISSIONS_GRANTED_TIME, 0)
        if (permissionsGrantedTime == 0L || !areAllPermissionsGranted()) {
            permissionsViewModel.requestPermissionsIfNeeded()
        } else {
            permissionsGranted = true
            onAllPermissionsGranted()
        }
    }

    private fun areAllPermissionsGranted(): Boolean =
        permissionsViewModel.requiredPermissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    private fun onAllPermissionsGranted() {
        sharedPreferences.edit { putLong(PERMISSIONS_GRANTED_TIME, java.util.Date().time) }
        lifecycleScope.launch(Dispatchers.IO) {
            checkAuthenticationStatus()
        }
    }

    private suspend fun checkAuthenticationStatus() {
        try {
            val currentUser = auth.currentUser
            val isProfileSetupComplete = sharedPreferences.getBoolean(PROFILE_SETUP_COMPLETE, false)
            val startDestination = when {
                currentUser == null -> Screen.PhoneNumber.route
                !isProfileSetupComplete -> Screen.ProfileSetup.route
                else -> Screen.Main.route
            }
            withContext(Dispatchers.Main) {
                setContent {
                    XhatTheme {
                        val navController = rememberNavController()
                        AppNavigation(
                            navController = navController,
                            startDestination = startDestination,
                            permissionsGranted = permissionsGranted,
                            onRequestPermissions = { permissionsViewModel.requestPermissionsIfNeeded() },
                            navigationState = navigationState,
                            locationTracker = locationTracker
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error en checkAuthenticationStatus")
            withContext(Dispatchers.Main) {
                showErrorDialog("Error al verificar estado de autenticación: ${e.message}")
            }
        }
    }

    private fun showPermissionRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permisos Requeridos")
            .setMessage("Esta aplicación necesita permisos para funcionar correctamente. Por favor, concede los permisos necesarios.")
            .setPositiveButton("Reintentar") { _, _ -> permissionsViewModel.requestPermissionsIfNeeded() }
            .setNegativeButton("Continuar sin permisos") { _, _ ->
                permissionsGranted = false
                lifecycleScope.launch(Dispatchers.IO) { checkAuthenticationStatus() }
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permisos Necesarios")
            .setMessage("Has denegado los permisos varias veces. Habilítalos en la configuración de la aplicación.")
            .setPositiveButton("Ir a Configuración") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("Cancelar") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (permissionsGranted || permissionRequestCount >= maxPermissionRequests) {
            lifecycleScope.launch(Dispatchers.IO) { checkAuthenticationStatus() }
        } else {
            permissionsViewModel.requestPermissionsIfNeeded()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        permissionsViewModel.cleanup()
    }
}