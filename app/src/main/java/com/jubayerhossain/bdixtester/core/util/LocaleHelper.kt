package com.jubayerhossain.bdixtester.core.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import com.jubayerhossain.bdixtester.core.model.AppLanguage
import java.util.Locale

object LocaleHelper {
  fun localizedContext(
    baseContext: Context,
    language: AppLanguage,
  ): Context {
    val tag = language.tag ?: return baseContext
    val locale = Locale.forLanguageTag(tag)
    val config = Configuration(baseContext.resources.configuration)
    config.setLocale(locale)
    val configContext = baseContext.createConfigurationContext(config)
    return object : ContextWrapper(baseContext) {
      override fun getResources(): Resources = configContext.resources

      override fun getAssets(): AssetManager = configContext.assets

      override fun createConfigurationContext(overrideConfiguration: Configuration): Context =
        configContext.createConfigurationContext(overrideConfiguration)
    }
  }
}
