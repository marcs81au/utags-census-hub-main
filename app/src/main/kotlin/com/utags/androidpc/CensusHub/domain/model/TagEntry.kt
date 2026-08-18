package com.utags.androidpc.CensusHub.domain.model

data class TagEntry(
    val epc: String,
    val tid: String,
    val userData: String,
    var readCount: Int = 1
)
