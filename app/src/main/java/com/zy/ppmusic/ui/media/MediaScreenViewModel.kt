package com.zy.ppmusic.ui.media

import android.support.v4.media.session.MediaSessionCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MediaScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MediaScreenUiState())
    val uiState: StateFlow<MediaScreenUiState> = _uiState.asStateFlow()

    fun setLibraryLoading(isLoading: Boolean) {
        updateState { state ->
            if (state.isLibraryLoading == isLoading) state else state.copy(isLibraryLoading = isLoading)
        }
    }

    fun updateThemeColor(color: Int) {
        updateState { it.copy(themeColor = color) }
    }

    fun updateHeader(transform: (MediaPlaybackHeaderUiState) -> MediaPlaybackHeaderUiState) {
        updateState { state -> state.copy(header = transform(state.header)) }
    }

    fun updateCountdown(transform: (MediaCountdownUiState) -> MediaCountdownUiState) {
        updateState { state -> state.copy(countdownState = transform(state.countdownState)) }
    }

    fun setQueueSnapshot(queueItems: List<MediaSessionCompat.QueueItem>, currentMediaId: String? = null) {
        updateState { state ->
            val resolvedIndex = resolveCurrentIndex(queueItems, currentMediaId, state.header.currentQueueIndex)
            state.copy(
                header = state.header.copy(currentQueueIndex = resolvedIndex),
                artworkPagerItems = queueItems.map { it.toArtworkPagerItem() },
                queuePreviewItems = queueItems.map { it.toQueuePreviewItem() },
                isLibraryLoading = false,
            )
        }
    }

    fun setCurrentQueueIndex(index: Int) {
        updateState { state ->
            state.copy(
                header = state.header.copy(
                    currentQueueIndex = if (index in state.queuePreviewItems.indices) index else -1,
                ),
            )
        }
    }

    fun showDeleteDialog(position: Int, includeFile: Boolean = true) {
        updateState { state ->
            val queueItem = state.queuePreviewItems.getOrNull(position)
            state.copy(
                deleteDialogState = MediaDeleteDialogUiState(
                    position = position,
                    title = queueItem?.title.orEmpty(),
                    subtitle = queueItem?.subtitle.orEmpty(),
                    includeFile = includeFile,
                ),
            )
        }
    }

    fun hideDeleteDialog() {
        updateState { it.copy(deleteDialogState = null) }
    }

    fun updateDeleteIncludeFile(includeFile: Boolean) {
        updateState { state ->
            val dialogState = state.deleteDialogState ?: return@updateState state
            state.copy(deleteDialogState = dialogState.copy(includeFile = includeFile))
        }
    }

    fun showQueueDetail(title: String, message: String) {
        updateState { it.copy(queueDetailState = MediaQueueDetailUiState(title = title, message = message)) }
    }

    fun hideQueueDetail() {
        updateState { it.copy(queueDetailState = null) }
    }

    fun showCountdownSheet() {
        updateState { state -> state.copy(countdownState = state.countdownState.copy(isSheetVisible = true)) }
    }

    fun hideCountdownSheet() {
        updateState { state -> state.copy(countdownState = state.countdownState.copy(isSheetVisible = false)) }
    }

    fun updateCountdownText(text: String?) {
        updateState { state -> state.copy(countdownState = state.countdownState.copy(text = text)) }
    }

    fun updateCustomTimeMinutes(minutes: Int) {
        updateState { state ->
            state.copy(countdownState = state.countdownState.copy(customTimeMinutes = minutes.coerceIn(1, 120)))
        }
    }

    fun setEmptyPlaybackState(title: String, subtitle: String, timeLabel: String) {
        updateState {
            it.copy(
                header = it.header.copy(
                    title = title,
                    subtitle = subtitle,
                    playingTime = timeLabel,
                    durationTime = timeLabel,
                    progress = 0f,
                    currentQueueIndex = -1,
                    isPlaying = false,
                ),
                artworkPagerItems = emptyList(),
                queuePreviewItems = emptyList(),
                isLibraryLoading = false,
                deleteDialogState = null,
                queueDetailState = null,
                countdownState = it.countdownState.copy(text = null, isSheetVisible = false),
            )
        }
    }

    private inline fun updateState(transform: (MediaScreenUiState) -> MediaScreenUiState) {
        _uiState.update(transform)
    }

    private fun resolveCurrentIndex(
        queueItems: List<MediaSessionCompat.QueueItem>,
        currentMediaId: String?,
        fallbackIndex: Int,
    ): Int {
        val matchedIndex = currentMediaId?.let { mediaId ->
            queueItems.indexOfFirst { it.description.mediaId == mediaId }
        } ?: -1
        return when {
            matchedIndex >= 0 -> matchedIndex
            fallbackIndex in queueItems.indices -> fallbackIndex
            else -> -1
        }
    }
}

private fun MediaSessionCompat.QueueItem.toQueuePreviewItem(): MediaQueuePreviewItemUiState {
    return MediaQueuePreviewItemUiState(
        queueId = queueId,
        mediaId = description.mediaId.orEmpty(),
        title = description.title?.toString().orEmpty(),
        subtitle = description.subtitle?.toString().orEmpty(),
        mediaUri = description.mediaUri?.toString(),
    )
}

private fun MediaSessionCompat.QueueItem.toArtworkPagerItem(): MediaArtworkPagerItemUiState {
    return MediaArtworkPagerItemUiState(
        mediaId = description.mediaId.orEmpty(),
        artwork = description.iconBitmap,
    )
}
