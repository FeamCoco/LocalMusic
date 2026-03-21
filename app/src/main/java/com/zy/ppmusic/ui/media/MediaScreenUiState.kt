package com.zy.ppmusic.ui.media

import android.graphics.Bitmap
import android.support.v4.media.session.PlaybackStateCompat

const val MEDIA_DEFAULT_THEME_COLOR = 0xFF5167D8.toInt()
const val MEDIA_DEFAULT_TIME_TEXT = "00:00"
const val MEDIA_DEFAULT_COUNTDOWN_MINUTES = 15

data class MediaPlaybackHeaderUiState(
    val title: String = "",
    val subtitle: String = "",
    val playingTime: String = MEDIA_DEFAULT_TIME_TEXT,
    val durationTime: String = MEDIA_DEFAULT_TIME_TEXT,
    val progress: Float = 0f,
    val repeatMode: Int = PlaybackStateCompat.REPEAT_MODE_NONE,
    val isPlaying: Boolean = false,
    val currentQueueIndex: Int = -1,
)

data class MediaArtworkPagerItemUiState(
    val mediaId: String,
    val artwork: Bitmap? = null,
)

data class MediaQueuePreviewItemUiState(
    val queueId: Long,
    val mediaId: String,
    val title: String,
    val subtitle: String,
    val mediaUri: String? = null,
)

data class MediaDeleteDialogUiState(
    val position: Int,
    val title: String,
    val subtitle: String,
    val includeFile: Boolean = true,
)

data class MediaQueueDetailUiState(
    val title: String,
    val message: String,
)

data class MediaCountdownUiState(
    val text: String? = null,
    val isSheetVisible: Boolean = false,
    val customTimeMinutes: Int = MEDIA_DEFAULT_COUNTDOWN_MINUTES,
)

data class MediaScreenUiState(
    val header: MediaPlaybackHeaderUiState = MediaPlaybackHeaderUiState(),
    val artworkPagerItems: List<MediaArtworkPagerItemUiState> = emptyList(),
    val queuePreviewItems: List<MediaQueuePreviewItemUiState> = emptyList(),
    val isLibraryLoading: Boolean = true,
    val deleteDialogState: MediaDeleteDialogUiState? = null,
    val queueDetailState: MediaQueueDetailUiState? = null,
    val countdownState: MediaCountdownUiState = MediaCountdownUiState(),
    val themeColor: Int = MEDIA_DEFAULT_THEME_COLOR,
)
