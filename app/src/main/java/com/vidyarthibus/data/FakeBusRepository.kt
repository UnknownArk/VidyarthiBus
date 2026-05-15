package com.vidyarthibus.data

import com.vidyarthibus.model.BusRoute
import com.vidyarthibus.model.CrowdReport
import com.vidyarthibus.model.DataSource
import com.vidyarthibus.model.RouteStatus
import android.content.Context
import org.json.JSONArray

class FakeBusRepository(private val context: Context) : BusRepository {
    private val reportStore = SharedPreferencesReportStore(context)
    private var routes: MutableList<BusRoute> = mutableListOf()

    init {
        loadRoutesFromAssets()
        val savedReports = reportStore.loadReports()
        savedReports.forEach { applyReport(it, persist = false) }
    }

    private fun loadRoutesFromAssets() {
        try {
            val jsonString = context.assets.open("bmtc_routes.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val stopsArray = obj.getJSONArray("stops")
                val stopsList = mutableListOf<String>()
                for (j in 0 until stopsArray.length()) {
                    stopsList.add(stopsArray.getString(j))
                }
                
                routes.add(BusRoute(
                    id = obj.getString("id"),
                    number = obj.getString("number"),
                    name = obj.getString("name"),
                    departureTime = obj.getString("departureTime"),
                    arrivalTime = obj.getString("arrivalTime"),
                    stops = stopsList,
                    capacity = 50,
                    status = RouteStatus.UNKNOWN,
                    lastUpdateMinutesAgo = 15,
                    reportCount = 0,
                    confidence = 0,
                    dataSource = DataSource.NONE,
                    nextHourPrediction = RouteStatus.UNKNOWN,
                    geofenceCenterLat = obj.getDouble("geofenceCenterLat"),
                    geofenceCenterLng = obj.getDouble("geofenceCenterLng"),
                    geofenceRadiusMeters = obj.getInt("geofenceRadiusMeters"),
                    predictionReason = obj.getString("predictionReason")
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            routes = sampleRoutes().toMutableList() // Fallback
        }
    }

    override fun getRoutes(): List<BusRoute> = routes.map { applyExpiry(it) }

    override fun getRoute(routeId: String): BusRoute? = routes.firstOrNull { it.id == routeId }?.let { applyExpiry(it) }

    private fun applyExpiry(route: BusRoute): BusRoute {
        val fifteenMinsAgo = System.currentTimeMillis() - 15 * 60 * 1000
        if (route.lastReportTimestamp > 0 && route.lastReportTimestamp < fifteenMinsAgo) {
            return route.copy(status = RouteStatus.UNKNOWN, reportCount = 0, confidence = 0, lastUpdateMinutesAgo = 15)
        }
        return route
    }

    fun replaceRoutes(newRoutes: List<BusRoute>) {
        if (newRoutes.isEmpty()) return
        routes.clear()
        routes.addAll(newRoutes.sortedBy { it.number.toIntOrNull() ?: Int.MAX_VALUE })
    }

    override fun submitReport(report: CrowdReport): BusRoute? = applyReport(report, persist = true)

    private fun applyReport(report: CrowdReport, persist: Boolean): BusRoute? {
        val index = routes.indexOfFirst { it.id == report.routeId }
        if (index == -1) return null

        val old = routes[index]
        val updated = old.copy(
            status = report.status,
            lastUpdateMinutesAgo = 0,
            reportCount = old.reportCount + 1,
            confidence = (old.confidence + 4).coerceAtMost(96),
            dataSource = DataSource.LIVE_REPORTS,
            lastReportTimestamp = report.timestamp
        )
        routes[index] = updated
        if (persist) reportStore.saveReport(report)
        return updated
    }

    companion object {
        fun sampleRoutes(): List<BusRoute> = listOf(
            BusRoute(
                id = "r42",
                name = "Main Campus Express",
                number = "42",
                departureTime = "7:35 AM",
                arrivalTime = "8:25 AM",
                stops = listOf("Village Circle", "Market Road", "Library Gate", "Main Campus"),
                capacity = 52,
                status = RouteStatus.SEATED,
                lastUpdateMinutesAgo = 3,
                reportCount = 18,
                confidence = 84,
                dataSource = DataSource.LIVE_REPORTS,
                nextHourPrediction = RouteStatus.FULL,
                predictionReason = "Morning lecture rush is building near Market Road.",
                geofenceCenterLat = 12.9716,
                geofenceCenterLng = 77.5946,
                geofenceRadiusMeters = 1200
            ),
            BusRoute(
                id = "r18",
                name = "North Hostel Shuttle",
                number = "18",
                departureTime = "7:50 AM",
                arrivalTime = "8:15 AM",
                stops = listOf("North Hostel", "Sports Ground", "Admin Block", "Main Campus"),
                capacity = 38,
                status = RouteStatus.EMPTY,
                lastUpdateMinutesAgo = 1,
                reportCount = 11,
                confidence = 91,
                dataSource = DataSource.LIVE_REPORTS,
                nextHourPrediction = RouteStatus.SEATED,
                predictionReason = "Current reports show enough seats, but demand rises after 8 AM.",
                geofenceCenterLat = 12.9757,
                geofenceCenterLng = 77.5929,
                geofenceRadiusMeters = 900
            ),
            BusRoute(
                id = "r7",
                name = "Railway Station Link",
                number = "7",
                departureTime = "8:05 AM",
                arrivalTime = "8:45 AM",
                stops = listOf("Station East", "Bus Stand", "Market Road", "Main Campus"),
                capacity = 48,
                status = RouteStatus.FULL,
                lastUpdateMinutesAgo = 4,
                reportCount = 23,
                confidence = 88,
                dataSource = DataSource.LIVE_REPORTS,
                nextHourPrediction = RouteStatus.FULL,
                predictionReason = "Station crowd is high and two users marked the bus full.",
                geofenceCenterLat = 12.9774,
                geofenceCenterLng = 77.5708,
                geofenceRadiusMeters = 1500
            ),
            BusRoute(
                id = "r31",
                name = "South Village Route",
                number = "31",
                departureTime = "8:20 AM",
                arrivalTime = "9:05 AM",
                stops = listOf("South Village", "Temple Stop", "Canal Bridge", "Main Campus"),
                capacity = 44,
                status = RouteStatus.UNKNOWN,
                lastUpdateMinutesAgo = 0,
                reportCount = 0,
                confidence = 45,
                dataSource = DataSource.PREDICTION,
                nextHourPrediction = RouteStatus.SEATED,
                predictionReason = "No fresh reports yet; prediction is based on usual Thursday traffic.",
                geofenceCenterLat = 12.9629,
                geofenceCenterLng = 77.6023,
                geofenceRadiusMeters = 1300
            )
        )
    }
}
