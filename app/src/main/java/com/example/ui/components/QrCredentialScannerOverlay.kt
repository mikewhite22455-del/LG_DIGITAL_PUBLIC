package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.core.ExchangeQrCredentialParser
import com.example.core.ParsedExchangeQrCredentials
import com.example.core.QrCredentialParseResult
import com.example.core.QrExchangeTarget
import com.example.data.local.EncryptedExchangeCredentialStore
import com.example.ui.theme.LgBrandFontFamily
import com.example.ui.theme.LgColors
import com.example.ui.theme.TabLabelTextStyle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

/**
 * Full-screen CameraX + ML Kit QR Code Scanner overlay for importing API credentials
 * directly from Kraken Pro, Coinbase Advanced, KuCoin, Bitstamp, and LG Render portals.
 */
@Composable
fun QrCredentialScannerOverlay(
    initialTarget: QrExchangeTarget = QrExchangeTarget.AUTO,
    onFillFieldsOnly: (ParsedExchangeQrCredentials) -> Unit,
    onSaveAndVerify: (ParsedExchangeQrCredentials) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var selectedTarget by remember(initialTarget) { mutableStateOf(initialTarget) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionRequestedOnce = true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var torchEnabled by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    var detectedCredentials by remember { mutableStateOf<ParsedExchangeQrCredentials?>(null) }
    var scanFeedbackError by remember { mutableStateOf<String?>(null) }
    var lastRejectedHash by remember { mutableStateOf<Int?>(null) }

    var showManualPayloadInput by remember { mutableStateOf(false) }
    var manualPayloadText by remember { mutableStateOf("") }

    fun handleRawPayload(raw: String) {
        if (detectedCredentials != null) return
        when (val result = ExchangeQrCredentialParser.parse(raw, selectedTarget)) {
            is QrCredentialParseResult.Success -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scanFeedbackError = null
                lastRejectedHash = null
                detectedCredentials = result.credentials
            }
            is QrCredentialParseResult.Invalid -> {
                val hash = raw.hashCode()
                if (lastRejectedHash != hash) {
                    lastRejectedHash = hash
                    scanFeedbackError = result.reason
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LgColors.ObsidianBg)
            .testTag("qr_credential_scanner_overlay")
    ) {
        // 1. Live CameraX Preview + ML Kit QR Analyzer when CAMERA permission is granted
        if (hasCameraPermission && !showManualPayloadInput) {
            CameraXQrPreview(
                useFrontCamera = useFrontCamera,
                torchEnabled = torchEnabled,
                isPaused = detectedCredentials != null,
                onFlashAvailabilityChanged = { available ->
                    hasFlashUnit = available
                    if (!available) torchEnabled = false
                },
                onCameraError = { message -> cameraError = message },
                onQrDetected = { raw -> handleRawPayload(raw) },
                modifier = Modifier.fillMaxSize()
            )

            // Cyber Turquoise & Gold scanning reticle overlay
            QrScannerReticleOverlay(
                isLockedOn = detectedCredentials != null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LgCircuitBackground(modifier = Modifier.fillMaxSize()) {}
        }

        // 2. Foreground HUD Controls (safe drawing insets)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar + Target Exchange Selector Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                LgColors.ObsidianBg.copy(alpha = 0.94f),
                                LgColors.ObsidianBg.copy(alpha = 0.72f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(LgColors.PanelSlate)
                                .border(1.5.dp, LgColors.Turquoise, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = null,
                                tint = LgColors.Turquoise,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.qr_scanner_title),
                                fontFamily = LgBrandFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                letterSpacing = 1.2.sp,
                                color = LgColors.GoldBright,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.qr_scanner_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = LgColors.Turquoise,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (hasCameraPermission && !showManualPayloadInput && hasFlashUnit) {
                            IconButton(
                                onClick = { torchEnabled = !torchEnabled },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("qr_scanner_torch_button")
                            ) {
                                Icon(
                                    imageVector = if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                    contentDescription = stringResource(R.string.qr_scanner_toggle_torch),
                                    tint = if (torchEnabled) LgColors.GoldBright else LgColors.InkPrimary
                                )
                            }
                        }
                        if (hasCameraPermission && !showManualPayloadInput) {
                            IconButton(
                                onClick = { useFrontCamera = !useFrontCamera },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("qr_scanner_switch_camera_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Cameraswitch,
                                    contentDescription = stringResource(R.string.qr_scanner_switch_camera),
                                    tint = LgColors.Turquoise
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("qr_scanner_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.qr_scanner_close),
                                tint = LgColors.GoldBright
                            )
                        }
                    }
                }

                // Target Exchange Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QrExchangeTarget.entries.forEach { target ->
                        val isSelected = selectedTarget == target
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) LgColors.Turquoise else LgColors.PanelSlate.copy(alpha = 0.88f),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) LgColors.GoldBright else LgColors.Gold.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    selectedTarget = target
                                    detectedCredentials = detectedCredentials?.reassignedTo(target)
                                }
                                .testTag("qr_target_chip_${target.id}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = target.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) LgColors.ObsidianBg else LgColors.InkPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Center State: Permission Denied / Camera Unavailable / Manual Payload Import
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    showManualPayloadInput -> {
                        ManualPayloadImportCard(
                            payload = manualPayloadText,
                            onPayloadChange = {
                                manualPayloadText = it
                                scanFeedbackError = null
                            },
                            errorMessage = scanFeedbackError,
                            onParse = { handleRawPayload(manualPayloadText) },
                            onReturnToCamera = {
                                showManualPayloadInput = false
                                scanFeedbackError = null
                            }
                        )
                    }
                    !hasCameraPermission -> {
                        CameraPermissionCard(
                            permissionRequestedOnce = permissionRequestedOnce,
                            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            onOpenSettings = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            },
                            onSwitchToManualImport = { showManualPayloadInput = true }
                        )
                    }
                    cameraError != null -> {
                        GlassPanelCard(highlightBorder = LgColors.Crimson.copy(alpha = 0.8f)) {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = 460.dp)
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = LgColors.Crimson,
                                    modifier = Modifier.size(30.dp)
                                )
                                Text(
                                    text = "Camera Feed Unavailable",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = LgColors.GoldBright,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = cameraError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LgColors.InkSecondary,
                                    textAlign = TextAlign.Center
                                )
                                GoldMetallicButton(
                                    text = "IMPORT JSON / URI TEXT",
                                    onClick = { showManualPayloadInput = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("qr_fallback_manual_import_button")
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Tray: Detected Credential Confirmation OR Live Scan Guidance
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                LgColors.ObsidianBg.copy(alpha = 0.88f),
                                LgColors.ObsidianBg.copy(alpha = 0.98f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(
                    visible = detectedCredentials != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    detectedCredentials?.let { parsed ->
                        DetectedCredentialConfirmationCard(
                            credentials = parsed,
                            onChangeExchange = { newTarget ->
                                selectedTarget = newTarget
                                detectedCredentials = parsed.reassignedTo(newTarget)
                            },
                            onSaveAndVerify = {
                                onSaveAndVerify(parsed)
                                onDismiss()
                            },
                            onFillOnly = {
                                onFillFieldsOnly(parsed)
                                onDismiss()
                            },
                            onRescan = {
                                detectedCredentials = null
                                scanFeedbackError = null
                                lastRejectedHash = null
                            }
                        )
                    }
                }

                if (detectedCredentials == null) {
                    scanFeedbackError?.let { err ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LgColors.PanelSlate.copy(alpha = 0.94f),
                            modifier = Modifier
                                .widthIn(max = 540.dp)
                                .fillMaxWidth()
                                .border(1.dp, LgColors.Crimson.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = LgColors.Crimson,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LgColors.InkPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (hasCameraPermission && !showManualPayloadInput) {
                        Column(
                            modifier = Modifier.widthIn(max = 540.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.qr_scanner_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = LgColors.GoldBright,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Supports Kraken Pro (kraken://), Coinbase CDP JSON, KuCoin, Bitstamp & LG Token QR codes · 100% On-Device",
                                style = MaterialTheme.typography.labelSmall,
                                color = LgColors.InkSecondary,
                                textAlign = TextAlign.Center
                            )
                            TextButton(
                                onClick = { showManualPayloadInput = true },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .semantics(mergeDescendants = true) {}
                                    .testTag("qr_paste_payload_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentPaste,
                                    contentDescription = null,
                                    tint = LgColors.Turquoise,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = "Or paste Coinbase CDP JSON / Portal URI",
                                    color = LgColors.Turquoise,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraXQrPreview(
    useFrontCamera: Boolean,
    torchEnabled: Boolean,
    isPaused: Boolean,
    onFlashAvailabilityChanged: (Boolean) -> Unit,
    onCameraError: (String) -> Unit,
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }

    // Keep latest callback / pause state accessible without recreating analyzer
    var pausedState by remember { mutableStateOf(isPaused) }
    pausedState = isPaused

    var qrCallback by remember { mutableStateOf(onQrDetected) }
    qrCallback = onQrDetected

    LaunchedEffect(cameraController, useFrontCamera) {
        val preferredSelector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        runCatching {
            val selector = when {
                cameraController.hasCamera(preferredSelector) -> preferredSelector
                cameraController.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                cameraController.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                else -> preferredSelector
            }
            cameraController.cameraSelector = selector
            onFlashAvailabilityChanged(cameraController.cameraInfo?.hasFlashUnit() == true)
        }.onFailure { err ->
            onCameraError(err.localizedMessage ?: "Unable to initialize device camera.")
        }
    }

    LaunchedEffect(cameraController, torchEnabled) {
        runCatching {
            if (cameraController.cameraInfo?.hasFlashUnit() == true) {
                cameraController.enableTorch(torchEnabled)
            }
        }
    }

    DisposableEffect(lifecycleOwner, cameraController, barcodeScanner) {
        val analyzer = MlKitAnalyzer(
            listOf(barcodeScanner),
            CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
            mainExecutor
        ) { result ->
            if (pausedState) return@MlKitAnalyzer
            val barcodes = result?.getValue(barcodeScanner) ?: return@MlKitAnalyzer
            val qrText = barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }
            if (qrText != null) {
                qrCallback(qrText)
            }
        }

        runCatching {
            cameraController.setImageAnalysisAnalyzer(mainExecutor, analyzer)
            cameraController.bindToLifecycle(lifecycleOwner)
            cameraController.initializationFuture.addListener(
                {
                    onFlashAvailabilityChanged(cameraController.cameraInfo?.hasFlashUnit() == true)
                },
                mainExecutor
            )
        }.onFailure { err ->
            onCameraError(err.localizedMessage ?: "CameraX failed to bind to lifecycle.")
        }

        onDispose {
            runCatching { cameraController.enableTorch(false) }
            runCatching { cameraController.clearImageAnalysisAnalyzer() }
            runCatching { cameraController.unbind() }
            runCatching { barcodeScanner.close() }
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                controller = cameraController
            }
        },
        update = { previewView ->
            if (previewView.controller !== cameraController) {
                previewView.controller = cameraController
            }
        },
        modifier = modifier.testTag("camerax_qr_preview_view")
    )
}

@Composable
private fun QrScannerReticleOverlay(
    isLockedOn: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "qr_laser_scan")
    val scanProgress by transition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "qr_scan_progress"
    )

    val cornerColor = if (isLockedOn) LgColors.Turquoise else LgColors.GoldBright
    val laserColor = LgColors.Turquoise

    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
    ) {
        val boxSide = (minOf(size.width, size.height) * 0.66f).coerceIn(220.dp.toPx(), 320.dp.toPx())
        val left = (size.width - boxSide) / 2f
        val top = (size.height - boxSide) / 2f - 24.dp.toPx()
        val cornerRadius = 18.dp.toPx()

        // Dimmed surround
        drawRect(color = Color.Black.copy(alpha = 0.56f))

        // Clear viewfinder cutout
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxSide, boxSide),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            blendMode = BlendMode.Clear
        )

        // Subtle outer hairline
        drawRoundRect(
            color = cornerColor.copy(alpha = 0.35f),
            topLeft = Offset(left, top),
            size = Size(boxSide, boxSide),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Four L-shaped cyber corner brackets
        val bracketLen = 28.dp.toPx()
        val strokePx = 4.dp.toPx()
        val right = left + boxSide
        val bottom = top + boxSide

        // Top-Left
        drawLine(cornerColor, Offset(left, top + bracketLen), Offset(left, top), strokePx, StrokeCap.Round)
        drawLine(cornerColor, Offset(left, top), Offset(left + bracketLen, top), strokePx, StrokeCap.Round)
        // Top-Right
        drawLine(cornerColor, Offset(right - bracketLen, top), Offset(right, top), strokePx, StrokeCap.Round)
        drawLine(cornerColor, Offset(right, top), Offset(right, top + bracketLen), strokePx, StrokeCap.Round)
        // Bottom-Left
        drawLine(cornerColor, Offset(left, bottom - bracketLen), Offset(left, bottom), strokePx, StrokeCap.Round)
        drawLine(cornerColor, Offset(left, bottom), Offset(left + bracketLen, bottom), strokePx, StrokeCap.Round)
        // Bottom-Right
        drawLine(cornerColor, Offset(right - bracketLen, bottom), Offset(right, bottom), strokePx, StrokeCap.Round)
        drawLine(cornerColor, Offset(right, bottom - bracketLen), Offset(right, bottom), strokePx, StrokeCap.Round)

        // Animated turquoise scan laser while actively searching
        if (!isLockedOn) {
            val laserY = top + boxSide * scanProgress
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, laserColor, LgColors.GoldBright, laserColor, Color.Transparent),
                    startX = left + 10.dp.toPx(),
                    endX = right - 10.dp.toPx()
                ),
                start = Offset(left + 10.dp.toPx(), laserY),
                end = Offset(right - 10.dp.toPx(), laserY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun DetectedCredentialConfirmationCard(
    credentials: ParsedExchangeQrCredentials,
    onChangeExchange: (QrExchangeTarget) -> Unit,
    onSaveAndVerify: () -> Unit,
    onFillOnly: () -> Unit,
    onRescan: () -> Unit
) {
    GlassPanelCard(
        highlightBorder = LgColors.Turquoise,
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .testTag("qr_detected_confirmation_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = LgColors.Turquoise,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "${credentials.exchangeDisplayName.uppercase()} CREDENTIALS DETECTED",
                            style = TabLabelTextStyle,
                            color = LgColors.GoldBright
                        )
                        Text(
                            text = credentials.formatLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = LgColors.Turquoise
                        )
                    }
                }
                StatusPill(
                    text = if (credentials.isCompleteForExchange) "READY" else "PARTIAL",
                    color = if (credentials.isCompleteForExchange) LgColors.Turquoise else LgColors.Gold
                )
            }

            // Masked preview summary (never displays plaintext secrets)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(LgColors.ObsidianBg.copy(alpha = 0.84f))
                    .border(1.dp, LgColors.Gold.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (credentials.exchangeId == "lg_server") {
                    Text(
                        text = "Bearer Token: ${EncryptedExchangeCredentialStore.maskSecret(credentials.lgServerToken)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LgColors.InkPrimary
                    )
                } else {
                    Text(
                        text = "API Key: ${EncryptedExchangeCredentialStore.maskSecret(credentials.apiKey)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LgColors.InkPrimary
                    )
                    Text(
                        text = "API Secret: ${if (credentials.apiSecret.isNotBlank()) "•••••••• (${credentials.apiSecret.length} chars)" else "Not included in QR"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (credentials.apiSecret.isNotBlank()) LgColors.Turquoise else LgColors.Gold
                    )
                    if (credentials.exchangeId == "kucoin") {
                        Text(
                            text = "Passphrase: ${if (credentials.passphrase.isNotBlank()) "•••••••• (captured)" else "Required — enter manually after fill"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (credentials.passphrase.isNotBlank()) LgColors.Turquoise else LgColors.Gold
                        )
                    }
                }
            }

            // Quick venue reassignment row in case generic JSON QR needs a different exchange
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Assign to:",
                    style = MaterialTheme.typography.labelSmall,
                    color = LgColors.InkSecondary
                )
                listOf(
                    QrExchangeTarget.KRAKEN,
                    QrExchangeTarget.COINBASE,
                    QrExchangeTarget.KUCOIN,
                    QrExchangeTarget.BITSTAMP,
                    QrExchangeTarget.LG_SERVER
                ).forEach { target ->
                    val active = credentials.exchangeId == target.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (active) LgColors.GoldBright else LgColors.ObsidianBg.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = 1.dp,
                                color = if (active) LgColors.GoldBright else LgColors.BorderHairline,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onChangeExchange(target) }
                            .testTag("qr_assign_${target.id}")
                    ) {
                        Text(
                            text = target.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (active) LgColors.ObsidianBg else LgColors.InkSecondary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Primary actions
            if (credentials.isCompleteForExchange) {
                GoldMetallicButton(
                    text = stringResource(R.string.qr_scanner_save_and_verify),
                    onClick = onSaveAndVerify,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qr_confirm_save_and_verify_button")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onFillOnly,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LgColors.Turquoise,
                        contentColor = LgColors.ObsidianBg
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("qr_confirm_fill_only_button")
                ) {
                    Text(
                        text = stringResource(R.string.qr_scanner_fill_only),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onRescan,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("qr_confirm_rescan_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = LgColors.Gold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = stringResource(R.string.qr_scanner_rescan),
                        color = LgColors.Gold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionCard(
    permissionRequestedOnce: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwitchToManualImport: () -> Unit
) {
    GlassPanelCard(
        highlightBorder = LgColors.Gold.copy(alpha = 0.8f),
        modifier = Modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = LgColors.Turquoise,
                modifier = Modifier.size(34.dp)
            )
            Text(
                text = stringResource(R.string.qr_scanner_permission_title),
                style = MaterialTheme.typography.titleMedium,
                color = LgColors.GoldBright,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.qr_scanner_permission_body),
                style = MaterialTheme.typography.bodySmall,
                color = LgColors.InkSecondary,
                textAlign = TextAlign.Center
            )
            GoldMetallicButton(
                text = stringResource(R.string.qr_scanner_grant_permission),
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_grant_camera_permission_button")
            )
            if (permissionRequestedOnce) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("qr_open_app_settings_button")
                ) {
                    Text(
                        text = stringResource(R.string.qr_scanner_open_settings),
                        color = LgColors.Gold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            TextButton(
                onClick = onSwitchToManualImport,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("qr_permission_manual_import_button")
            ) {
                Text(
                    text = "Paste JSON / Portal URI instead",
                    color = LgColors.Turquoise,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ManualPayloadImportCard(
    payload: String,
    onPayloadChange: (String) -> Unit,
    errorMessage: String?,
    onParse: () -> Unit,
    onReturnToCamera: () -> Unit
) {
    GlassPanelCard(
        highlightBorder = LgColors.Turquoise.copy(alpha = 0.85f),
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "IMPORT PORTAL JSON OR QR URI",
                style = TabLabelTextStyle,
                color = LgColors.GoldBright
            )
            Text(
                text = "Paste a Coinbase CDP JSON key export, Kraken API URI (kraken://apikey?key=...&secret=...), or KuCoin credential JSON.",
                style = MaterialTheme.typography.bodySmall,
                color = LgColors.InkSecondary
            )
            OutlinedTextField(
                value = payload,
                onValueChange = onPayloadChange,
                label = { Text("Exchange JSON or QR URI") },
                minLines = 3,
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LgColors.Turquoise,
                    unfocusedBorderColor = LgColors.Gold.copy(alpha = 0.55f),
                    focusedContainerColor = LgColors.ObsidianBg.copy(alpha = 0.72f),
                    unfocusedContainerColor = LgColors.ObsidianBg.copy(alpha = 0.72f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_manual_payload_input")
            )
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = LgColors.Crimson
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GoldMetallicButton(
                    text = "PARSE CREDENTIALS",
                    onClick = onParse,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("qr_manual_parse_button")
                )
                OutlinedButton(
                    onClick = onReturnToCamera,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("qr_return_to_camera_button")
                ) {
                    Text("Camera", color = LgColors.Turquoise, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
