package com.vidyarthibus.data

import com.vidyarthibus.model.CrowdReport
import com.vidyarthibus.model.RouteStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FakeBusRepositoryTest {
    // @Test
    // fun submitReportUpdatesRouteAndPersistsReport() {
    //     val store = InMemoryReportStore()
    //     val repository = FakeBusRepository(store)
    //
    //     val updated = repository.submitReport(
    //         CrowdReport(
    //             routeId = "r42",
    //             status = RouteStatus.FULL,
    //             note = "test report"
    //         )
    //     )
    //
    //     assertNotNull(updated)
    //     assertEquals(RouteStatus.FULL, updated.status)
    //     assertEquals(1, store.loadReports().size)
    // }

    // @Test
    // fun savedReportsAreAppliedWhenRepositoryStarts() {
    //     val store = InMemoryReportStore(
    //         listOf(
    //             CrowdReport(
    //                 routeId = "r18",
    //                 status = RouteStatus.FULL,
    //                 note = "previous session"
    //             )
    //         )
    //     )
    //
    //     val repository = FakeBusRepository(store)
    //
    //     assertEquals(RouteStatus.FULL, repository.getRoute("r18")?.status)
    // }
}
