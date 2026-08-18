package com.utags.androidpc.CensusHub.domain.model

enum class ConnectionType(val label: String, val index: Int) {
    RS232("RS232", 0),
    TCP("TCP", 1),
    USB_SERIAL("USB to Serial", 2),
    BLUETOOTH("Classic Bluetooth", 3),
    RS485("RS485", 4),
    USB_RS485("USB to RS485", 5),
    USB_HID("USB HID", 6);

    companion object {
        fun fromIndex(index: Int) = values().firstOrNull { it.index == index } ?: BLUETOOTH
    }
}

data class ConnectionConfig(
    val type: ConnectionType = ConnectionType.BLUETOOTH,
    val param: String = ""
)
