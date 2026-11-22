package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.LgCircuitBackground
import com.example.ui.components.StatusPill
import com.example.ui.theme.AppColorTheme
import com.example.ui.theme.LgBrandFontFamily
import com.example.ui.theme.LgColors
import com.example.ui.theme.TabLabelTextStyle
import com.example.ui.theme.TradingViewFontFamily

/**
 * Interactive 8-Theme Live Preview Gallery:
 * Renders all 8 distinct color schemes side-by-side with the real unchanged LG Digital images
 * (LG Coin crest, mini Fear & Greed arc, mini price rows, mini metallic CTA) so the user can
 * visually compare all 8 palettes and apply any one to the entire app with a single tap.
 */
@Composable
fun ThemePreviewGalleryScreen(
    activeTheme: AppColorTheme,
    onSelectTheme: (AppColorTheme) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val themePairs = AppColorTheme.entries.chunked(2)

    Surface(
        color = LgColors.ObsidianBg,
        modifier = Modifier
            .fillMaxSize()
            .testTag("theme_preview_gallery_screen")
    ) {
        LgCircuitBackground(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 860.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.testTag("theme_gallery_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = LgColors.GoldBright
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Palette,
                                    contentDescription = null,
                                    tint = LgColors.Turquoise,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "8 COLOR SCHEME PREVIEWS",
                                        fontFamily = TradingViewFontFamily,
                                        fontSize = 15.sp,
                                        color = LgColors.GoldBright,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "All images stay identical · Tap any preview to apply live",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LgColors.InkSecondary
                                    )
                                }
                            }
                            StatusPill(
                                text = "#${activeTheme.number} ACTIVE",
                                color = LgColors.Turquoise
                            )
                        }
                    }

                    items(themePairs.size) { rowIdx ->
                        val pair = themePairs[rowIdx]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { theme ->
                                ThemePreviewMiniCard(
                                    theme = theme,
                                    isSelected = theme == activeTheme,
                                    onSelect = { onSelectTheme(theme) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    item {
                        GoldMetallicButton(
                            text = "KEEP #${activeTheme.number} · ${activeTheme.displayName.uppercase()}",
                            onClick = onClose,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("theme_gallery_confirm_button")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewMiniCard(
    theme: AppColorTheme,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = theme.palette
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(p.tealAmbient, p.obsidianBg, p.panelSlate)
                )
            )
            .border(
                width = if (isSelected) 2.2.dp else 1.1.dp,
                color = if (isSelected) p.goldBright else p.borderHairline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
            .padding(10.dp)
            .testTag("theme_preview_card_${theme.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Preview Header with number badge & active check
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(p.turquoise.copy(alpha = 0.2f))
                    .border(1.dp, p.turquoise.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "OPTION ${theme.number}",
                    fontFamily = TradingViewFontFamily,
                    fontSize = 9.sp,
                    color = p.turquoise
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Active theme",
                    tint = p.goldBright,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Miniature Live App Mockup (Real Unchanged LG Coin + Mini Dashboard + Mini Gauge + Mini Sparkline)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(p.panelSlate.copy(alpha = 0.94f))
                .border(1.dp, p.gold.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // Mini Top Bar with REAL LG Coin image
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_user_lg_coin),
                        contentDescription = "LG Coin unchanged preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(1.dp, p.goldBright, CircleShape)
                    )
                    Column {
                        Text(
                            text = "LG DIGITAL",
                            fontFamily = LgBrandFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.5.sp,
                            letterSpacing = 1.2.sp,
                            color = p.goldBright,
                            maxLines = 1
                        )
                        Text(
                            text = "$104,820",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.5.sp,
                            color = p.turquoise
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(p.elevatedSlate)
                        .border(0.8.dp, p.turquoise.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("PRO", fontFamily = TradingViewFontFamily, fontSize = 8.sp, color = p.goldBright)
                }
            }

            // Mini Live Chart + Fear & Greed Arc visual
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(p.obsidianBg.copy(alpha = 0.85f))
                    .border(0.8.dp, p.borderHairline.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Canvas(modifier = Modifier.size(34.dp, 26.dp)) {
                    val d = minOf(size.width, size.height * 1.7f)
                    val left = (size.width - d) / 2f
                    val top = size.height - d / 2f
                    drawArc(
                        color = p.borderHairline,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(left, top),
                        size = Size(d, d),
                        style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = p.turquoise,
                        startAngle = 180f,
                        sweepAngle = 128f,
                        useCenter = false,
                        topLeft = Offset(left, top),
                        size = Size(d, d),
                        style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                    )
                }
                Canvas(modifier = Modifier.weight(1f).height(26.dp)) {
                    val pts = listOf(0.75f, 0.62f, 0.70f, 0.48f, 0.54f, 0.30f, 0.36f, 0.16f)
                    val path = Path().apply {
                        pts.forEachIndexed { i, yFrac ->
                            val x = size.width * i / (pts.size - 1)
                            val y = size.height * yFrac
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }
                    drawPath(path, color = p.turquoise, style = Stroke(width = 2.4f, cap = StrokeCap.Round))
                    drawCircle(color = p.goldBright, radius = 3.2f, center = Offset(size.width, size.height * pts.last()))
                }
            }

            // Mini Walkthrough / Trade Box with real unchanged walkthrough bot thumbnail
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(p.elevatedSlate.copy(alpha = 0.85f))
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_walkthrough_bot),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(5.dp))
                )
                Text(
                    text = "BTC +3.8%",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = p.inkPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Text(
                    text = "BUY",
                    fontFamily = TradingViewFontFamily,
                    fontSize = 8.5.sp,
                    color = p.turquoise
                )
            }

            // Mini Accent CTA Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(listOf(p.goldDark, p.goldBright, p.gold))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "EXECUTE",
                    fontFamily = TradingViewFontFamily,
                    fontSize = 8.sp,
                    color = p.obsidianBg
                )
            }
        }

        // Theme Title & Swatch Dots
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = theme.displayName,
                fontFamily = TradingViewFontFamily,
                fontSize = 11.5.sp,
                color = p.inkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = theme.tagline,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.5.sp,
                color = p.inkSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(p.obsidianBg, p.panelSlate, p.turquoise, p.gold, p.goldBright).forEach { swatch ->
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(0.8.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                    )
                }
            }
        }

        Button(
            onClick = onSelect,
            shape = RoundedCornerShape(9.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) p.goldBright else p.turquoise,
                contentColor = p.obsidianBg
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 38.dp)
        ) {
            Text(
                text = if (isSelected) "APPLIED" else "USE THEME #${theme.number}",
                style = TabLabelTextStyle,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = p.obsidianBg
            )
        }
    }
}
