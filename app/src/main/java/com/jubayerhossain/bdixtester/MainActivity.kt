package com.jubayerhossain.bdixtester

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jubayerhossain.bdixtester.core.util.LocaleHelper
import com.jubayerhossain.bdixtester.navigation.BdixTesterAppRoot
import com.jubayerhossain.bdixtester.ui.theme.BdixTesterTheme
import com.jubayerhossain.bdixtester.ui.theme.resolveUseDarkTheme
import android.graphics.Color as AndroidColor

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val appContainer = (application as BdixtesterApplication).appContainer
    setContent {
      val themePreset by appContainer.themePreset.collectAsStateWithLifecycle()
      val themePreference by appContainer.themePreference.collectAsStateWithLifecycle()
      val appLanguage by appContainer.appLanguage.collectAsStateWithLifecycle()
      val reduceAnimations by appContainer.reduceAnimations.collectAsStateWithLifecycle()
      val useDarkTheme = resolveUseDarkTheme(themePreference)

      DisposableEffect(useDarkTheme, themePreset) {
        enableEdgeToEdge(
          statusBarStyle =
            if (useDarkTheme) {
              SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            } else {
              SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            },
          navigationBarStyle =
            if (useDarkTheme) {
              SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            } else {
              SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            },
        )
        onDispose {}
      }

      val baseContext = LocalContext.current
      val localizedContext =
        remember(baseContext, appLanguage) {
          LocaleHelper.localizedContext(baseContext, appLanguage)
        }
      CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalResources provides localizedContext.resources,
        LocalActivityResultRegistryOwner provides this@MainActivity,
        LocalOnBackPressedDispatcherOwner provides this@MainActivity,
      ) {
        BdixTesterTheme(
          themePreset = themePreset,
          themePreference = themePreference,
          reduceAnimations = reduceAnimations,
        ) {
          BdixTesterAppRoot(appContainer = appContainer)
        }
      }
    }
  }
}
