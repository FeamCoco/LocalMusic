package com.zy.ppmusic.mvp.view

import android.Manifest
import android.graphics.Bitmap
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.provider.DocumentsContract
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zy.ppmusic.R
import com.zy.ppmusic.compose.MediaComposeScreen
import com.zy.ppmusic.compose.MediaMenuAction
import com.zy.ppmusic.compose.theme.LocalMusicComposeTheme
import com.zy.ppmusic.mvp.base.AbstractBaseMvpActivity
import com.zy.ppmusic.mvp.contract.IMediaActivityContract
import com.zy.ppmusic.mvp.presenter.MediaPresenterImpl
import com.zy.ppmusic.service.MediaService
import com.zy.ppmusic.utils.DateUtil
import com.zy.ppmusic.utils.DataProvider
import com.zy.ppmusic.utils.PrintLog
import com.zy.ppmusic.utils.logd
import com.zy.ppmusic.utils.loge
import com.zy.ppmusic.utils.toast
import com.zy.ppmusic.widget.ChooseStyleDialog
import com.zy.ppmusic.widget.EasyTintView
import com.zy.ppmusic.widget.Loader
import com.zy.ppmusic.ui.media.MediaScreenViewModel
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.lang.ref.WeakReference
import java.util.Locale
import kotlin.system.exitProcess

@Keep
class MediaActivity : AbstractBaseMvpActivity<MediaPresenterImpl>(), IMediaActivityContract.IMediaActivityView,
    EasyPermissions.PermissionCallbacks {

    private var mediaBrowser: MediaBrowserCompat? = null
    private var mediaController: MediaControllerCompat? = null
    private var currentMediaId: String? = null
    private var endPosition = 0L
    private val requestDelPermissionCode = 10002
    private var resultReceive: MediaResultReceive? = null
    private var isTrackingBar = false
    private var modeIndex = PlaybackStateCompat.REPEAT_MODE_NONE
    private var isStarted = false
    private var loader: Loader? = null
    private var doDelActionPosition = -1
    private var doDelActionIncludeFile = true
    private var pendingSeekFraction = 0f
    private val countdownOptions = listOf(15, 30, 45, 60, 75, 120)
    private val mediaScreenViewModel by lazy { ViewModelProvider(this).get(MediaScreenViewModel::class.java) }

    @Keep
    class MediaResultReceive(activity: MediaActivity, handler: Handler) : ResultReceiver(handler) {
        private val weakView = WeakReference(activity)

        override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
            super.onReceiveResult(resultCode, resultData)
            val activity = weakView.get() ?: return
            when (resultCode) {
                MediaService.COMMAND_POSITION_CODE -> activity.updatePlaybackPosition(resultData.getInt(MediaService.EXTRA_POSITION).toLong())
                MediaService.COMMAND_UPDATE_QUEUE_CODE -> {
                    activity.syncMediaCollections(activity.mediaController?.queue)
                    activity.hideLoading()
                }
                else -> PrintLog.print("MediaResultReceive other result....$resultCode,$resultData")
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 100

        fun action(context: Context) {
            context.startActivity(Intent(context, MediaActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
    }

    override fun getContentViewId(): Int = R.layout.activity_compose_host

    override fun createPresenter(): MediaPresenterImpl = MediaPresenterImpl(this)

    override fun initViews() {
        updateThemeColor()
        setContent {
            LocalMusicComposeTheme(dynamicColor = false) {
                val uiState by mediaScreenViewModel.uiState.collectAsStateWithLifecycle()
                MediaComposeScreen(
                    uiState = uiState,
                    onSeekChange = ::onSeekProgressChanged,
                    onSeekFinished = ::onSeekFinished,
                    onSkipPrevious = ::skipPrevious,
                    onToggleRepeatMode = ::toggleRepeatMode,
                    onTogglePlayPause = ::togglePlayPause,
                    onSkipNext = ::skipNext,
                    onMenuAction = ::handleMenuAction,
                    onSelectQueueItem = ::handleSelectQueueItem,
                    onShowQueueItemDetail = ::showQueueItemDetail,
                    onDeleteQueueItem = { mediaScreenViewModel.showDeleteDialog(it) },
                    onPagerSettled = ::skipToCurrentPosition,
                    onDeleteDialogDismiss = mediaScreenViewModel::hideDeleteDialog,
                    onDeleteIncludeFileChanged = { includeFile -> mediaScreenViewModel.updateDeleteIncludeFile(includeFile) },
                    onDeleteConfirm = ::confirmDeleteQueueItem,
                    onQueueDetailDismiss = mediaScreenViewModel::hideQueueDetail,
                    onTimeSheetDismiss = mediaScreenViewModel::hideCountdownSheet,
                    onCustomTimeMinutesChange = { mediaScreenViewModel.updateCustomTimeMinutes(it) },
                    onTimeOptionSelected = ::handleCountdownOption,
                    onCustomTimeConfirm = ::confirmCustomCountdown,
                )
            }
        }
        requestStoragePermissionIfNeeded()
    }

    private fun requestStoragePermissionIfNeeded(onGranted: (() -> Unit)? = null) {
        if (!EasyPermissions.hasPermissions(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.string_permission_read),
                REQUEST_CODE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        } else {
            onGranted?.invoke()
        }
    }

    private fun handleMenuAction(action: MediaMenuAction) {
        when (action) {
            MediaMenuAction.Scan -> requestStoragePermissionIfNeeded(::handleReloadMedia)
            MediaMenuAction.Bluetooth -> openBluetoothManager()
            MediaMenuAction.NotifyStyle -> ChooseStyleDialog().show(supportFragmentManager, "选择通知栏样式")
            MediaMenuAction.CountDown -> mediaScreenViewModel.showCountdownSheet()
        }
    }

    private fun onSeekProgressChanged(progress: Float) {
        pendingSeekFraction = progress
        isTrackingBar = true
        val currentPosition = (endPosition * progress).toLong()
        mediaScreenViewModel.updateHeader { state ->
            state.copy(
                progress = progress,
                playingTime = DateUtil.get().getTime(currentPosition),
            )
        }
    }

    private fun onSeekFinished() {
        val mediaId = currentMediaId
        if (mediaId.isNullOrEmpty()) {
            isTrackingBar = false
            return
        }
        val extra = Bundle().apply {
            putString(MediaService.ACTION_PARAM, MediaService.ACTION_SEEK_TO)
            putLong(MediaService.SEEK_TO_POSITION_PARAM, (endPosition * pendingSeekFraction).toLong())
        }
        mPresenter?.playWithId(mediaId, extra)
        isTrackingBar = false
    }

    private fun toggleRepeatMode() {
        val nextMode = when (modeIndex) {
            PlaybackStateCompat.REPEAT_MODE_NONE -> PlaybackStateCompat.REPEAT_MODE_ONE
            PlaybackStateCompat.REPEAT_MODE_ONE -> PlaybackStateCompat.REPEAT_MODE_ALL
            else -> PlaybackStateCompat.REPEAT_MODE_NONE
        }
        applyRepeatMode(nextMode, true)
    }

    private fun togglePlayPause() {
        val extra = Bundle().apply { putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_WITH_ID) }
        if (currentMediaId != null) {
            mPresenter?.playWithId(currentMediaId!!, extra)
        } else {
            logd(getString(R.string.empty_play_queue))
            mPresenter?.playWithId("-1", extra)
        }
    }

    private fun skipPrevious() {
        mPresenter?.skipPrevious()
    }

    private fun skipNext() {
        mPresenter?.skipNext()
    }

    private fun openBluetoothManager() {
        startActivity(Intent(this, BlScanActivity::class.java))
    }

    private fun handleSelectQueueItem(index: Int) {
        val queueItems = mediaScreenViewModel.uiState.value.queuePreviewItems
        if (index !in queueItems.indices) {
            return
        }
        mediaScreenViewModel.setCurrentQueueIndex(index)
        mPresenter?.skipToPosition(index.toLong())
    }

    private fun showQueueItemDetail(position: Int) {
        val item = mediaScreenViewModel.uiState.value.queuePreviewItems.getOrNull(position) ?: return
        mediaScreenViewModel.showQueueDetail(
            title = String.format(Locale.CHINA, getString(R.string.show_name_and_author), item.title, item.subtitle),
            message = item.mediaUri.orEmpty(),
        )
    }

    private fun confirmDeleteQueueItem() {
        val state = mediaScreenViewModel.uiState.value.deleteDialogState ?: return
        when (mPresenter?.deleteFile(state.includeFile, state.position)) {
            null -> {
                doDelActionIncludeFile = state.includeFile
                needDocumentPermission(state.position)
            }
            true -> toast("删除成功")
            false -> toast("删除失败")
        }
        mediaScreenViewModel.hideDeleteDialog()
    }

    override fun setDeleteResult(isSuccess: Boolean, path: String?) = Unit

    override fun needDocumentPermission(position: Int) {
        toast("需要授予权限")
        doDelActionPosition = position
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), requestDelPermissionCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == requestDelPermissionCode && resultCode == Activity.RESULT_OK) {
            val treeUri = intent?.data ?: return
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
            mPresenter?.setGrantedRootUri(treeUri.toString(), childrenUri.toString())
            if (doDelActionPosition != -1) {
                mPresenter?.deleteFile(includeFile = doDelActionIncludeFile, doDelActionPosition)
                doDelActionPosition = -1
                doDelActionIncludeFile = true
            }
            return
        }
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            if (resultCode == -1) {
                finish()
                exitProcess(-1)
            } else {
                requestStoragePermissionIfNeeded()
            }
        }
    }

    private fun handleCountdownOption(position: Int) {
        val timeMillis = getCurrentTimeClockLength(position)
        mediaScreenViewModel.hideCountdownSheet()
        if (timeMillis == 0L) {
            return
        }
        mPresenter?.sendCustomAction(MediaService.ACTION_COUNT_DOWN_TIME, Bundle().apply {
            putLong(MediaService.ACTION_COUNT_DOWN_TIME, timeMillis)
        })
    }

    private fun confirmCustomCountdown() {
        mPresenter?.sendCustomAction(MediaService.ACTION_COUNT_DOWN_TIME, Bundle().apply {
            putLong(MediaService.ACTION_COUNT_DOWN_TIME, mediaScreenViewModel.uiState.value.countdownState.customTimeMinutes * 60_000L)
        })
        mediaScreenViewModel.hideCountdownSheet()
    }

    private fun getCurrentTimeClockLength(position: Int): Long {
        val ticking = mediaScreenViewModel.uiState.value.countdownState.text != null
        return if (ticking) {
            if (position == 0) {
                mPresenter?.sendCustomAction(MediaService.ACTION_STOP_COUNT_DOWN, Bundle())
                mediaScreenViewModel.updateCountdownText(null)
                0L
            } else {
                countdownOptions[position - 1] * 60_000L
            }
        } else {
            countdownOptions[position] * 60_000L
        }
    }

    override fun onResume() {
        super.onResume()
        mPresenter?.refreshQueue(false)
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>?) {
        if (EasyPermissions.hasPermissions(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return
        }
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms ?: mutableListOf())) {
            AppSettingsDialog.Builder(this).setNegativeButton(R.string.exit).build().show()
        } else {
            requestStoragePermissionIfNeeded()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>?) = handleReloadMedia()

    private fun resolveQueueSnapshot(queue: List<MediaSessionCompat.QueueItem>? = null): List<MediaSessionCompat.QueueItem> {
        return queue
            ?.takeIf { it.isNotEmpty() }
            ?: mediaController?.queue?.takeIf { it.isNotEmpty() }
            ?: DataProvider.get().queueItemList.get().orEmpty()
    }

    private fun syncMediaCollections(queue: List<MediaSessionCompat.QueueItem>? = null) {
        mediaScreenViewModel.setQueueSnapshot(resolveQueueSnapshot(queue), currentMediaId)
    }

    private fun skipToCurrentPosition(position: Int) {
        val item = mediaScreenViewModel.uiState.value.artworkPagerItems.getOrNull(position) ?: return
        val mediaId = item.mediaId
        if (mediaId == currentMediaId) {
            return
        }
        loge("准备播放第${position}首")
        mPresenter?.skipToPosition(position.toLong())
        calculatePalette(item.artwork)
    }

    private fun calculatePalette(artwork: Bitmap?) {
        // Keep the player on a stable brand palette instead of drifting with album artwork colors.
        updateThemeColor()
    }

    private fun updateThemeColor(color: Int? = null) {
        val themeColor = color ?: ContextCompat.getColor(this, R.color.colorTheme)
        val chromeColor = ContextCompat.getColor(this, R.color.colorSystemChrome)
        val useDarkSystemBarIcons = ColorUtils.calculateLuminance(chromeColor) > 0.5
        window.statusBarColor = chromeColor
        window.navigationBarColor = chromeColor
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = useDarkSystemBarIcons
            isAppearanceLightNavigationBars = useDarkSystemBarIcons
        }
        modifyThemeColor(themeColor)
        mediaScreenViewModel.updateThemeColor(themeColor)
    }

    private fun handleReloadMedia() {
        mediaScreenViewModel.setLibraryLoading(true)
        showMsg(getString(R.string.start_scanning_the_local_file))
        mPresenter?.refreshQueue(true)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                mediaController?.dispatchMediaButtonEvent(event)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(Bundle())
    }

    override fun loadFinished(isForce: Boolean) {
        if (mediaController == null) {
            connectMediaService()
            return
        }
        if (DataProvider.get().getPathList().isNotEmpty()) {
            resultReceive?.let { receiver ->
                mPresenter?.sendCommand(MediaService.COMMAND_UPDATE_QUEUE, Bundle().apply {
                    putBoolean(MediaService.EXTRA_SCAN_COMPLETE, isForce)
                }, receiver)
            }
        } else {
            mediaScreenViewModel.setEmptyPlaybackState(
                title = getString(R.string.app_name),
                subtitle = getString(R.string.app_name),
                timeLabel = getString(R.string.string_time_init),
            )
            showMsg(getString(R.string.no_media_searched))
            mPresenter?.playWithId("-1", Bundle())
        }
    }

    private fun connectMediaService() {
        if (mediaScreenViewModel.uiState.value.queuePreviewItems.isEmpty()) {
            mediaScreenViewModel.setLibraryLoading(true)
        }
        if (mediaBrowser?.isConnected == true) {
            mediaBrowser?.disconnect()
            mediaBrowser = null
        }
        if (mediaBrowser == null) {
            mediaBrowser = MediaBrowserCompat(this, ComponentName(this, MediaService::class.java), connectionCallback, null)
        }
        try {
            mediaBrowser?.connect()
        } catch (_: IllegalStateException) {
            PrintLog.e("正在连接服务...")
        }
    }

    override fun showLoading() {
        if (loader == null) {
            loader = Loader.show(this)
        }
    }

    override fun hideLoading() {
        loader?.hide()
        loader = null
    }

    private val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            if (isFinishing) {
                return
            }
            mediaBrowser?.subscribe(mediaBrowser!!.root, subscriptionCallback)
            mediaController = MediaControllerCompat(this@MediaActivity, mediaBrowser!!.sessionToken)
            resultReceive = MediaResultReceive(this@MediaActivity, Handler(Looper.myLooper() ?: Looper.getMainLooper()))
            MediaControllerCompat.setMediaController(this@MediaActivity, mediaController)
            mediaController?.registerCallback(controllerCallback)
            mPresenter.attachModelController(mediaController)
            mPresenter?.getLocalMode(applicationContext)
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            if (!isFinishing) connectMediaService()
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            if (!isFinishing) connectMediaService()
        }
    }

    override fun setRepeatMode(mode: Int) {
        if (mediaController != null) {
            applyRepeatMode(mode, false)
        }
    }

    private fun applyRepeatMode(mode: Int, showToast: Boolean) {
        modeIndex = mode
        mediaScreenViewModel.updateHeader { it.copy(repeatMode = mode) }
        if (showToast) {
            showMsg(
                when (mode) {
                    PlaybackStateCompat.REPEAT_MODE_ONE -> "单曲循环"
                    PlaybackStateCompat.REPEAT_MODE_ALL -> "列表循环"
                    else -> "顺序播放"
                },
            )
        }
        mPresenter?.setRepeatMode(applicationContext, mode)
    }

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            if (children.isNotEmpty()) {
                if (mediaController?.playbackState?.state != PlaybackStateCompat.STATE_NONE) {
                    mediaController?.metadata?.apply {
                        endPosition = getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                        updatePlaybackPosition(mediaController?.playbackState?.position ?: 0L)
                        currentMediaId = description.mediaId
                        setMediaInfo(description.title?.toString(), description.subtitle?.toString())
                        syncMediaCollections(mediaController?.queue)
                        calculatePalette(description.iconBitmap)
                    }
                    handlePlayState(mediaController?.playbackState?.state ?: PlaybackStateCompat.STATE_NONE)
                } else {
                    setMediaInfo(children[0].description.title?.toString(), children[0].description.subtitle?.toString())
                    currentMediaId = children[0].description.mediaId
                    currentMediaId?.let { mediaId ->
                        mPresenter?.playWithId(mediaId, Bundle().apply { putString(MediaService.ACTION_PARAM, MediaService.ACTION_PLAY_INIT) })
                    }
                }
                syncMediaCollections(mediaController?.queue)
            } else {
                mediaScreenViewModel.setEmptyPlaybackState(
                    title = getString(R.string.app_name),
                    subtitle = getString(R.string.app_name),
                    timeLabel = getString(R.string.string_time_init),
                )
            }
            hideLoading()
        }
    }

    private fun updatePlaybackPosition(position: Long) {
        if (isTrackingBar) {
            return
        }
        val safePosition = minOf(position, endPosition)
        val progress = if (endPosition > 0L) (safePosition.toFloat() / endPosition.toFloat()).coerceIn(0f, 1f) else 0f
        mediaScreenViewModel.updateHeader { state ->
            state.copy(
                playingTime = DateUtil.get().getTime(safePosition),
                durationTime = if (endPosition > 0L) DateUtil.get().getTime(endPosition) else getString(R.string.string_time_init),
                progress = progress,
            )
        }
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            if (metadata == null) return
            currentMediaId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            endPosition = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            updatePlaybackPosition(mediaController?.playbackState?.position ?: 0L)
            mediaScreenViewModel.uiState.value.queuePreviewItems
                .indexOfFirst { it.mediaId == currentMediaId }
                .takeIf { it >= 0 }
                ?.let(mediaScreenViewModel::setCurrentQueueIndex)
            setMediaInfo(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE), metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE))
            calculatePalette(metadata.description.iconBitmap)
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
            val snapshot = resolveQueueSnapshot(queue)
            if (snapshot.isEmpty()) {
                if (mediaScreenViewModel.uiState.value.isLibraryLoading) {
                    return
                }
                mediaScreenViewModel.setEmptyPlaybackState(
                    title = getString(R.string.app_name),
                    subtitle = getString(R.string.app_name),
                    timeLabel = getString(R.string.string_time_init),
                )
                updateThemeColor(null)
                return
            }
            syncMediaCollections(snapshot)
            showMsg("更新播放列表")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            updatePlaybackPosition(state?.position ?: 0L)
            if (mediaScreenViewModel.uiState.value.queuePreviewItems.isEmpty()) {
                setMediaInfo(getString(R.string.app_name), getString(R.string.app_name))
            }
            handlePlayState(state?.state ?: PlaybackStateCompat.STATE_NONE)
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            when (event) {
                MediaService.LOCAL_CACHE_POSITION_EVENT -> updatePlaybackPosition(extras?.getLong(MediaService.LOCAL_CACHE_POSITION_EVENT) ?: 0L)
                MediaService.ERROR_PLAY_QUEUE_EVENT -> showMsg(getString(R.string.empty_play_queue))
                MediaService.LOADING_QUEUE_EVENT -> {
                    mediaScreenViewModel.setLibraryLoading(true)
                    showMsg(getString(R.string.queue_loading))
                    showLoading()
                }
                MediaService.LOAD_COMPLETE_EVENT -> {
                    if (mediaScreenViewModel.uiState.value.queuePreviewItems.isNotEmpty()) {
                        mediaScreenViewModel.setLibraryLoading(false)
                    }
                    showMsg(getString(R.string.loading_complete))
                    hideLoading()
                }
                MediaService.ACTION_COUNT_DOWN_TIME -> {
                    mediaScreenViewModel.updateCountdownText(DateUtil.get().getTime(extras?.getLong(MediaService.ACTION_COUNT_DOWN_TIME) ?: 0L))
                }
                MediaService.ACTION_COUNT_DOWN_END -> {
                    mediaScreenViewModel.updateCountdownText(null)
                    disConnectService()
                    finish()
                }
                MediaService.RESET_SESSION_EVENT -> connectMediaService()
                MediaService.UPDATE_POSITION_EVENT -> updatePlaybackPosition(extras?.getInt(MediaService.UPDATE_POSITION_EVENT, 0)?.toLong() ?: 0L)
                else -> PrintLog.e("this event was not intercepted")
            }
        }
    }

    private fun handlePlayState(state: Int) {
        if (state != PlaybackStateCompat.STATE_PLAYING) {
            stopLoop()
            mediaScreenViewModel.updateHeader { it.copy(isPlaying = false) }
            if (state == PlaybackStateCompat.STATE_STOPPED) {
                updatePlaybackPosition(0L)
            }
        } else {
            startLoop()
            mediaScreenViewModel.updateHeader { it.copy(isPlaying = true) }
        }
    }

    private fun startLoop() {
        if (!isStarted) {
            mediaController?.sendCommand(MediaService.COMMAND_START_LOOP, null, null)
            isStarted = true
        }
    }

    private fun stopLoop() {
        if (isStarted) {
            mediaController?.sendCommand(MediaService.COMMAND_STOP_LOOP, null, null)
            isStarted = false
        }
    }

    private fun showMsg(msg: String) {
        val rootView = findViewById<View>(android.R.id.content) ?: contentView
        EasyTintView.makeText(rootView, msg, EasyTintView.TINT_SHORT).show()
    }

    private fun setMediaInfo(displayTitle: String?, subTitle: String?) {
        mediaScreenViewModel.updateHeader { state ->
            state.copy(
                title = displayTitle ?: getString(R.string.unknown_name),
                subtitle = subTitle ?: getString(R.string.unknown_author),
            )
        }
    }

    private fun disConnectService() {
        stopLoop()
        mediaController?.unregisterCallback(controllerCallback)
        mediaController = null
        MediaControllerCompat.setMediaController(this, null)
        if (mediaBrowser?.isConnected == true && mediaBrowser?.root != null) {
            mediaBrowser?.unsubscribe(mediaBrowser!!.root, subscriptionCallback)
        }
        mediaBrowser?.disconnect()
        mediaBrowser = null
    }

    override fun onDestroy() {
        super.onDestroy()
        disConnectService()
        resultReceive = null
        loader?.hide()
        loader = null
    }
}



