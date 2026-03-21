package com.zy.ppmusic.mvp.view

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zy.ppmusic.R
import com.zy.ppmusic.compose.BluetoothComposeScreen
import com.zy.ppmusic.compose.theme.LocalMusicComposeTheme
import com.zy.ppmusic.entity.ScanResultEntity
import com.zy.ppmusic.mvp.base.AbstractBaseMvpActivity
import com.zy.ppmusic.mvp.contract.IBLActivityContract
import com.zy.ppmusic.mvp.presenter.BlActivityPresenter
import com.zy.ppmusic.receiver.DeviceFoundReceiver
import com.zy.ppmusic.receiver.StatusChangeReceiver
import com.zy.ppmusic.ui.bluetooth.BluetoothScanItemUiState
import com.zy.ppmusic.ui.bluetooth.BluetoothScreenViewModel
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class BlScanActivity : AbstractBaseMvpActivity<BlActivityPresenter>(), IBLActivityContract.IBLActivityView,
    EasyPermissions.PermissionCallbacks {

    private val blEnableRequestCode = 0x001
    private var blStateChangeReceiver: StatusChangeReceiver? = null
    private var deviceFoundReceiver: DeviceFoundReceiver? = null
    private val bluetoothDeviceCache = LinkedHashMap<String, BluetoothDevice>()
    private val bluetoothScreenViewModel by lazy { ViewModelProvider(this).get(BluetoothScreenViewModel::class.java) }

    override fun getContentViewId(): Int = R.layout.activity_compose_host

    override fun createPresenter(): BlActivityPresenter = BlActivityPresenter(this)

    override fun initViews() {
        if (mPresenter?.isSupportBl() != true) {
            android.widget.Toast.makeText(this, getString(R.string.unsupport_bluetooth), android.widget.Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (mPresenter?.isEnable() == true) {
            bluetoothScreenViewModel.updateStatus(title = "蓝牙已开启", enabled = true)
            mPresenter?.getExitsDevices()
            mPresenter?.startDiscovery()
        } else {
            bluetoothScreenViewModel.updateStatus(title = "蓝牙已关闭", enabled = false, isRefreshing = false)
        }
        setContent {
            LocalMusicComposeTheme {
                val uiState by bluetoothScreenViewModel.uiState.collectAsStateWithLifecycle()
                BluetoothComposeScreen(
                    state = uiState,
                    onClose = ::finish,
                    onRefresh = { mPresenter?.getExitsDevices() },
                    onToggleBluetooth = ::toggleBluetooth,
                    onDeviceClick = ::handleDeviceClick,
                    onDeleteBondClick = ::handleDeleteBondClick,
                    onTransientMessageConsumed = bluetoothScreenViewModel::consumeTransientMessage,
                )
            }
        }
        checkLocationPermission()
    }

    private fun toggleBluetooth(isChecked: Boolean) {
        if (isChecked) {
            if (mPresenter?.isEnable() != true) {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                startActivityForResult(intent, blEnableRequestCode)
            }
        } else {
            mPresenter?.disable()
        }
    }

    private fun checkLocationPermission() {
        if (!EasyPermissions.hasPermissions(applicationContext, "android.permission.ACCESS_COARSE_LOCATION")) {
            if (!EasyPermissions.permissionPermanentlyDenied(this, "android.permission.ACCESS_COARSE_LOCATION")) {
                EasyPermissions.requestPermissions(this, "获取粗略位置用来加快扫描", 1, "android.permission.ACCESS_COARSE_LOCATION")
            } else {
                AppSettingsDialog.Builder(this)
                    .setRationale("没有位置信息将无法获取新设备，这是安卓6.0之后的系统要求，请允许权限")
                    .setNegativeButton("任性不给")
                    .setPositiveButton("赏给你")
                    .build()
                    .show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>?) {
        if (requestCode == 1 && mPresenter?.isEnable() == true) {
            mPresenter?.startDiscovery()
            bluetoothScreenViewModel.syncDiscoveryState(true)
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>?) {
        if (EasyPermissions.hasPermissions(applicationContext, "android.permission.ACCESS_COARSE_LOCATION")) {
            return
        }
        if (requestCode == 1) {
            if (EasyPermissions.somePermissionPermanentlyDenied(this, perms ?: mutableListOf())) {
                AppSettingsDialog.Builder(this)
                    .setRationale("没有位置信息将无法获取新设备，这是安卓6.0之后的系统要求，请允许权限")
                    .build()
                    .show()
            } else {
                EasyPermissions.requestPermissions(this, "获取粗略位置用来加快扫描,否则无法发现新设备", 1, "android.permission.ACCESS_COARSE_LOCATION")
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun setExitsDevices(exitList: MutableList<ScanResultEntity>) {
        exitList.forEach { cacheDevice(it.device) }
        bluetoothScreenViewModel.setPairedDevices(exitList)
        refreshConnectionStates()
        mPresenter?.startDiscovery()
        bluetoothScreenViewModel.syncDiscoveryState(true)
    }

    override fun getContext(): Context = applicationContext

    fun onBLStateChange(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_TURNING_ON -> bluetoothScreenViewModel.setEnabling()
            BluetoothAdapter.STATE_ON -> {
                bluetoothScreenViewModel.setEnabled(true)
                checkLocationPermission()
            }
            BluetoothAdapter.STATE_TURNING_OFF -> bluetoothScreenViewModel.setDisabling()
            BluetoothAdapter.STATE_OFF -> {
                bluetoothScreenViewModel.setEnabled(false)
                clearData()
            }
        }
    }

    fun discoveryStateChange(state: String) {
        when (state) {
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> bluetoothScreenViewModel.syncDiscoveryState(false)
            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                bluetoothScreenViewModel.syncDiscoveryState(true)
                checkLocationPermission()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun onDeviceBondStateChanged(state: Int, device: BluetoothDevice) {
        cacheDevice(device)
        bluetoothScreenViewModel.updateDeviceBondState(device.address, device.name.orEmpty(), state)
        when (state) {
            BluetoothDevice.BOND_BONDED -> mPresenter?.getExitsDevices()
            BluetoothDevice.BOND_NONE -> device.createBond()
        }
    }

    fun connectStateChanged() {
        refreshConnectionStates()
    }

    override fun onResume() {
        super.onResume()
        bluetoothScreenViewModel.syncDiscoveryState(mPresenter?.isDiscovering() == true)
        if (deviceFoundReceiver == null) {
            deviceFoundReceiver = DeviceFoundReceiver(this)
        }
        if (blStateChangeReceiver == null) {
            blStateChangeReceiver = StatusChangeReceiver(this)
        }
        val foundFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        foundFilter.addAction("android.bluetooth.device.action.DISAPPEARED")
        registerReceiver(deviceFoundReceiver, foundFilter)

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        filter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        registerReceiver(blStateChangeReceiver, filter)
        checkConnectDevice()
    }

    private fun checkConnectDevice() {
        refreshConnectionStates()
    }

    private fun refreshConnectionStates() {
        bluetoothScreenViewModel.updateConnectionStates { address ->
            when (resolveBluetoothDevice(address)?.let { mPresenter?.getConnectState(it) }) {
                BluetoothA2dp.STATE_CONNECTING -> "正在连接"
                BluetoothA2dp.STATE_CONNECTED -> "已连接"
                else -> ""
            }
        }
    }

    fun foundNewDevice(device: BluetoothDevice) {
        cacheDevice(device)
        bluetoothScreenViewModel.addFoundDevice(
            address = device.address,
            displayName = device.name ?: "unknown",
            bondState = device.bondState,
        )
    }

    override fun onPause() {
        super.onPause()
        bluetoothScreenViewModel.syncDiscoveryState(false)
        deviceFoundReceiver?.let { unregisterReceiver(it) }
        blStateChangeReceiver?.let { unregisterReceiver(it) }
        mPresenter?.cancelDiscovery()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == blEnableRequestCode && resultCode == Activity.RESULT_OK) {
            mPresenter?.getExitsDevices()
            mPresenter?.startDiscovery()
            bluetoothScreenViewModel.syncDiscoveryState(true)
            onBLStateChange(BluetoothAdapter.STATE_ON)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @SuppressLint("MissingPermission")
    private fun handleDeviceClick(deviceAddress: String) {
        val device = resolveBluetoothDevice(deviceAddress) ?: return
        val deviceClass = device.bluetoothClass?.deviceClass
        if (deviceClass != BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET &&
            deviceClass != BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
        ) {
            bluetoothScreenViewModel.postTransientMessage("此设备不可作为蓝牙耳机")
            return
        }
        when (device.bondState) {
            BluetoothDevice.BOND_BONDED -> {
                if (mPresenter?.isDiscovering() == true) {
                    mPresenter?.cancelDiscovery()
                }
                if (mPresenter?.isConnected(device) == true) {
                    mPresenter?.disconnectDevice(device)
                } else {
                    mPresenter?.connectDevice(device)
                }
                mPresenter?.startDiscovery()
            }
            BluetoothDevice.BOND_BONDING -> mPresenter?.getExitsDevices()
            BluetoothDevice.BOND_NONE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    device.createBond()
                }
                mPresenter?.getExitsDevices()
            }
        }
    }

    private fun handleDeleteBondClick(deviceAddress: String) {
        val device = resolveBluetoothDevice(deviceAddress) ?: return
        if (mPresenter?.removeBondDevice(device) == true) {
            bluetoothScreenViewModel.removeBondedDevice(device.address)
            bluetoothDeviceCache.remove(device.address)
        }
    }

    private fun clearData() {
        bluetoothScreenViewModel.clearDevices()
    }

    private fun cacheDevice(device: BluetoothDevice?) {
        if (device == null || device.address.isNullOrBlank()) {
            return
        }
        bluetoothDeviceCache[device.address] = device
    }

    private fun resolveBluetoothDevice(address: String): BluetoothDevice? {
        if (address.isBlank()) {
            return null
        }
        bluetoothDeviceCache[address]?.let { return it }
        mPresenter?.getBondDevice()?.firstOrNull { it.address == address }?.let { return it }
        mPresenter?.getConnectDevice()?.firstOrNull { it.address == address }?.let { return it }
        return null
    }
}



