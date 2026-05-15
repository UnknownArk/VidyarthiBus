package com.vidyarthibus.model

data class BusRoute(
    val id: String,
    val name: String,
    val number: String,
    val departureTime: String,
    val arrivalTime: String,
    val stops: List<String>,
    val capacity: Int,
    val status: RouteStatus,
    val lastUpdateMinutesAgo: Int,
    val reportCount: Int,
    val confidence: Int,
    val dataSource: DataSource,
    val nextHourPrediction: RouteStatus,
    val predictionReason: String,
    val geofenceCenterLat: Double,
    val geofenceCenterLng: Double,
    val geofenceRadiusMeters: Int,
    val lastReportTimestamp: Long = 0
)
