package com.vidyarthibus.model

data class CrowdReport(
    val routeId: String,
    val status: RouteStatus,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)
