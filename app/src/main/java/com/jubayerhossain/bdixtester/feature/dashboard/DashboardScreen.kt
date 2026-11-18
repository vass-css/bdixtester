package com.jubayerhossain.bdixtester.feature.dashboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.network.NetworkSnapshot
import com.jubayerhossain.bdixtester.core.network.TransportType
import com.jubayerhossain.bdixtester.ui.components.BdixMotionSpecs
import com.jubayerhossain.bdixtester.ui.components.LocalReducedMotion
import com.jubayerhossain.bdixtester.ui.components.LtrContent
import com.jubayerhossain.bdixtester.ui.components.PulsingStatusDot
import com.jubayerhossain.bdixtester.ui.components.RollingCountText
import com.jubayerhossain.bdixtester.ui.components.ShimmerPillOverlay
import com.jubayerhossain.bdixtester.ui.components.springPressScale
import com.jubayerhossain.bdixtester.ui.theme.BdixBorder
import com.jubayerhossain.bdixtester.ui.theme.BdixIconSize
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing
import com.jubayerhossain.bdixtester.ui.theme.BdixTesterTheme
import com.jubayerhossain.bdixtester.ui.theme.LocalBdixSemanticColors
import com.jubayerhossain.bdixtester.ui.theme.MonoHeroNumberStyle
import com.jubayerhossain.bdixtester.ui.theme.MonoHeroSubNumberStyle
import com.jubayerhossain.bdixtester.ui.theme.MonoUrlStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DashboardScreen(
  uiState: DashboardUiState,
  onViewServers: () -> Unit,
  onStartScan: (ScanMode) -> Unit,
  onStopScan: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val semantic = LocalBdixSemanticColors.current
  var showWifiWarningDialog by remember { mutableStateOf(false) }

  val progress = uiState.scanProgress
  val isScanning = progress.isScanning
  val totalCount = if (isScanning && progress.totalCount > 0) progress.totalCount else uiState.totalServers
  val workingCount = if (isScanning) progress.reachableCount else uiState.reachableCount

  // Subtle completion haptic when a scan finishes naturally
  var wasScanning by remember { mutableStateOf(false) }
  LaunchedEffect(isScanning) {
    if (wasScanning && !isScanning && uiState.testedCount > 0) {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    wasScanning = isScanning
  }

  val triggerStartScan = {
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    if (uiState.networkSnapshot.hasWifiTransport) {
      onStartScan(uiState.selectedScanMode)
    } else {
      showWifiWarningDialog = true
    }
  }

  val reducedMotion = LocalReducedMotion.current
  val ambientAlpha =
    if (isScanning && !reducedMotion) {
      val ambientTransition = rememberInfiniteTransition(label = "home_ambient_canvas")
      ambientTransition
        .animateFloat(
          initialValue = 0.08f,
          targetValue = 0.16f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "ambient_aura_alpha",
        ).value
    } else {
      0.10f
    }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .drawBehind {
          // Full-bleed calm atmospheric radial glow behind the Orbital Radar center stage
          val auraCenter = Offset(size.width * 0.5f, size.height * 0.44f)
          val auraRadius = size.maxDimension * 0.56f
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    semantic.ringGradientEnd.copy(alpha = ambientAlpha),
                    semantic.ringGradientStart.copy(alpha = ambientAlpha * 0.4f),
                    Color.Transparent,
                  ),
                center = auraCenter,
                radius = auraRadius,
              ),
            radius = auraRadius,
            center = auraCenter,
          )
        }.testTag("dashboard_screen"),
  ) {
    val isWideLayout = maxWidth >= 600.dp
    val isCompactHeight = maxHeight < 500.dp

    if (isWideLayout) {
      // Adaptive Landscape / Tablet 2-Column Split
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
      ) {
        HomeBrandTopBar(
          networkSnapshot = uiState.networkSnapshot,
          isScanning = isScanning,
        )
        Row(
          modifier =
            Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(horizontal = BdixSpacing.xl, vertical = BdixSpacing.md),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(
            modifier =
              Modifier
                .weight(1.15f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            val ringDiameter = (this@BoxWithConstraints.maxHeight * 0.68f).coerceIn(220.dp, 320.dp)
            HeroTesterRing(
              ringSize = ringDiameter,
              isScanning = isScanning,
              isStarting = showWifiWarningDialog,
              progressFraction = progress.progressFraction,
              workingCount = workingCount,
              totalCount = totalCount,
              onViewClick = onViewServers,
            )
            CurrentlyTestingLine(
              visible = isScanning && !progress.currentServerName.isNullOrBlank(),
              serverName = progress.currentServerName.orEmpty(),
              modifier = Modifier.padding(top = BdixSpacing.md),
            )
          }

          Column(
            modifier =
              Modifier
                .weight(0.85f)
                .fillMaxHeight()
                .padding(horizontal = BdixSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            PrimaryTestActionButton(
              isScanning = isScanning,
              onStartClick = triggerStartScan,
              onStopClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onStopScan()
              },
            )
          }
        }
      }
    } else if (isCompactHeight) {
      // Ultra-compact portrait fallback (split-screen / tiny viewport) with safe scroll
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        HomeBrandTopBar(
          networkSnapshot = uiState.networkSnapshot,
          isScanning = isScanning,
        )
        Spacer(modifier = Modifier.height(BdixSpacing.md))
        HeroTesterRing(
          ringSize = (this@BoxWithConstraints.maxWidth * 0.72f).coerceIn(220.dp, 280.dp),
          isScanning = isScanning,
          isStarting = showWifiWarningDialog,
          progressFraction = progress.progressFraction,
          workingCount = workingCount,
          totalCount = totalCount,
          onViewClick = onViewServers,
        )
        CurrentlyTestingLine(
          visible = isScanning && !progress.currentServerName.isNullOrBlank(),
          serverName = progress.currentServerName.orEmpty(),
          modifier = Modifier.padding(top = BdixSpacing.sm),
        )
        Spacer(modifier = Modifier.height(BdixSpacing.xl))
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = BdixSpacing.xl, vertical = BdixSpacing.lg),
          contentAlignment = Alignment.Center,
        ) {
          PrimaryTestActionButton(
            isScanning = isScanning,
            onStartClick = triggerStartScan,
            onStopClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onStopScan()
            },
          )
        }
      }
    } else {
      // Sculptural Full-Height Portrait Canvas (Zero dead lower space, zero extra clutter)
      val heroDiameter =
        min(
          maxWidth * 0.86f,
          (maxHeight - 210.dp).coerceAtLeast(240.dp),
        ).coerceIn(256.dp, 344.dp)

      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // 1. Minimal Brand Top Bar + Live Pulsing IP Pill
        HomeBrandTopBar(
          networkSnapshot = uiState.networkSnapshot,
          isScanning = isScanning,
        )

        // 2. Sculptural Center Stage — Oversized Orbital Radar Hero Ring
        Column(
          modifier =
            Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(horizontal = BdixSpacing.lg),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          HeroTesterRing(
            ringSize = heroDiameter,
            isScanning = isScanning,
            isStarting = showWifiWarningDialog,
            progressFraction = progress.progressFraction,
            workingCount = workingCount,
            totalCount = totalCount,
            onViewClick = onViewServers,
          )

          CurrentlyTestingLine(
            visible = isScanning && !progress.currentServerName.isNullOrBlank(),
            serverName = progress.currentServerName.orEmpty(),
            modifier = Modifier.padding(top = BdixSpacing.md),
          )
        }

        // 3. Bottom-Anchored Gradient Shimmer Start / Stop Pill (Natural Thumb Zone)
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(
                start = BdixSpacing.xl,
                end = BdixSpacing.xl,
                top = BdixSpacing.sm,
                bottom = 28.dp,
              ),
          contentAlignment = Alignment.Center,
        ) {
          PrimaryTestActionButton(
            isScanning = isScanning,
            onStartClick = triggerStartScan,
            onStopClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onStopScan()
            },
          )
        }
      }
    }
  }

  if (showWifiWarningDialog) {
    AlertDialog(
      onDismissRequest = { showWifiWarningDialog = false },
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      title = {
        Text(
          text = stringResource(R.string.wifi_warning_title),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
      },
      text = {
        Text(
          text = stringResource(R.string.wifi_warning_body),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      },
      dismissButton = {
        TextButton(
          onClick = {
            showWifiWarningDialog = false
            try {
              val intent =
                Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
              context.startActivity(intent)
            } catch (_: Exception) {
              // Gracefully ignore if settings activity is unavailable
            }
          },
          modifier =
            Modifier
              .heightIn(min = 48.dp)
              .testTag("wifi_warning_settings_button"),
        ) {
          Text(
            text = stringResource(R.string.wifi_warning_settings),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            showWifiWarningDialog = false
            onStartScan(uiState.selectedScanMode)
          },
          modifier =
            Modifier
              .heightIn(min = 48.dp)
              .testTag("wifi_warning_continue_button"),
        ) {
          Text(
            text = stringResource(R.string.wifi_warning_continue),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      },
    )
  }
}

@Composable
private fun HomeBrandTopBar(
  networkSnapshot: NetworkSnapshot,
  isScanning: Boolean,
) {
  val semantic = LocalBdixSemanticColors.current
  val (dotColor, transportLabel) =
    when {
      !networkSnapshot.isConnected -> semantic.unreachable to stringResource(R.string.home_ip_pill_offline)
      networkSnapshot.transportType == TransportType.WIFI ->
        semantic.reachable to
          stringResource(R.string.home_network_wifi)
      networkSnapshot.transportType == TransportType.ETHERNET ->
        semantic.reachable to
          stringResource(R.string.home_network_ethernet)
      else -> semantic.restricted to stringResource(R.string.home_network_mobile)
    }
  val ipText = networkSnapshot.localIpAddress?.takeIf { it.isNotBlank() }
  val pillLabel =
    if (networkSnapshot.isConnected && ipText != null) {
      stringResource(R.string.home_ip_pill_connected, transportLabel, ipText)
    } else {
      transportLabel
    }

  val reducedMotion = LocalReducedMotion.current
  val radarRotation =
    if (isScanning && !reducedMotion) {
      val iconSpinTransition = rememberInfiniteTransition(label = "brand_radar_spin")
      iconSpinTransition
        .animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1800, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "brand_radar_angle",
        ).value
    } else {
      0f
    }

  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = BdixSpacing.lg, vertical = BdixSpacing.md),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
    ) {
      Surface(
        shape = RoundedCornerShape(11.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
        modifier = Modifier.size(34.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.Radar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier =
              Modifier
                .size(19.dp)
                .graphicsLayer { rotationZ = radarRotation },
          )
        }
      }
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Surface(
      shape = RoundedCornerShape(50),
      color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
      border = BorderStroke(BdixBorder.hairline, MaterialTheme.colorScheme.outlineVariant),
      tonalElevation = 1.dp,
      modifier = Modifier.testTag("home_top_ip_pill"),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        PulsingStatusDot(
          color = dotColor,
          isLive = networkSnapshot.isConnected,
          dotSize = 7.dp,
        )
        LtrContent {
          Text(
            text = pillLabel,
            style = MonoUrlStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun PrimaryTestActionButton(
  isScanning: Boolean,
  onStartClick: () -> Unit,
  onStopClick: () -> Unit,
) {
  val semantic = LocalBdixSemanticColors.current
  val interactionSource = remember { MutableInteractionSource() }
  val pillShape = RoundedCornerShape(50)

  val startColor by animateColorAsState(
    targetValue = if (isScanning) semantic.stopGradientStart else semantic.ctaGradientStart,
    animationSpec = tween(320),
    label = "cta_start_color",
  )
  val endColor by animateColorAsState(
    targetValue = if (isScanning) semantic.stopGradientEnd else semantic.ctaGradientEnd,
    animationSpec = tween(320),
    label = "cta_end_color",
  )
  val contentColor by animateColorAsState(
    targetValue = if (isScanning) semantic.stopContent else semantic.ctaContent,
    animationSpec = tween(220),
    label = "cta_content_color",
  )

  val gradientBrush =
    Brush.horizontalGradient(
      colors = listOf(startColor, endColor),
    )

  val actionLabel =
    stringResource(
      if (isScanning) R.string.home_action_stop else R.string.home_action_start_test,
    )

  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .widthIn(max = 380.dp)
        .springPressScale(interactionSource = interactionSource, pressedScale = 0.96f)
        .shadow(
          elevation = if (isScanning) 10.dp else 14.dp,
          shape = pillShape,
          ambientColor = startColor.copy(alpha = 0.36f),
          spotColor = startColor.copy(alpha = 0.44f),
        ).clip(pillShape)
        .background(brush = gradientBrush)
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(color = contentColor.copy(alpha = 0.22f)),
          role = Role.Button,
          onClickLabel = actionLabel,
          onClick = if (isScanning) onStopClick else onStartClick,
        ).heightIn(min = 58.dp)
        .testTag(if (isScanning) "home_stop_test_button" else "home_start_test_button"),
    contentAlignment = Alignment.Center,
  ) {
    // Gliding diagonal light shimmer only while actively scanning and motion is allowed
    ShimmerPillOverlay(
      modifier = Modifier.matchParentSize(),
      shimmerColor = contentColor.copy(alpha = 0.18f),
      durationMillis = 1700,
      enabled = isScanning && !LocalReducedMotion.current,
    )

    AnimatedContent(
      targetState = isScanning,
      transitionSpec = {
        (fadeIn(tween(220)) + scaleIn(initialScale = 0.92f)) togetherWith
          (fadeOut(tween(160)) + scaleOut(targetScale = 0.92f))
      },
      label = "cta_content_morph",
    ) { scanning ->
      Row(
        modifier = Modifier.padding(horizontal = BdixSpacing.xl, vertical = BdixSpacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = if (scanning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(if (scanning) BdixIconSize.md else 24.dp),
        )
        Spacer(modifier = Modifier.width(BdixSpacing.sm))
        Text(
          text =
            stringResource(
              if (scanning) R.string.home_action_stop else R.string.home_action_start_test,
            ),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = contentColor,
        )
      }
    }
  }
}

@Composable
private fun CurrentlyTestingLine(
  visible: Boolean,
  serverName: String,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(220)),
    exit = fadeOut(tween(180)),
    modifier = modifier,
  ) {
    LtrContent {
      Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.86f),
        border = BorderStroke(BdixBorder.hairline, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Text(
          text = stringResource(R.string.home_currently_testing, serverName),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          modifier =
            Modifier
              .padding(horizontal = BdixSpacing.md, vertical = 6.dp)
              .semantics { liveRegion = LiveRegionMode.Polite }
              .testTag("home_currently_testing_label"),
        )
      }
    }
  }
}

@Composable
private fun HeroTesterRing(
  ringSize: Dp,
  isScanning: Boolean,
  isStarting: Boolean,
  progressFraction: Float,
  workingCount: Int,
  totalCount: Int,
  onViewClick: () -> Unit,
) {
  val semantic = LocalBdixSemanticColors.current
  val haptic = LocalHapticFeedback.current
  val viewInteractionSource = remember { MutableInteractionSource() }

  val reducedMotion = LocalReducedMotion.current
  val activeMotion = (isScanning || isStarting) && !reducedMotion

  val animatedProgress by animateFloatAsState(
    targetValue = progressFraction.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = if (reducedMotion) 0 else 300, easing = FastOutSlowInEasing),
    label = "scan_progress",
  )

  val ringBreathingScale by animateFloatAsState(
    targetValue = if (activeMotion) 1.02f else 1f,
    animationSpec = if (reducedMotion) tween(0) else BdixMotionSpecs.bouncySpring,
    label = "scan_ring_scale",
  )

  // Calm Idle / Energetic Active: allocate zero infinite transitions while idle or when Reduced Motion is active
  val scanSpinAngle: Float
  val orbitAngle: Float
  val tickAngle: Float
  val sonarWave1: Float
  val sonarWave2: Float
  val glowAlpha: Float

  if (activeMotion) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbital_radar_motion")
    scanSpinAngle =
      infiniteTransition
        .animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 2200, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "scan_spin_angle",
        ).value
    orbitAngle =
      infiniteTransition
        .animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 5500, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "orbit_angle",
        ).value
    tickAngle =
      infiniteTransition
        .animateFloat(
          initialValue = 360f,
          targetValue = 0f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 9000, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "tick_angle",
        ).value
    sonarWave1 =
      infiniteTransition
        .animateFloat(
          initialValue = 0f,
          targetValue = 1f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "sonar_wave_1",
        ).value
    sonarWave2 =
      infiniteTransition
        .animateFloat(
          initialValue = 0f,
          targetValue = 1f,
          animationSpec =
            infiniteRepeatable(
              animation =
                tween(
                  durationMillis = 1900,
                  delayMillis = 950,
                  easing = FastOutSlowInEasing,
                ),
              repeatMode = RepeatMode.Restart,
            ),
          label = "sonar_wave_2",
        ).value
    glowAlpha =
      infiniteTransition
        .animateFloat(
          initialValue = 0.16f,
          targetValue = 0.28f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "hero_glow_alpha",
        ).value
  } else {
    scanSpinAngle = 0f
    orbitAngle = 0f
    tickAngle = 0f
    sonarWave1 = -1f
    sonarWave2 = -1f
    glowAlpha = 0.14f
  }

  val ringColors =
    remember(semantic) {
      listOf(
        semantic.ringGradientStart,
        semantic.ringGradientEnd,
        semantic.ringGradientStart,
      )
    }
  val dashedOrbitEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 14f), 0f) }

  val ringA11yDescription =
    if (isScanning) {
      stringResource(
        R.string.home_ring_a11y_scanning,
        "%.1f".format(Locale.US, animatedProgress * 100f),
        workingCount,
      )
    } else {
      stringResource(R.string.home_ring_a11y_idle, workingCount, totalCount)
    }

  Box(
    modifier =
      Modifier
        .size(ringSize)
        .graphicsLayer {
          scaleX = ringBreathingScale
          scaleY = ringBreathingScale
        }.testTag("home_tester_ring"),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val fullRadius = size.minDimension / 2f

      // Layer 1: Soft ambient radial nebula aura
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                semantic.ringGradientEnd.copy(alpha = glowAlpha),
                semantic.ringGradientStart.copy(alpha = glowAlpha * 0.42f),
                Color.Transparent,
              ),
            center = center,
            radius = fullRadius,
          ),
        radius = fullRadius,
        center = center,
      )

      val mainRingRadius = fullRadius * 0.77f
      val orbitRadius = fullRadius * 0.91f
      val tickOuterRadius = mainRingRadius + 13.dp.toPx()
      val tickInnerRadius = mainRingRadius + 8.dp.toPx()

      // Layer 2: Two staggered expanding sonar pulse waves (active scan only)
      if (activeMotion) {
        listOf(sonarWave1, sonarWave2).forEach { wave ->
          if (wave >= 0f) {
            val waveRadius = mainRingRadius + (fullRadius - mainRingRadius) * wave
            val waveAlpha = (1f - wave) * 0.34f
            drawCircle(
              color = semantic.ringGradientEnd.copy(alpha = waveAlpha),
              radius = waveRadius,
              center = center,
              style = Stroke(width = (2.2f - wave * 1.4f).dp.toPx()),
            )
          }
        }
      }

      // Layer 3: Outer rotating dashed orbital track + 3 glowing orbiting particles
      rotate(degrees = orbitAngle, pivot = center) {
        drawCircle(
          color = semantic.ringGradientStart.copy(alpha = if (isScanning) 0.32f else 0.22f),
          radius = orbitRadius,
          center = center,
          style = Stroke(width = 1.4.dp.toPx(), pathEffect = dashedOrbitEffect),
        )

        val particleAngles = listOf(0.0, 120.0, 240.0)
        val particleColors =
          listOf(
            semantic.ringGradientStart,
            semantic.ringGradientEnd,
            semantic.ringGradientStart,
          )
        particleAngles.forEachIndexed { idx, deg ->
          val rad = deg * PI / 180.0
          val px = center.x + (orbitRadius * cos(rad)).toFloat()
          val py = center.y + (orbitRadius * sin(rad)).toFloat()
          val pos = Offset(px, py)
          val pColor = particleColors[idx]
          val coreRadius = if (idx == 0) 4.2.dp.toPx() else 3.2.dp.toPx()

          // Particle halo glow
          drawCircle(
            color = pColor.copy(alpha = 0.28f),
            radius = coreRadius * 2.4f,
            center = pos,
          )
          // Particle crisp luminous core
          drawCircle(
            color = pColor,
            radius = coreRadius,
            center = pos,
          )
        }
      }

      // Layer 4: Counter-rotating precision 36-tick telemetry ring
      rotate(degrees = tickAngle, pivot = center) {
        val tickCount = 36
        for (i in 0 until tickCount) {
          val deg = i * (360.0 / tickCount)
          val rad = deg * PI / 180.0
          val isMajor = i % 3 == 0
          val innerR = if (isMajor) tickInnerRadius - 2.dp.toPx() else tickInnerRadius
          val start =
            Offset(
              center.x + (innerR * cos(rad)).toFloat(),
              center.y + (innerR * sin(rad)).toFloat(),
            )
          val end =
            Offset(
              center.x + (tickOuterRadius * cos(rad)).toFloat(),
              center.y + (tickOuterRadius * sin(rad)).toFloat(),
            )
          drawLine(
            color = semantic.ringGradientEnd.copy(alpha = if (isMajor) 0.42f else 0.18f),
            start = start,
            end = end,
            strokeWidth = if (isMajor) 1.6.dp.toPx() else 1.dp.toPx(),
            cap = StrokeCap.Round,
          )
        }
      }

      // Layer 5: Primary gradient ring + inner soft ring + scanning sweep comet
      val outerStrokePx = 12.dp.toPx()
      val innerStrokePx = 4.dp.toPx()
      val outerTopLeft = Offset(center.x - mainRingRadius, center.y - mainRingRadius)
      val outerArcSize = Size(mainRingRadius * 2f, mainRingRadius * 2f)

      val innerRingRadius = (mainRingRadius - outerStrokePx * 0.95f).coerceAtLeast(0f)
      val innerTopLeft = Offset(center.x - innerRingRadius, center.y - innerRingRadius)
      val innerArcSize = Size(innerRingRadius * 2f, innerRingRadius * 2f)

      val sweepBrush = Brush.sweepGradient(colors = ringColors, center = center)
      val innerSoftBrush =
        Brush.sweepGradient(
          colors = ringColors.map { it.copy(alpha = 0.30f) },
          center = center,
        )

      // Soft ring halo
      drawArc(
        color = semantic.ringGradientEnd.copy(alpha = if (isScanning) 0.24f else glowAlpha * 0.78f),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = outerTopLeft,
        size = outerArcSize,
        style = Stroke(width = outerStrokePx + 12.dp.toPx()),
      )

      if (isScanning || isStarting) {
        drawArc(
          color = semantic.ringTrack,
          startAngle = 0f,
          sweepAngle = 360f,
          useCenter = false,
          topLeft = outerTopLeft,
          size = outerArcSize,
          style = Stroke(width = outerStrokePx),
        )
        val sweepDegrees = if (isStarting) 250f else (84f + 276f * animatedProgress).coerceAtMost(360f)
        rotate(degrees = scanSpinAngle, pivot = center) {
          drawArc(
            brush = sweepBrush,
            startAngle = -90f,
            sweepAngle = sweepDegrees,
            useCenter = false,
            topLeft = outerTopLeft,
            size = outerArcSize,
            style = Stroke(width = outerStrokePx, cap = StrokeCap.Round),
          )
          drawArc(
            brush = innerSoftBrush,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = innerTopLeft,
            size = innerArcSize,
            style = Stroke(width = innerStrokePx),
          )

          // Bright luminous comet head at the leading edge of the scan arc
          val headRad = (-90.0 + sweepDegrees) * PI / 180.0
          val headPos =
            Offset(
              center.x + (mainRingRadius * cos(headRad)).toFloat(),
              center.y + (mainRingRadius * sin(headRad)).toFloat(),
            )
          drawCircle(
            color = semantic.ringCometHead.copy(alpha = 0.92f),
            radius = 4.5.dp.toPx(),
            center = headPos,
          )
        }
      } else {
        rotate(degrees = orbitAngle * 0.35f, pivot = center) {
          drawArc(
            brush = sweepBrush,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = outerTopLeft,
            size = outerArcSize,
            style = Stroke(width = outerStrokePx, cap = StrokeCap.Round),
          )
        }
        drawArc(
          brush = innerSoftBrush,
          startAngle = -90f,
          sweepAngle = 360f,
          useCenter = false,
          topLeft = innerTopLeft,
          size = innerArcSize,
          style = Stroke(width = innerStrokePx),
        )
      }
    }

    // Layer 6: Interactive Core with Rolling Counter & Spring-Bouncy VIEW Pill
    Column(
      modifier = Modifier.padding(horizontal = BdixSpacing.xxl),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(BdixSpacing.xs),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BdixSpacing.xs),
        modifier =
          Modifier.semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
            contentDescription = ringA11yDescription
          },
      ) {
        when {
          isStarting -> {
            Text(
              text = stringResource(R.string.home_starting_title),
              style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center,
            )
            Text(
              text = stringResource(R.string.home_checking_internet),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }

          isScanning -> {
            LtrContent {
              Row(verticalAlignment = Alignment.Bottom) {
                Text(
                  text = "%.1f".format(Locale.US, animatedProgress * 100f),
                  style = MonoHeroNumberStyle,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "%",
                  style = MonoHeroSubNumberStyle,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(bottom = 6.dp, start = 2.dp),
                )
              }
            }
            Text(
              text = stringResource(R.string.home_testing_wait),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
            Text(
              text = stringResource(R.string.home_working_servers_count_live, workingCount),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = semantic.reachable,
              textAlign = TextAlign.Center,
            )
          }

          else -> {
            LtrContent {
              Row(verticalAlignment = Alignment.Bottom) {
                RollingCountText(
                  count = workingCount,
                  style = MonoHeroNumberStyle,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "/$totalCount",
                  style = MonoHeroSubNumberStyle,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(bottom = 6.dp, start = 2.dp),
                )
              }
            }
            Text(
              text = stringResource(R.string.home_working_servers_found),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(BdixSpacing.xs))

      OutlinedButton(
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onViewClick()
        },
        interactionSource = viewInteractionSource,
        shape = RoundedCornerShape(50),
        border = BorderStroke(BdixBorder.accent, MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp),
        modifier =
          Modifier
            .springPressScale(interactionSource = viewInteractionSource, pressedScale = 0.94f)
            .heightIn(min = 48.dp)
            .minimumInteractiveComponentSize()
            .testTag("home_view_servers_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Dns,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = stringResource(R.string.home_action_view),
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
  BdixTesterTheme {
    DashboardScreen(
      uiState =
        DashboardUiState(
          totalServers = 752,
          testedCount = 752,
          reachableCount = 41,
          failedCount = 711,
          networkSnapshot =
            NetworkSnapshot(
              isConnected = true,
              transportType = TransportType.WIFI,
              hasWifiTransport = true,
              localIpAddress = "192.168.0.104",
            ),
        ),
      onViewServers = {},
      onStartScan = {},
      onStopScan = {},
    )
  }
}
