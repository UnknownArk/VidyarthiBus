package com.vidyarthibus.data

import com.vidyarthibus.model.BusRoute
import com.vidyarthibus.model.CrowdReport

interface BusRepository {
    fun getRoutes(): List<BusRoute>
    fun getRoute(routeId: String): BusRoute?
    fun submitReport(report: CrowdReport): BusRoute?
}
