package com.vidyarthibus.data

import android.location.Location
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.vidyarthibus.model.BusRoute
import com.vidyarthibus.model.CrowdReport
import com.vidyarthibus.model.DataSource
import com.vidyarthibus.model.RouteStatus

class FirebaseBusService(
    private val fallbackRoutes: () -> List<BusRoute>
) {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private var routesListener: ValueEventListener? = null

    fun start(
        onRoutesChanged: (List<BusRoute>) -> Unit,
        onStatus: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        onStatus("Connecting to Firebase...")
        auth.signInAnonymously()
            .addOnSuccessListener {
                onStatus("Connected. Loading live routes...")
                seedRoutesIfEmpty(onStatus, onError)
                listenRoutes(onRoutesChanged, onError)
            }
            .addOnFailureListener { error ->
                onError("Firebase sign-in failed: ${error.localizedMessage ?: "Unknown error"}")
            }
    }

    fun stop() {
        routesListener?.let { database.child(ROUTES).removeEventListener(it) }
        routesListener = null
    }

    fun submitReport(
        report: CrowdReport,
        location: Location?,
        verified: Boolean,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: "anonymous"
        val reportRef = database.child(REPORTS).push()
        val payload = mapOf(
            "routeId" to report.routeId,
            "status" to report.status.name,
            "note" to report.note,
            "timestamp" to report.timestamp,
            "userId" to userId,
            "verified" to verified,
            "lat" to location?.latitude,
            "lng" to location?.longitude
        )

        reportRef.setValue(payload)
            .addOnSuccessListener {
                updateRouteAfterReport(report, onComplete, onError)
            }
            .addOnFailureListener { error ->
                onError("Report upload failed: ${error.localizedMessage ?: "Unknown error"}")
            }
    }

    private fun seedRoutesIfEmpty(onStatus: (String) -> Unit, onError: (String) -> Unit) {
        database.child(ROUTES).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    val seed = fallbackRoutes().associate { route -> route.id to route.toFirebaseMap() }
                    database.child(ROUTES).setValue(seed)
                        .addOnSuccessListener { onStatus("Sample routes added to Firebase.") }
                        .addOnFailureListener { error -> onError("Could not seed routes: ${error.localizedMessage ?: "Unknown error"}") }
                }
            }
            .addOnFailureListener { error -> onError("Could not check routes: ${error.localizedMessage ?: "Unknown error"}") }
    }

    private fun listenRoutes(onRoutesChanged: (List<BusRoute>) -> Unit, onError: (String) -> Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val routes = snapshot.children.mapNotNull { child -> child.toBusRoute() }
                if (routes.isNotEmpty()) onRoutesChanged(routes)
            }

            override fun onCancelled(error: DatabaseError) {
                onError("Live route listener failed: ${error.message}")
            }
        }
        routesListener = listener
        database.child(ROUTES).addValueEventListener(listener)
    }

    private fun updateRouteAfterReport(report: CrowdReport, onComplete: () -> Unit, onError: (String) -> Unit) {
        val routeRef = database.child(ROUTES).child(report.routeId)
        routeRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentCount = currentData.child("reportCount").getValue(Int::class.java) ?: 0
                val currentConfidence = currentData.child("confidence").getValue(Int::class.java) ?: 50
                
                currentData.child("status").value = report.status.name
                currentData.child("lastUpdateMinutesAgo").value = 0
                currentData.child("reportCount").value = currentCount + 1
                currentData.child("confidence").value = (currentConfidence + 4).coerceAtMost(96)
                currentData.child("dataSource").value = DataSource.LIVE_REPORTS.name
                currentData.child("lastReportTimestamp").value = report.timestamp
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    onError("Route update failed: ${error.message}")
                } else {
                    onComplete()
                }
            }
        })
    }

    private fun BusRoute.toFirebaseMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "number" to number,
        "departureTime" to departureTime,
        "arrivalTime" to arrivalTime,
        "stops" to stops,
        "capacity" to capacity,
        "status" to status.name,
        "lastUpdateMinutesAgo" to lastUpdateMinutesAgo,
        "reportCount" to reportCount,
        "confidence" to confidence,
        "dataSource" to dataSource.name,
        "nextHourPrediction" to nextHourPrediction.name,
        "predictionReason" to predictionReason,
        "geofenceCenterLat" to geofenceCenterLat,
        "geofenceCenterLng" to geofenceCenterLng,
        "geofenceRadiusMeters" to geofenceRadiusMeters,
        "lastReportTimestamp" to lastReportTimestamp
    )

    private fun DataSnapshot.toBusRoute(): BusRoute? {
        val id = child("id").getValue(String::class.java) ?: key ?: return null
        val status = enumValue<RouteStatus>(child("status").getValue(String::class.java), RouteStatus.UNKNOWN)
        val dataSource = enumValue<DataSource>(child("dataSource").getValue(String::class.java), DataSource.NONE)
        val nextPrediction = enumValue<RouteStatus>(child("nextHourPrediction").getValue(String::class.java), RouteStatus.UNKNOWN)
        val stops = child("stops").children.mapNotNull { it.getValue(String::class.java) }

        val lastReportTimestamp = child("lastReportTimestamp").getValue(Long::class.java) ?: 0L
        val fifteenMinsAgo = System.currentTimeMillis() - 15 * 60 * 1000

        val (effectiveStatus, effectiveCount, effectiveConfidence, effectiveLastUpdate) = 
            if (lastReportTimestamp > 0 && lastReportTimestamp < fifteenMinsAgo) {
                listOf(RouteStatus.UNKNOWN, 0, 0, 15)
            } else {
                listOf(
                    status, 
                    child("reportCount").getValue(Int::class.java) ?: 0,
                    child("confidence").getValue(Int::class.java) ?: 0,
                    child("lastUpdateMinutesAgo").getValue(Int::class.java) ?: 0
                )
            }

        return BusRoute(
            id = id,
            name = child("name").getValue(String::class.java).orEmpty(),
            number = child("number").getValue(String::class.java).orEmpty(),
            departureTime = child("departureTime").getValue(String::class.java).orEmpty(),
            arrivalTime = child("arrivalTime").getValue(String::class.java).orEmpty(),
            stops = stops,
            capacity = child("capacity").getValue(Int::class.java) ?: 0,
            status = effectiveStatus as RouteStatus,
            lastUpdateMinutesAgo = effectiveLastUpdate as Int,
            reportCount = effectiveCount as Int,
            confidence = effectiveConfidence as Int,
            dataSource = dataSource,
            nextHourPrediction = nextPrediction,
            predictionReason = child("predictionReason").getValue(String::class.java).orEmpty(),
            geofenceCenterLat = child("geofenceCenterLat").getValue(Double::class.java) ?: 0.0,
            geofenceCenterLng = child("geofenceCenterLng").getValue(Double::class.java) ?: 0.0,
            geofenceRadiusMeters = child("geofenceRadiusMeters").getValue(Int::class.java) ?: 0,
            lastReportTimestamp = lastReportTimestamp
        )
    }

    private inline fun <reified T : Enum<T>> enumValue(raw: String?, fallback: T): T =
        runCatching { if (raw == null) fallback else enumValueOf<T>(raw) }.getOrDefault(fallback)

    private companion object {
        const val ROUTES = "routes"
        const val REPORTS = "reports"
    }
}
