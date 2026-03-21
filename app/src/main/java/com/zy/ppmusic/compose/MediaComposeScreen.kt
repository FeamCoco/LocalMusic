package com.zy.ppmusic.compose

import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zy.ppmusic.R
import com.zy.ppmusic.compose.theme.LocalMusicComposeTheme
import com.zy.ppmusic.ui.media.MediaArtworkPagerItemUiState
import com.zy.ppmusic.ui.media.MediaCountdownUiState
import com.zy.ppmusic.ui.media.MediaDeleteDialogUiState
import com.zy.ppmusic.ui.media.MediaPlaybackHeaderUiState
import com.zy.ppmusic.ui.media.MediaQueuePreviewItemUiState
import com.zy.ppmusic.ui.media.MediaScreenUiState
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

enum class MediaMenuAction { Scan, Bluetooth, NotifyStyle, CountDown }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaComposeScreen(
    uiState: MediaScreenUiState,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleRepeatMode: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onMenuAction: (MediaMenuAction) -> Unit,
    onSelectQueueItem: (Int) -> Unit,
    onShowQueueItemDetail: (Int) -> Unit,
    onDeleteQueueItem: (Int) -> Unit,
    onPagerSettled: (Int) -> Unit,
    onDeleteDialogDismiss: () -> Unit,
    onDeleteIncludeFileChanged: (Boolean) -> Unit,
    onDeleteConfirm: () -> Unit,
    onQueueDetailDismiss: () -> Unit,
    onTimeSheetDismiss: () -> Unit,
    onCustomTimeMinutesChange: (Int) -> Unit,
    onTimeOptionSelected: (Int) -> Unit,
    onCustomTimeConfirm: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val accent = remember(uiState.themeColor, colors.primary) { lerp(colors.primary, Color(uiState.themeColor), 0.06f) }
    val showLibraryLoadingStage = uiState.isLibraryLoading && uiState.queuePreviewItems.isEmpty()
    val background = remember(colors.background, colors.surface, colors.primaryContainer) {
        Brush.verticalGradient(
            listOf(
                colors.background,
                colors.primaryContainer.copy(alpha = 0.08f),
                colors.background,
            ),
        )
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var queueVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu),
                            contentDescription = stringResource(R.string.player_more_actions),
                        )
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.scan_media)) },
                            onClick = {
                                menuExpanded = false
                                onMenuAction(MediaMenuAction.Scan)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.choose_notify_style)) },
                            onClick = {
                                menuExpanded = false
                                onMenuAction(MediaMenuAction.NotifyStyle)
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                PlayerStageCard(
                    uiState = uiState,
                    accent = accent,
                    isLibraryLoading = showLibraryLoadingStage,
                    onSeekChange = onSeekChange,
                    onSeekFinished = onSeekFinished,
                    onSkipPrevious = onSkipPrevious,
                    onToggleRepeatMode = onToggleRepeatMode,
                    onTogglePlayPause = onTogglePlayPause,
                    onSkipNext = onSkipNext,
                    onOpenQueue = { queueVisible = true },
                    onOpenBluetooth = { onMenuAction(MediaMenuAction.Bluetooth) },
                    onOpenTimer = { onMenuAction(MediaMenuAction.CountDown) },
                    onOpenScan = { onMenuAction(MediaMenuAction.Scan) },
                    onPagerSettled = onPagerSettled,
                )
            }
            item {
                if (!showLibraryLoadingStage && uiState.queuePreviewItems.isEmpty()) {
                    EmptyLibraryCard(accent = accent, onScanClick = { onMenuAction(MediaMenuAction.Scan) }, onBluetoothClick = { onMenuAction(MediaMenuAction.Bluetooth) })
                } else if (uiState.queuePreviewItems.isNotEmpty()) {
                    QueuePeekStrip(
                        items = uiState.queuePreviewItems,
                        selectedIndex = uiState.header.currentQueueIndex,
                        accent = accent,
                        onOpenQueue = { queueVisible = true },
                    )
                }
            }
        }
    }

    if (queueVisible) {
        QueueBottomSheet(
            items = uiState.queuePreviewItems,
            selectedIndex = uiState.header.currentQueueIndex,
            accent = accent,
            onDismiss = { queueVisible = false },
            onSelect = {
                onSelectQueueItem(it)
                queueVisible = false
            },
            onDetail = onShowQueueItemDetail,
            onDelete = onDeleteQueueItem,
        )
    }

    uiState.deleteDialogState?.let { state ->
        AlertDialog(
            onDismissRequest = onDeleteDialogDismiss,
            title = { Text(stringResource(R.string.player_delete_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (state.title.isBlank()) {
                            stringResource(R.string.player_delete_dialog_body_fallback)
                        } else {
                            stringResource(R.string.player_delete_dialog_body, state.title, state.subtitle.ifBlank { stringResource(R.string.unknown_author) })
                        },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = state.includeFile, onCheckedChange = onDeleteIncludeFileChanged)
                        Text(text = stringResource(R.string.player_delete_dialog_checkbox))
                    }
                }
            },
            confirmButton = { Button(onClick = onDeleteConfirm) { Text(stringResource(R.string.string_del)) } },
            dismissButton = { OutlinedButton(onClick = onDeleteDialogDismiss) { Text(stringResource(R.string.string_cancel)) } },
        )
    }

    uiState.queueDetailState?.let {
        AlertDialog(
            onDismissRequest = onQueueDetailDismiss,
            title = { Text(it.title) },
            text = { Text(it.message) },
            confirmButton = { Button(onClick = onQueueDetailDismiss) { Text(stringResource(R.string.string_sure)) } },
        )
    }

    if (uiState.countdownState.isSheetVisible) {
        CountdownBottomSheet(
            countdownState = uiState.countdownState,
            accent = accent,
            onDismiss = onTimeSheetDismiss,
            onCustomTimeMinutesChange = onCustomTimeMinutesChange,
            onTimeOptionSelected = onTimeOptionSelected,
            onCustomTimeConfirm = onCustomTimeConfirm,
        )
    }
}

@Composable
private fun PlayerStageCard(
    uiState: MediaScreenUiState,
    accent: Color,
    isLibraryLoading: Boolean,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleRepeatMode: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenScan: () -> Unit,
    onPagerSettled: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val controlsEnabled = uiState.queuePreviewItems.isNotEmpty()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surface,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = stringResource(R.string.player_now_playing_label), color = accent, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (isLibraryLoading) {
                            stringResource(R.string.player_loading_status)
                        } else if (uiState.queuePreviewItems.isEmpty()) {
                            stringResource(R.string.player_waiting_for_library)
                        } else {
                            playerStateLabel(uiState)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                PlaybackModeButton(
                    repeatMode = uiState.header.repeatMode,
                    onClick = onToggleRepeatMode,
                    enabled = !isLibraryLoading && controlsEnabled,
                )
            }
            AlbumPagerCard(
                artworkItems = uiState.artworkPagerItems,
                currentIndex = uiState.header.currentQueueIndex,
                accent = accent,
                isLibraryLoading = isLibraryLoading,
                onPagerSettled = onPagerSettled,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = when {
                        isLibraryLoading -> stringResource(R.string.player_loading_title)
                        uiState.header.title.isBlank() -> stringResource(R.string.player_empty_title)
                        else -> uiState.header.title
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when {
                            isLibraryLoading -> stringResource(R.string.player_loading_body)
                            uiState.header.subtitle.isBlank() -> stringResource(R.string.player_empty_subtitle)
                            else -> uiState.header.subtitle
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    currentTrackPosition(uiState)?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Slider(
                value = uiState.header.progress,
                onValueChange = onSeekChange,
                onValueChangeFinished = onSeekFinished,
                enabled = controlsEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = colors.surfaceVariant,
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = uiState.header.playingTime, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                Text(text = uiState.header.durationTime, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransportButton(R.drawable.ic_previous, stringResource(R.string.player_previous_track), colors.primaryContainer, colors.onPrimaryContainer, onSkipPrevious, enabled = controlsEnabled)
                TransportButton(
                    if (uiState.header.isPlaying) R.drawable.ic_black_pause else R.drawable.ic_black_play,
                    stringResource(R.string.action_start_pause),
                    accent,
                    Color.White,
                    onTogglePlayPause,
                    enabled = controlsEnabled,
                    size = 88.dp,
                )
                TransportButton(R.drawable.ic_next, stringResource(R.string.player_next_track), colors.primaryContainer, colors.onPrimaryContainer, onSkipNext, enabled = controlsEnabled)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ConsoleActionButton(
                    iconRes = R.drawable.ic_menu,
                    label = stringResource(R.string.player_queue_button_label),
                    badge = uiState.queuePreviewItems.size.takeIf { it > 0 }?.toString(),
                    accent = accent,
                    enabled = controlsEnabled,
                    onClick = onOpenQueue,
                )
                ConsoleActionButton(
                    iconRes = R.mipmap.ic_device,
                    label = stringResource(R.string.player_empty_library_secondary_action),
                    accent = accent,
                    onClick = onOpenBluetooth,
                )
                ConsoleActionButton(
                    iconRes = R.drawable.ic_menu_time_to_close,
                    label = stringResource(R.string.player_timer_sheet_title),
                    badge = uiState.countdownState.text,
                    accent = accent,
                    enabled = controlsEnabled || uiState.countdownState.text != null,
                    onClick = onOpenTimer,
                )
                ConsoleActionButton(
                    iconRes = R.drawable.ic_menu_search_media,
                    label = stringResource(R.string.player_empty_library_primary_action),
                    accent = accent,
                    onClick = onOpenScan,
                )
            }
        }
    }
}

@Composable
private fun PlaybackModeButton(
    repeatMode: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val iconRes = when (repeatMode) {
        PlaybackStateCompat.REPEAT_MODE_ONE -> R.drawable.ic_loop_mode_only_svg
        PlaybackStateCompat.REPEAT_MODE_ALL -> R.drawable.ic_loop_mode_list_svg
        else -> R.drawable.ic_loop_mode_normal_svg
    }
    Surface(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f }
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = colors.surfaceVariant.copy(alpha = 0.42f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(R.string.string_loop_mode),
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun AlbumPagerCard(
    artworkItems: List<MediaArtworkPagerItemUiState>,
    currentIndex: Int,
    accent: Color,
    isLibraryLoading: Boolean,
    onPagerSettled: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val hasArtwork = artworkItems.any { it.artwork != null }
    val showPagerTransformer = hasArtwork && artworkItems.size > 1
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.primaryContainer.copy(alpha = if (hasArtwork) 0.08f else 0.14f),
                        colors.surfaceVariant.copy(alpha = if (hasArtwork) 0.1f else 0.18f),
                    ),
                ),
            ),
    ) {
        if (artworkItems.isEmpty()) {
            ArtworkPlaceholder(
                accent = accent,
                isLoading = isLibraryLoading,
                modifier = Modifier.fillMaxSize(),
            )
            return@Box
        }
        val pagerState = rememberPagerState(initialPage = currentIndex.takeIf { it in artworkItems.indices } ?: 0, pageCount = { artworkItems.size })
        LaunchedEffect(currentIndex, artworkItems.size) {
            if (currentIndex in artworkItems.indices && pagerState.currentPage != currentIndex) pagerState.scrollToPage(currentIndex)
        }
        LaunchedEffect(pagerState, artworkItems.size) {
            var skipInitialEmission = true
            snapshotFlow { pagerState.settledPage }
                .filter { it in artworkItems.indices }
                .distinctUntilChanged()
                .collect { page ->
                    if (skipInitialEmission) {
                        skipInitialEmission = false
                    } else {
                        onPagerSettled(page)
                    }
                }
        }
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = if (showPagerTransformer) 1 else 0,
            contentPadding = PaddingValues(horizontal = if (showPagerTransformer) 18.dp else 0.dp),
            pageSpacing = 0.dp,
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val clampedOffset = pageOffset.coerceIn(0f, 1f)
            val scale = if (showPagerTransformer) 0.95f + ((1f - clampedOffset) * 0.05f) else 1f
            val alpha = if (showPagerTransformer) 0.82f + ((1f - clampedOffset) * 0.18f) else 1f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (showPagerTransformer) 6.dp else 0.dp,
                        vertical = if (showPagerTransformer) 10.dp else 0.dp,
                    )
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        translationX = if (showPagerTransformer) {
                            if (page < pagerState.currentPage) {
                                -size.width * 0.02f * clampedOffset
                            } else {
                                size.width * 0.02f * clampedOffset
                            }
                        } else {
                            0f
                        }
                    },
            ) {
                artworkItems[page].artwork?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = stringResource(R.string.artist_headset),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop,
                    )
                } ?: ArtworkPlaceholder(
                    accent = accent,
                    isLoading = isLibraryLoading,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ArtworkPlaceholder(
    accent: Color,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "artwork_placeholder")
    val swayDegrees by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isLoading) 2600 else 3800,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "artwork_placeholder_sway",
    )
    val markScale by transition.animateFloat(
        initialValue = 0.99f,
        targetValue = 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isLoading) 2200 else 3200,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "artwork_placeholder_scale",
    )
    val glowTravel by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isLoading) 2100 else 2800,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "artwork_placeholder_glow",
    )
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.surface.copy(alpha = 0.66f),
                        colors.primaryContainer.copy(alpha = 0.16f),
                        colors.surfaceVariant.copy(alpha = 0.22f),
                    ),
                ),
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frame = size.minDimension
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = accent.copy(alpha = 0.08f),
                radius = frame * 0.3f,
                center = center,
            )
            drawCircle(
                color = colors.tertiary.copy(alpha = 0.10f),
                radius = frame * 0.1f,
                center = Offset(size.width * 0.68f, size.height * 0.34f),
            )
        }

        LocalMusicGlyph(
            accent = accent,
            glowTravel = glowTravel,
            glow = colors.tertiary,
            modifier = Modifier
                .size(196.dp)
                .graphicsLayer(
                    scaleX = markScale,
                    scaleY = markScale,
                    rotationZ = swayDegrees,
                ),
        )
    }
}

@Composable
private fun LocalMusicGlyph(
    accent: Color,
    glowTravel: Float,
    glow: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val frame = size.minDimension
        val center = Offset(frame / 2f, frame / 2f)
        val strokeWidth = frame * 0.084f
        val rightLoopCenter = Offset(frame * 0.66f, center.y)
        val dotAngle = -52f + (glowTravel * 116f)
        val dotRadians = Math.toRadians(dotAngle.toDouble())
        val dotCenter = Offset(
            x = rightLoopCenter.x + (cos(dotRadians) * frame * 0.14f).toFloat(),
            y = rightLoopCenter.y + (sin(dotRadians) * frame * 0.18f).toFloat(),
        )

        drawCircle(
            color = accent.copy(alpha = 0.12f),
            radius = frame * 0.29f,
            center = center,
        )
        drawPath(
            path = Path().apply {
                moveTo(center.x, center.y)
                cubicTo(frame * 0.42f, frame * 0.31f, frame * 0.18f, frame * 0.34f, frame * 0.18f, center.y)
                cubicTo(frame * 0.18f, frame * 0.66f, frame * 0.42f, frame * 0.69f, center.x, center.y)
                cubicTo(frame * 0.58f, frame * 0.31f, frame * 0.82f, frame * 0.34f, frame * 0.82f, center.y)
                cubicTo(frame * 0.82f, frame * 0.66f, frame * 0.58f, frame * 0.69f, center.x, center.y)
            },
            color = accent,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        drawCircle(
            color = glow,
            radius = frame * 0.047f,
            center = dotCenter,
        )
    }
}

@Composable
private fun ConsoleActionButton(
    iconRes: Int,
    label: String,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
    badge: String? = null,
) {
    val contentAlpha = if (enabled) 1f else 0.42f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .size(54.dp)
                    .graphicsLayer { alpha = contentAlpha }
                    .clickable(enabled = enabled, onClick = onClick),
                shape = CircleShape,
                color = accent.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = label,
                        tint = accent,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            badge?.takeIf { it.isNotBlank() }?.let {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TransportButton(iconRes: Int, contentDescription: String, containerColor: Color, iconTint: Color, onClick: () -> Unit, enabled: Boolean = true, size: Dp = 64.dp) {
    Surface(
        modifier = Modifier
            .size(size)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f },
        shape = CircleShape,
        color = containerColor,
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(painter = painterResource(iconRes), contentDescription = contentDescription, tint = iconTint, modifier = Modifier.size(if (size > 70.dp) 34.dp else 28.dp))
        }
    }
}

@Composable
private fun EmptyLibraryCard(accent: Color, onScanClick: () -> Unit, onBluetoothClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(modifier = Modifier.size(84.dp), shape = CircleShape, color = accent.copy(alpha = 0.12f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(R.drawable.ic_menu_search_media), contentDescription = null, tint = accent, modifier = Modifier.size(40.dp))
                }
            }
            Text(text = stringResource(R.string.player_empty_library_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(text = stringResource(R.string.player_empty_library_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onScanClick, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = accent)) { Text(stringResource(R.string.player_empty_library_primary_action)) }
                OutlinedButton(onClick = onBluetoothClick, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.player_empty_library_secondary_action)) }
            }
        }
    }
}

@Composable
private fun QueuePeekStrip(items: List<MediaQueuePreviewItemUiState>, selectedIndex: Int, accent: Color, onOpenQueue: () -> Unit) {
    val nextItem = items.getOrNull(
        when {
            selectedIndex in items.indices && selectedIndex < items.lastIndex -> selectedIndex + 1
            selectedIndex !in items.indices && items.isNotEmpty() -> 0
            else -> -1
        },
    )
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenQueue),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.player_next_up_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = nextItem?.title ?: stringResource(R.string.player_next_up_none),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = nextItem?.subtitle ?: stringResource(R.string.player_queue_summary_ready),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AssistChip(
                onClick = onOpenQueue,
                label = { Text(stringResource(R.string.player_queue_tracks_count, items.size)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = accent.copy(alpha = 0.12f),
                    labelColor = accent,
                ),
            )
        }
    }
}

@Composable
private fun QueuePreviewRow(item: MediaQueuePreviewItemUiState, selected: Boolean, accent: Color) {
    Surface(shape = MaterialTheme.shapes.medium, color = if (selected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = if (selected) accent else MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(if (selected) R.drawable.ic_black_play else R.drawable.ic_music_play), contentDescription = null, tint = if (selected) Color.White else MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(text = item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueBottomSheet(
    items: List<MediaQueuePreviewItemUiState>,
    selectedIndex: Int,
    accent: Color,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onDetail: (Int) -> Unit,
    onDelete: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = stringResource(R.string.player_queue_sheet_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(text = stringResource(R.string.player_queue_sheet_body), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Text(text = stringResource(R.string.player_queue_tracks_count, items.size), color = accent, fontWeight = FontWeight.SemiBold)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                itemsIndexed(items, key = { _, item -> item.queueId }) { index, item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(index) },
                        shape = MaterialTheme.shapes.large,
                        color = if (index == selectedIndex) accent.copy(alpha = 0.10f) else colors.surfaceVariant.copy(alpha = 0.26f),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = if (index == selectedIndex) accent else colors.primaryContainer) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = (index + 1).toString(), fontWeight = FontWeight.Bold, color = if (index == selectedIndex) Color.White else colors.onPrimaryContainer)
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(text = item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = item.subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(onClick = { onDetail(index) }) { Text(text = stringResource(R.string.player_queue_item_detail_action)) }
                            IconButton(onClick = { onDelete(index) }) {
                                Icon(painter = painterResource(R.mipmap.ic_del_media), contentDescription = stringResource(R.string.string_del_media), tint = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountdownBottomSheet(
    countdownState: MediaCountdownUiState,
    accent: Color,
    onDismiss: () -> Unit,
    onCustomTimeMinutesChange: (Int) -> Unit,
    onTimeOptionSelected: (Int) -> Unit,
    onCustomTimeConfirm: () -> Unit,
) {
    val presetMinutes = listOf(15, 30, 45, 60, 75, 120)
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(text = stringResource(R.string.player_timer_sheet_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(text = stringResource(R.string.player_timer_sheet_body), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            countdownState.text?.let {
                Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = colors.secondaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.player_timer_current, it), color = colors.onSecondaryContainer)
                        TextButton(onClick = { onTimeOptionSelected(0) }) { Text(text = stringResource(R.string.player_timer_cancel)) }
                    }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(presetMinutes) { index, minute ->
                    FilledTonalButton(onClick = { onTimeOptionSelected(if (countdownState.text != null) index + 1 else index) }, shape = MaterialTheme.shapes.medium) {
                        Text(text = stringResource(R.string.player_timer_minutes, minute))
                    }
                }
            }
            Text(text = stringResource(R.string.string_custom_time_hint), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Slider(
                value = countdownState.customTimeMinutes.toFloat(),
                onValueChange = { onCustomTimeMinutesChange(it.roundToInt().coerceIn(1, 120)) },
                valueRange = 1f..120f,
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
            )
            Text(text = stringResource(R.string.player_timer_minutes, countdownState.customTimeMinutes), fontSize = 13.sp, color = colors.onSurfaceVariant)
            Button(onClick = onCustomTimeConfirm, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                Text(text = stringResource(R.string.string_sure))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun repeatModeLabel(mode: Int): String = when (mode) {
    PlaybackStateCompat.REPEAT_MODE_ONE -> "单曲循环"
    PlaybackStateCompat.REPEAT_MODE_ALL -> "列表循环"
    else -> "顺序播放"
}

@Composable
private fun playerStateLabel(uiState: MediaScreenUiState): String {
    val queueSize = uiState.queuePreviewItems.size
    val currentIndex = uiState.header.currentQueueIndex
    return when {
        queueSize == 0 -> stringResource(R.string.player_state_idle)
        uiState.header.isPlaying && currentIndex in uiState.queuePreviewItems.indices ->
            stringResource(R.string.player_state_position, currentIndex + 1, queueSize)
        uiState.header.isPlaying -> stringResource(R.string.player_state_playing)
        currentIndex in uiState.queuePreviewItems.indices ->
            stringResource(R.string.player_state_paused) + " · " + stringResource(R.string.player_state_position, currentIndex + 1, queueSize)
        else -> stringResource(R.string.player_state_paused)
    }
}

@Composable
private fun currentTrackPosition(uiState: MediaScreenUiState): String? {
    val queueSize = uiState.queuePreviewItems.size
    val currentIndex = uiState.header.currentQueueIndex
    return if (queueSize > 0 && currentIndex in uiState.queuePreviewItems.indices) {
        stringResource(R.string.player_state_position, currentIndex + 1, queueSize)
    } else {
        null
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F6F7)
@Composable
private fun MediaComposeScreenPreview() {
    LocalMusicComposeTheme(dynamicColor = false) {
        MediaComposeScreen(
            uiState = MediaScreenUiState(
                header = MediaPlaybackHeaderUiState(title = "Da Ya Think I'm Sexy", subtitle = "Rod Stewart", playingTime = "01:12", durationTime = "04:12", progress = 0.32f, isPlaying = true, currentQueueIndex = 1),
                artworkPagerItems = listOf(MediaArtworkPagerItemUiState("1"), MediaArtworkPagerItemUiState("2")),
                queuePreviewItems = listOf(
                    MediaQueuePreviewItemUiState(1L, "1", "Da Ya Think I'm Sexy", "Rod Stewart"),
                    MediaQueuePreviewItemUiState(2L, "2", "Year of the Cat", "Al Stewart"),
                ),
                countdownState = MediaCountdownUiState(text = "00:15"),
            ),
            onSeekChange = {},
            onSeekFinished = {},
            onSkipPrevious = {},
            onToggleRepeatMode = {},
            onTogglePlayPause = {},
            onSkipNext = {},
            onMenuAction = {},
            onSelectQueueItem = {},
            onShowQueueItemDetail = {},
            onDeleteQueueItem = {},
            onPagerSettled = {},
            onDeleteDialogDismiss = {},
            onDeleteIncludeFileChanged = {},
            onDeleteConfirm = {},
            onQueueDetailDismiss = {},
            onTimeSheetDismiss = {},
            onCustomTimeMinutesChange = {},
            onTimeOptionSelected = {},
            onCustomTimeConfirm = {},
        )
    }
}
