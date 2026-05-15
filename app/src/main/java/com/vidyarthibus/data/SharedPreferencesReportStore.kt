package com.vidyarthibus.data

import android.content.Context
import com.vidyarthibus.model.CrowdReport
import com.vidyarthibus.model.RouteStatus

class SharedPreferencesReportStore(context: Context) : ReportStore {
    private val preferences = context.getSharedPreferences("vidyarthi_bus_reports", Context.MODE_PRIVATE)

    override fun loadReports(): List<CrowdReport> {
        val encoded = preferences.getString(KEY_REPORTS, null).orEmpty()
        if (encoded.isBlank()) return emptyList()

        val fifteenMinsAgo = System.currentTimeMillis() - 15 * 60 * 1000

        return encoded.lineSequence()
            .mapNotNull { line -> decode(line) }
            .filter { it.timestamp >= fifteenMinsAgo }
            .toList()
    }

    override fun saveReport(report: CrowdReport) {
        val reports = loadReports() + report
        preferences.edit()
            .putString(KEY_REPORTS, reports.joinToString(separator = "\n") { encode(it) })
            .apply()
    }

    private fun encode(report: CrowdReport): String = listOf(
        report.routeId,
        report.status.name,
        report.timestamp.toString(),
        report.note.replace("|", "/").replace("\n", " ")
    ).joinToString("|")

    private fun decode(line: String): CrowdReport? {
        val parts = line.split("|", limit = 4)
        if (parts.size < 4) return null

        val status = runCatching { RouteStatus.valueOf(parts[1]) }.getOrNull() ?: return null
        val timestamp = parts[2].toLongOrNull() ?: return null
        return CrowdReport(
            routeId = parts[0],
            status = status,
            timestamp = timestamp,
            note = parts[3]
        )
    }

    private companion object {
        const val KEY_REPORTS = "reports"
    }
}
