package com.utags.androidpc.CensusHub.domain.model

data class AntennaPower(
    val antennaNumber: Int,
    val power: Int,
    val enabled: Boolean
)

data class ReaderSettings(
    val antennaPowers: List<AntennaPower> = emptyList(),
    val selectedAntennas: Set<Int> = setOf(1),  // bitmask antenna selection
    val frequencyIndex: Int = 0,
    val baseSpeedTypeIndex: Int = 0,
    val baseSpeedQValue: Int = 0,
    val beepEnabled: Boolean = true,
    val tagType: String = "6C",
    val filterTime: Int = 0,
    val rssiFilter: Int = 0,
    val minPower: Int = 0,
    val maxPower: Int = 30,
    val antennaCount: Int = 1
)
