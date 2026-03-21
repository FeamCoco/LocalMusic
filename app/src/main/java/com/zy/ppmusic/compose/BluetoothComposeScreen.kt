package com.zy.ppmusic.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zy.ppmusic.R
import com.zy.ppmusic.compose.theme.LocalMusicComposeTheme
import com.zy.ppmusic.ui.bluetooth.BLUETOOTH_SCREEN_PAIRED_HEADER
import com.zy.ppmusic.ui.bluetooth.BluetoothScanItemUiState
import com.zy.ppmusic.ui.bluetooth.BluetoothScreenUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothComposeScreen(
    state: BluetoothScreenUiState,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    onToggleBluetooth: (Boolean) -> Unit,
    onDeviceClick: (String) -> Unit,
    onDeleteBondClick: (String) -> Unit,
    onTransientMessageConsumed: () -> Unit,
) {
    val rotation = if (state.isRefreshing) {
        rememberInfiniteTransition(label = "refresh").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing), repeatMode = RepeatMode.Restart),
            label = "refreshRotation",
        ).value
    } else 0f
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = MaterialTheme.colorScheme
    val background = Brush.verticalGradient(listOf(colors.surface, colors.primaryContainer.copy(alpha = 0.14f), colors.background))
    val deviceItems = state.items.filter { it.deviceAddress != null }

    state.transientMessage?.let { message ->
        LaunchedEffect(message.token) {
            snackbarHostState.showSnackbar(message.message)
            onTransientMessageConsumed()
        }
    }

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painter = painterResource(R.mipmap.ic_action_close), contentDescription = stringResource(R.string.string_close))
                    }
                },
                title = {
                    Text(text = "蓝牙设备", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(painter = painterResource(R.mipmap.refresh), contentDescription = stringResource(R.string.menu_refresh), modifier = Modifier.rotate(rotation))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(background).padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = colors.surface) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = state.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = when {
                                        !state.enabled -> "开启蓝牙后即可扫描和管理耳机设备。"
                                        state.isRefreshing -> "正在刷新附近设备与已配对设备状态。"
                                        deviceItems.isEmpty() -> "暂时没有可显示的设备，建议重新扫描。"
                                        else -> "轻触设备即可连接、断开或发起配对。"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            Switch(checked = state.enabled, onCheckedChange = onToggleBluetooth)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(if (state.enabled) "蓝牙已开启" else "蓝牙已关闭") },
                                colors = AssistChipDefaults.assistChipColors(
                                    disabledContainerColor = if (state.enabled) colors.primaryContainer else colors.surfaceVariant,
                                    disabledLabelColor = if (state.enabled) colors.onPrimaryContainer else colors.onSurfaceVariant,
                                ),
                            )
                            if (state.isRefreshing) {
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = { Text("扫描中") },
                                    colors = AssistChipDefaults.assistChipColors(disabledContainerColor = colors.secondaryContainer, disabledLabelColor = colors.onSecondaryContainer),
                                )
                            }
                        }
                        if (!state.enabled) {
                            Button(onClick = { onToggleBluetooth(true) }, modifier = Modifier.fillMaxWidth()) {
                                Text("开启蓝牙")
                            }
                        }
                    }
                }
            }
            if (deviceItems.isEmpty()) {
                item {
                    Surface(shape = MaterialTheme.shapes.extraLarge, color = colors.surface) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(modifier = Modifier.size(72.dp), shape = CircleShape, color = colors.primaryContainer) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(painter = painterResource(R.mipmap.ic_device), contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(32.dp))
                                }
                            }
                            Text(text = if (state.isRefreshing) "正在查找设备" else "还没有设备结果", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (state.isRefreshing) "请保持耳机处于可连接状态，扫描完成后会出现在列表中。" else "你可以刷新列表，或先打开蓝牙与系统定位权限。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                            if (state.enabled) {
                                TextButton(onClick = onRefresh) { Text("重新扫描") }
                            }
                        }
                    }
                }
            } else {
                items(state.items) { item ->
                    if (item.deviceAddress == null) {
                        Text(
                            text = item.title.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = if (item.title == BLUETOOTH_SCREEN_PAIRED_HEADER) 4.dp else 10.dp),
                        )
                    } else {
                        DeviceCard(item = item, onDeviceClick = onDeviceClick, onDeleteBondClick = onDeleteBondClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(item: BluetoothScanItemUiState, onDeviceClick: (String) -> Unit, onDeleteBondClick: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val primaryLabel = when {
        item.statusText == "已连接" -> "断开"
        item.bondStateLabel == "已配对" -> "连接"
        item.bondStateLabel == "正在配对" -> "等待"
        else -> "配对"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = colors.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painter = painterResource(R.mipmap.ic_device), contentDescription = stringResource(R.string.scan_result_icon), tint = colors.onPrimaryContainer, modifier = Modifier.size(24.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = item.displayName ?: "unknown", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    item.deviceAddress?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item.bondStateLabel?.takeIf { it.isNotBlank() }?.let {
                            AssistChip(onClick = {}, enabled = false, label = { Text(it) })
                        }
                        item.statusText?.takeIf { it.isNotBlank() }?.let {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(it) },
                                colors = AssistChipDefaults.assistChipColors(disabledContainerColor = colors.secondaryContainer, disabledLabelColor = colors.onSecondaryContainer),
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { item.deviceAddress?.let(onDeviceClick) }, enabled = primaryLabel != "等待") {
                    Text(primaryLabel)
                }
                if (item.canDeleteBond) {
                    TextButton(onClick = { item.deviceAddress?.let(onDeleteBondClick) }) {
                        Text("取消配对")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6FBF8)
@Composable
private fun BluetoothComposeScreenPreview() {
    LocalMusicComposeTheme(dynamicColor = false) {
        BluetoothComposeScreen(
            state = BluetoothScreenUiState(enabled = true, isRefreshing = true),
            onClose = {},
            onRefresh = {},
            onToggleBluetooth = {},
            onDeviceClick = {},
            onDeleteBondClick = {},
            onTransientMessageConsumed = {},
        )
    }
}
