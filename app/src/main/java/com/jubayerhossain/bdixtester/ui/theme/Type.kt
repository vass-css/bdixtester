package com.jubayerhossain.bdixtester.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.jubayerhossain.bdixtester.R

// Bundled TTF binaries are static Regular (400). Declaring only FontWeight.Normal allows
// Android's text renderer to synthesize crisp Medium/SemiBold/Bold weight hierarchy instead
// of suppressing synthetic emboldening via no-op FontVariation mappings.
val PlusJakartaSansFamily =
  FontFamily(
    Font(resId = R.font.plus_jakarta_sans, weight = FontWeight.Normal),
  )

val NotoSansBengaliFamily =
  FontFamily(
    Font(resId = R.font.noto_sans_bengali, weight = FontWeight.Normal),
  )

val JetBrainsMonoFamily =
  FontFamily(
    Font(resId = R.font.jetbrains_mono, weight = FontWeight.Normal),
  )

fun buildBdixTypography(
  bodyFontFamily: FontFamily,
  lineHeightScale: Float = 1f,
  disableLetterSpacing: Boolean = false,
): Typography {
  fun scaledLineHeight(baseSp: Float) = (baseSp * lineHeightScale).sp

  fun tracking(baseSp: Float): TextUnit = if (disableLetterSpacing) 0.sp else baseSp.sp

  return Typography(
    displaySmall =
      TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = scaledLineHeight(44f),
        letterSpacing = tracking(-0.4f),
      ),
    headlineMedium =
      TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = scaledLineHeight(32f),
        letterSpacing = tracking(-0.2f),
      ),
    headlineSmall =
      TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = scaledLineHeight(28f),
        letterSpacing = tracking(-0.1f),
      ),
    titleLarge =
      TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = scaledLineHeight(26f),
        letterSpacing = tracking(0f),
      ),
    titleMedium =
      TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = scaledLineHeight(22f),
        letterSpacing = tracking(0.1f),
      ),
    titleSmall =
      TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = scaledLineHeight(20f),
        letterSpacing = tracking(0.1f),
      ),
    bodyLarge =
      TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = scaledLineHeight(22f),
        letterSpacing = tracking(0f),
      ),
    bodyMedium =
      TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = scaledLineHeight(20f),
        letterSpacing = tracking(0f),
      ),
    bodySmall =
      TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = scaledLineHeight(18f),
        letterSpacing = tracking(0f),
      ),
    labelLarge =
      TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = scaledLineHeight(20f),
        letterSpacing = tracking(0.1f),
      ),
    labelMedium =
      TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = scaledLineHeight(16f),
        letterSpacing = tracking(0.15f),
      ),
    labelSmall =
      TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = scaledLineHeight(16f),
        letterSpacing = tracking(0.15f),
        fontFeatureSettings = "tnum",
      ),
  )
}

val BdixTypography =
  buildBdixTypography(
    bodyFontFamily = PlusJakartaSansFamily,
    lineHeightScale = 1f,
    disableLetterSpacing = false,
  )

val BdixTypographyBengali =
  buildBdixTypography(
    bodyFontFamily = NotoSansBengaliFamily,
    lineHeightScale = 1.14f,
    disableLetterSpacing = true,
  )

val MonoUrlStyle =
  TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.5.sp,
    lineHeight = 18.sp,
  )

val MonoBadgeStyle =
  TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 16.sp,
    fontFeatureSettings = "tnum",
  )

val MonoMetricStyle =
  TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    fontFeatureSettings = "tnum",
  )

val MonoHeroNumberStyle =
  TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 44.sp,
    lineHeight = 52.sp,
    letterSpacing = (-1.0).sp,
    fontFeatureSettings = "tnum",
  )

val MonoHeroSubNumberStyle =
  TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 24.sp,
    lineHeight = 32.sp,
    fontFeatureSettings = "tnum",
  )
