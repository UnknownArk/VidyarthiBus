package com.vidyarthibus.data

import com.vidyarthibus.model.CrowdReport

interface ReportStore {
    fun loadReports(): List<CrowdReport>
    fun saveReport(report: CrowdReport)
}

class InMemoryReportStore(initialReports: List<CrowdReport> = emptyList()) : ReportStore {
    private val reports = initialReports.toMutableList()

    override fun loadReports(): List<CrowdReport> = reports.toList()

    override fun saveReport(report: CrowdReport) {
        reports += report
    }
}
