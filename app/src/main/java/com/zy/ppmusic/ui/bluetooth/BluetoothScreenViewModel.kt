package com.zy.ppmusic.ui.bluetooth

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import com.zy.ppmusic.entity.ScanResultEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BluetoothScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BluetoothScreenUiState())
    val uiState: StateFlow<BluetoothScreenUiState> = _uiState.asStateFlow()

    private val pairedItems = LinkedHashMap<String, BluetoothScanItemUiState>()
    private val discoveredItems = LinkedHashMap<String, BluetoothScanItemUiState>()
    private var messageToken = 0L

    fun updateStatus(
        title: String? = null,
        enabled: Boolean? = null,
        isRefreshing: Boolean? = null,
    ) {
        updateState { state ->
            state.copy(
                title = title ?: state.title,
                enabled = enabled ?: state.enabled,
                isRefreshing = isRefreshing ?: state.isRefreshing,
            )
        }
    }

    fun setRefreshing(isRefreshing: Boolean) {
        updateState { it.copy(isRefreshing = isRefreshing) }
    }

    fun syncDiscoveryState(isDiscovering: Boolean) {
        updateState { it.copy(isRefreshing = isDiscovering) }
    }

    fun setEnabled(enabled: Boolean) {
        updateState { state ->
            state.copy(
                enabled = enabled,
                title = if (enabled) BLUETOOTH_SCREEN_TITLE_ENABLED else BLUETOOTH_SCREEN_TITLE_DISABLED,
                isRefreshing = if (enabled) state.isRefreshing else false,
            )
        }
        if (!enabled) {
            clearDevices()
        }
    }

    fun setEnabling() {
        updateState { it.copy(title = BLUETOOTH_SCREEN_TITLE_ENABLING) }
    }

    fun setDisabling() {
        updateState { it.copy(title = BLUETOOTH_SCREEN_TITLE_DISABLING) }
    }

    fun setPairedDevices(exitList: List<ScanResultEntity>) {
        pairedItems.clear()
        exitList.forEach { entity ->
            val item = BluetoothScanItemUiState.from(entity)
            item.deviceAddress?.let { address ->
                pairedItems[address] = item.copy(canDeleteBond = true)
            }
        }
        rebuildItems()
    }

    fun addFoundDevice(
        address: String,
        displayName: String,
        bondState: Int? = null,
        statusText: String? = null,
    ) {
        if (address.isBlank()) return
        discoveredItems[address] = BluetoothScanItemUiState.device(
            address = address,
            displayName = displayName,
            statusText = statusText,
            bondStateLabel = BluetoothScanItemUiState.bondStateLabel(bondState),
            canDeleteBond = bondState == BluetoothDevice.BOND_BONDED,
        )
        rebuildItems()
    }

    fun updateDeviceBondState(
        address: String,
        displayName: String,
        bondState: Int,
    ) {
        if (address.isBlank()) return
        val target = BluetoothScanItemUiState.device(
            address = address,
            displayName = displayName.ifBlank { "unknown" },
            statusText = when (bondState) {
                BluetoothDevice.BOND_BONDING -> "正在配对"
                else -> null
            },
            bondStateLabel = BluetoothScanItemUiState.bondStateLabel(bondState),
            canDeleteBond = bondState == BluetoothDevice.BOND_BONDED,
        )
        if (pairedItems.containsKey(address)) {
            pairedItems[address] = target
        } else {
            discoveredItems[address] = target
        }
        rebuildItems()
    }

    fun updateConnectionStates(stateResolver: (String) -> String?) {
        val updatedPaired = pairedItems.mapValues { (_, item) ->
            item.copy(statusText = stateResolver(item.deviceAddress.orEmpty()).orEmpty())
        }
        val updatedDiscovered = discoveredItems.mapValues { (_, item) ->
            item.copy(statusText = stateResolver(item.deviceAddress.orEmpty()).orEmpty())
        }
        pairedItems.clear()
        pairedItems.putAll(updatedPaired)
        discoveredItems.clear()
        discoveredItems.putAll(updatedDiscovered)
        rebuildItems()
    }

    fun removeBondedDevice(address: String) {
        if (address.isBlank()) return
        pairedItems.remove(address)
        discoveredItems.remove(address)
        rebuildItems()
    }

    fun clearDevices() {
        pairedItems.clear()
        discoveredItems.clear()
        rebuildItems()
    }

    fun postTransientMessage(message: String) {
        messageToken += 1
        updateState { it.copy(transientMessage = BluetoothTransientMessageUiState(message = message, token = messageToken)) }
    }

    fun consumeTransientMessage() {
        updateState { it.copy(transientMessage = null) }
    }

    private inline fun updateState(transform: (BluetoothScreenUiState) -> BluetoothScreenUiState) {
        _uiState.update(transform)
    }

    private fun rebuildItems() {
        val pairedAddresses = pairedItems.keys.toSet()
        val discovered = discoveredItems.values.filterNot { pairedAddresses.contains(it.deviceAddress) }
        val hasAnyDevices = pairedItems.isNotEmpty() || discovered.isNotEmpty()
        updateState {
            it.copy(
                items = if (!hasAnyDevices) {
                    emptyList()
                } else {
                    buildList {
                        if (pairedItems.isNotEmpty()) {
                            add(BluetoothScanItemUiState.title(BLUETOOTH_SCREEN_PAIRED_HEADER))
                            addAll(pairedItems.values)
                        }
                        if (discovered.isNotEmpty()) {
                            add(BluetoothScanItemUiState.title(BLUETOOTH_SCREEN_SCANNED_HEADER))
                            addAll(discovered)
                        }
                    }
                },
            )
        }
    }
}
