package com.zy.ppmusic.entity

import android.bluetooth.BluetoothDevice

class ScanResultEntity {
    var type: Int = 0
    var title: String? = null
    var state: String? = null
    var device: BluetoothDevice? = null

    constructor()

    constructor(type: Int, title: String) {
        this.type = type
        this.title = title
    }

    constructor(type: Int, device: BluetoothDevice) {
        this.type = type
        this.device = device
    }

    override fun toString(): String {
        return "ScanResultEntity{" +
            "type=" + type +
            ", title='" + title + '\'' +
            ", state='" + state + '\'' +
            ", device=" + device +
            '}'
    }

    companion object {
        const val TYPE_TITLE = 1
        const val TYPE_DEVICE = 2
    }
}
