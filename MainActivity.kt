// MainActivity.kt: the front door of the app.
//
// Android starts this "Activity" when the user taps the app icon. It does very little on
// purpose: it creates the ViewModel (the object that holds the match), keeps the splash screen
// up until the saved data has loaded, and hands the screen over to Jetpack Compose, which draws
// everything else.
//
// Words used here:
//  - Activity: one window of an Android app. Score247 has just one.
//  - ViewModel: an object that holds the app's data and survives screen rotation.
//  - Compose: the toolkit that draws the user interface from Kotlin code.
package com.waleedahmedja.score247

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.waleedahmedja.score247.navigation.AppNavigation
import com.waleedahmedja.score247.ui.theme.Ink50
import com.waleedahmedja.score247.ui.theme.Score247Theme
import com.waleedahmedja.score247.viewmodel.MatchViewModel

/** The app's only Activity. */
class MainActivity : ComponentActivity() {

    // "by viewModels()" asks Android for the ViewModel. Android makes it once and gives back the
    // SAME one after a rotation, so the match is not lost when the phone is turned sideways.
    private val vm: MatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. installSplashScreen MUST come before super.onCreate()
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // 2. Draw behind the system bars. Android 15 (targetSdk 35) forces this anyway;
        //    calling it explicitly makes older versions look identical, and screens apply
        //    window insets themselves (see Scaffold / statusBarsPadding in the screens).
        enableEdgeToEdge()

        // 3. Keep the splash up until the saved match has loaded, so the first frame the
        //    user sees is already the right screen — no flash, no spinner.
        splash.setKeepOnScreenCondition { !vm.ready.value }

        // "setContent" is where Compose takes over: everything inside draws the app.
        // Score247Theme applies the app's colours and fonts; Surface paints the background colour;
        // AppNavigation decides which screen is showing.
        setContent {
            Score247Theme {
                Surface(modifier = Modifier.fillMaxSize(), color = Ink50) {
                    AppNavigation(vm)
                }
            }
        }
    }
}
