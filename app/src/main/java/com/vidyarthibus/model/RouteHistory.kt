package com.vidyarthibus.model

data class RouteHistory(
    val routeId: String,
    val historyPoints: List<HistoryPoint>
) {
    data class HistoryPoint(
        val timestamp: Long,
        val reportCount: Int
    )
}
