package com.krishna.infodeck

import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.krishna.infodeck.core.Logger
import com.krishna.infodeck.ui.AppViewModel
import com.krishna.infodeck.ui.CardEditorScreen
import com.krishna.infodeck.ui.CardViewScreen
import com.krishna.infodeck.ui.CategoriesScreen
import com.krishna.infodeck.ui.ErrorLogsScreen
import com.krishna.infodeck.ui.HomeScreen
import com.krishna.infodeck.ui.InfoDeckTheme
import com.krishna.infodeck.ui.SettingsScreen
import com.krishna.infodeck.ui.SystemBars
import com.krishna.infodeck.ui.UpcomingScreen
import java.security.KeyStore
import java.util.concurrent.Executor
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class MainActivity : FragmentActivity() {

    companion object {
        const val EXTRA_CARD_ID = "card_id"
        private const val APP_LOCK_KEY_ALIAS = "infodeck_app_lock_key"
        private const val APP_LOCK_TRANSFORMATION =
            "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        com.krishna.infodeck.ui.ShareFiles.sweep(this)
        super.onCreate(savedInstanceState)
        measureNavBarOnce()

        val startCardId = intent?.getLongExtra(EXTRA_CARD_ID, -1L) ?: -1L

        setContent {
            val vm: AppViewModel = viewModel()
            val settings by vm.settings.collectAsState()
            var unlocked by remember { mutableStateOf(false) }

            InfoDeckTheme(
                themeSetting = settings.theme,
                ui = settings.ui,
                followSystemText = settings.followSystemText
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (settings.appLock && !unlocked) {
                        LockScreen(onUnlock = { unlocked = true })
                    } else {
                        AppNav(vm = vm, startCardId = startCardId)
                    }
                }
            }
        }
    }

    /**
     * Nav-bar height measured ONCE from the activity decor view - never from insets
     * read inside a sheet (that reads 0 inside ModalBottomSheet's popup window).
     */
    private fun measureNavBarOnce() {
        val density = resources.displayMetrics.density
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            SystemBars.navBottomDp = (bottom / density).toInt()
            insets
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val existing = getSecretKey()
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        val spec = KeyGenParameterSpec.Builder(
            APP_LOCK_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey? {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        return keyStore.getKey(APP_LOCK_KEY_ALIAS, null) as? SecretKey
    }

    private fun buildEncryptCipher(): Cipher {
        val cipher = Cipher.getInstance(APP_LOCK_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        return cipher
    }

    @Composable
    private fun LockScreen(onUnlock: () -> Unit) {
        val activity = this
        val executor: Executor = androidx.core.content.ContextCompat.getMainExecutor(activity)

        fun prompt() {
            try {
                val manager = BiometricManager.from(activity)
                val allowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                else BiometricManager.Authenticators.BIOMETRIC_WEAK
                if (manager.canAuthenticate(allowed) != BiometricManager.BIOMETRIC_SUCCESS) {
                    Logger.e("Lock", "no biometric or device credential available - unlocking")
                    onUnlock(); return
                }
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock InfoDeck")
                    .setSubtitle("Use your fingerprint or device PIN")
                    .setAllowedAuthenticators(allowed)
                    .build()

                val cipher = buildEncryptCipher()
                val prompt = BiometricPrompt(
                    activity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            try {
                                val authenticatedCipher = result.cryptoObject?.cipher
                                if (authenticatedCipher == null) {
                                    Logger.e("Lock", "auth succeeded but no crypto object returned")
                                    return
                                }
                                authenticatedCipher.doFinal("unlock".toByteArray(Charsets.UTF_8))
                                onUnlock()
                            } catch (t: Throwable) {
                                Logger.e("Lock", "crypto verification failed", t)
                            }
                        }
                        override fun onAuthenticationError(code: Int, msg: CharSequence) {
                            Logger.e("Lock", "auth error $code: $msg")
                        }
                    }
                )
                prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            } catch (t: Throwable) {
                Logger.e("Lock", "prompt failed - unlocking to avoid a lock-out", t)
                onUnlock()
            }
        }

        LaunchedEffect(Unit) { prompt() }

        com.krishna.infodeck.ui.DeckLockScreen(onUnlock = { prompt() })
    }
}

@Composable
private fun AppNav(vm: AppViewModel, startCardId: Long) {
    val nav = rememberNavController()

    LaunchedEffect(startCardId) {
        if (startCardId > 0) nav.navigate("card/$startCardId")
    }

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onEditCard = { nav.navigate("editor?cardId=$it&catId=0") },
                onShareCard = { nav.navigate("card/$it?share=true") },
                vm = vm,
                onOpenCard = { nav.navigate("card/$it") },
                onNewCard = { catId -> nav.navigate("editor?cardId=0&catId=${catId ?: 0L}") },
                onUpcoming = { nav.navigate("upcoming") },
                onSettings = { nav.navigate("settings") },
                onCategories = { nav.navigate("categories") },
                onTags = { nav.navigate("tags") }
            )
        }
        composable(
            "card/{cardId}?share={share}",
            arguments = listOf(
                navArgument("cardId") { type = NavType.LongType },
                navArgument("share") { type = NavType.BoolType; defaultValue = false }
            )
        ) { entry ->
            val id = entry.arguments?.getLong("cardId") ?: -1L
            val openShare = entry.arguments?.getBoolean("share") ?: false
            CardViewScreen(
                vm = vm, cardId = id, openShareOnStart = openShare,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate("editor?cardId=$it&catId=0") },
                onOpenCategory = { nav.popBackStack("home", inclusive = false) }
            )
        }
        composable(
            "editor?cardId={cardId}&catId={catId}",
            arguments = listOf(
                navArgument("cardId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("catId") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { entry ->
            val cardId = entry.arguments?.getLong("cardId") ?: 0L
            val catId = entry.arguments?.getLong("catId") ?: 0L
            CardEditorScreen(
                vm = vm,
                cardId = cardId.takeIf { it > 0 },
                presetCategoryId = catId.takeIf { it > 0 },
                onDone = { nav.popBackStack() }
            )
        }
        composable("categories") { CategoriesScreen(vm = vm, onBack = { nav.popBackStack() }) }
        composable("upcoming") {
            UpcomingScreen(
                vm = vm, onBack = { nav.popBackStack() }, onOpenCard = { nav.navigate("card/$it") },
                onHome = { nav.popBackStack("home", inclusive = false) },
                onSettings = { nav.navigate("settings") },
                onTags = { nav.navigate("tags") },
                onNewCard = { nav.navigate("editor?cardId=0&catId=0") }
            )
        }
        composable("settings") {
            SettingsScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onErrorLogs = { nav.navigate("errors") },
                onTextSize = { nav.navigate("textsize") },
                onTags = { nav.navigate("tags") },
                onCategories = { nav.navigate("categories") },
                onHome = { nav.popBackStack("home", inclusive = false) },
                onUpcoming = { nav.navigate("upcoming") },
                onNewCard = { nav.navigate("editor?cardId=0&catId=0") }
            )
        }
        composable("textsize") {
            com.krishna.infodeck.ui.TextSizeScreen(
                vm = vm, onBack = { nav.popBackStack() }, onFineTune = { nav.navigate("finetune") }
            )
        }
        composable("finetune") { com.krishna.infodeck.ui.FineTuneScreen(vm = vm, onBack = { nav.popBackStack() }) }
        composable("tags") {
            com.krishna.infodeck.ui.ManageTagsScreen(
                vm = vm, onBack = { nav.popBackStack() },
                onHome = { nav.popBackStack("home", inclusive = false) },
                onUpcoming = { nav.navigate("upcoming") },
                onSettings = { nav.navigate("settings") },
                onNewCard = { nav.navigate("editor?cardId=0&catId=0") }
            )
        }
        composable("errors") { ErrorLogsScreen(vm = vm, onBack = { nav.popBackStack() }) }
    }
}
