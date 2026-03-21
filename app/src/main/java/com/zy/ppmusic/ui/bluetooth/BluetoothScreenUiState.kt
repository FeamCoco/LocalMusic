package com.zy.ppmusic.ui.bluetooth

import com.zy.ppmusic.entity.ScanResultEntity

const val BLUETOOTH_SCREEN_TITLE_DISABLED = "蓝牙已关闭"
const val BLUETOOTH_SCREEN_TITLE_ENABLING = "蓝牙正在开启"
const val BLUETOOTH_SCREEN_TITLE_ENABLED = "蓝牙已开启"
const val BLUETOOTH_SCREEN_TITLE_DISABLING = "蓝牙正在关闭"
const val BLUETOOTH_SCREEN_EMPTY_TEXT = "等待扫描结果"
const val BLUETOOTH_SCREEN_PAIRED_HEADER = "已配对的设备"
const val BLUETOOTH_SCREEN_SCANNED_HEADER = "扫描到的设备"

data class BluetoothTransientMessageUiState(
    val message: String,
    val token: Long,
)

data class BluetoothScanItemUiState(
    val type: Int,
    val title: String? = null,
    val deviceAddress: String? = null,
    val displayName: String? = null,
    val statusText: String? = null,
    val bondStateLabel: String? = null,
    val canDeleteBond: Boolean = false,
) {
    companion object {
        fun title(title: String): BluetoothScanItemUiState {
            return BluetoothScanItemUiState(
                type = ScanResultEntity.TYPE_TITLE,
                title = title,
            )
        }

        fun device(
            address: String,
            displayName: String,
            statusText: String? = null,
            bondStateLabel: String,
            canDeleteBond: Boolean = false,
        ): BluetoothScanItemUiState {
            return BluetoothScanItemUiState(
                type = ScanResultEntity.TYPE_DEVICE,
                deviceAddress = address,
                displayName = displayName,
                statusText = statusText,
                bondStateLabel = bondStateLabel,
                canDeleteBond = canDeleteBond,
            )
        }

        fun from(entity: ScanResultEntity): BluetoothScanItemUiState {
            val device = entity.device
            val address = device?.address ?: entity.title.orEmpty()
            val bondState = device?.bondState
            return if (entity.type == ScanResultEntity.TYPE_TITLE) {
                title(entity.title.orEmpty())
            } else {
                device(
                    address = address,
                    displayName = device?.name ?: "unknown",
                    statusText = entity.state,
                    bondStateLabel = bondStateLabel(bondState),
                    canDeleteBond = bondState == android.bluetooth.BluetoothDevice.BOND_BONDED,
                )
            }
        }

        fun bondStateLabel(state: Int?): String {
            return when (state) {
                android.bluetooth.BluetoothDevice.BOND_BONDING -> "正在配对"
                android.bluetooth.BluetoothDevice.BOND_BONDED -> "已配对"
                else -> "未配对"
            }
        }
    }
}

data class BluetoothScreenUiState(
    val title: String = BLUETOOTH_SCREEN_TITLE_DISABLED,
    val enabled: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: List<BluetoothScanItemUiState> = emptyList(),
    val transientMessage: BluetoothTransientMessageUiState? = null,
)
