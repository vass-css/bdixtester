package com.jubayerhossain.bdixtester.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val BdixShapes =
  Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

object BdixSpacing {
  val xs = 4.dp
  val sm = 8.dp
  val md = 12.dp
  val lg = 16.dp
  val xl = 24.dp
  val xxl = 32.dp
}

object BdixIconSize {
  val xs = 14.dp
  val sm = 18.dp
  val md = 20.dp
  val lg = 24.dp
  val xl = 28.dp
}

object BdixBorder {
  val hairline = 1.dp
  val accent = 1.5.dp
  val hero = 2.dp
}

object BdixCardShapes {
  val chip = RoundedCornerShape(8.dp)
  val iconSquircle = RoundedCornerShape(12.dp)
  val input = RoundedCornerShape(14.dp)
  val richCard = RoundedCornerShape(20.dp)
  val profileHeader = RoundedCornerShape(24.dp)
  val pill = RoundedCornerShape(50)
}

object BdixElevation {
  val flat = 0.dp
  val bar = 2.dp
  val card = 2.dp
  val modal = 6.dp
}
