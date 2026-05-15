package com.vidyarthibus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.vidyarthibus.model.BusRoute
import com.vidyarthibus.model.RouteStatus

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var selectedRouteId: String? = null
    private var pendingReportStatus: RouteStatus? = null
    private var currentTab: Int = 0
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel.routes.observe(this) { routes ->
            if (routes != null) refreshCurrentScreen()
        }

        viewModel.firebaseStatus.observe(this) { status ->
            if (status != null) refreshCurrentScreen()
        }

        viewModel.toastMessage.observe(this) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REPORT_LOCATION_PERMISSION) {
            val status = pendingReportStatus
            pendingReportStatus = null
            if (status != null) {
                if (hasLocationPermission()) {
                    viewModel.submitReport(selectedRouteId!!, status, lastKnownLocation())
                } else {
                    Toast.makeText(this, "Location permission is required for accurate reporting.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showRoutes() {
        val routes = viewModel.routes.value ?: emptyList()
        val filteredRoutes = routes
            .filter { it.name.contains(searchQuery, ignoreCase = true) || it.number.contains(searchQuery, ignoreCase = true) }

        val content = vertical(padding = 16)
        content.addView(label("Choose your bus", 24, bold = true, color = 0xFF123047.toInt()))
        content.addView(label("Live crowd status from students on board.", 14, color = 0xFF666666.toInt()))
        content.addView(label(viewModel.firebaseStatus.value ?: "", 13, color = 0xFF1E88E5.toInt()), matchWrap(top = 4))

        val search = EditText(this).apply {
            hint = "Search route or number"
            setSingleLine(true)
            setText(searchQuery)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = getDrawable(android.R.drawable.edit_text)
        }
        content.addView(search, matchWrap(top = 16))

        if (searchQuery.isNotEmpty()) {
            search.requestFocus()
            search.setSelection(searchQuery.length)
        }

        val list = vertical()
        content.addView(list, matchWrap(top = 16))

        fun renderList(filtered: List<BusRoute>) {
            list.removeAllViews()
            filtered.forEach { route -> list.addView(routeCard(route), matchWrap(bottom = 12)) }
            if (filtered.isEmpty()) {
                list.addView(label("No routes found", 16, color = 0xFF666666.toInt()), matchWrap(top = 16))
            }
        }

        renderList(filteredRoutes)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty()
                val currentRoutes = viewModel.routes.value ?: emptyList()
                val filtered = currentRoutes.filter {
                    it.name.contains(searchQuery, ignoreCase = true) || it.number.contains(searchQuery, ignoreCase = true)
                }
                renderList(filtered)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        setScreen("Vidyarthi Bus", content, selectedTab = 0)
    }

    private fun showDashboard(routeId: String) {
        selectedRouteId = routeId
        val routes = viewModel.routes.value ?: emptyList()
        val route = routes.firstOrNull { it.id == routeId } ?: return showRoutes()
        
        val content = vertical(padding = 16)

        content.addView(label("Route ${route.number}", 14, color = 0xFF666666.toInt()))
        content.addView(label(route.name, 28, bold = true, color = 0xFF123047.toInt()))
        content.addView(label("${route.departureTime} to ${route.arrivalTime}", 14, color = 0xFF666666.toInt()))
        content.addView(label(viewModel.firebaseStatus.value ?: "", 13, color = 0xFF1E88E5.toInt()), matchWrap(top = 4))

        content.addView(statusPanel(route), matchWrap(top = 20))
        content.addView(stopsPanel(route), matchWrap(top = 16))

        // Historical Trends Chart per route
        content.addView(label("Recent Trends", 18, bold = true, color = 0xFF123047.toInt()), matchWrap(top = 24))
        content.addView(historicalTrendsChart(route.id), matchWrap(top = 12))

        if (route.status == RouteStatus.FULL) content.addView(alternativePanel(), matchWrap(top = 16))

        val report = primaryButton("Report Crowd").apply { setOnClickListener { showReportDialog(route.id) } }
        content.addView(report, matchWrap(top = 24, bottom = 24))

        setScreen("Live Dashboard", content, selectedTab = 1)
    }

    private fun showAnalytics() {
        val content = vertical(padding = 16)
        content.addView(label("Analytics", 24, bold = true, color = 0xFF123047.toInt()))
        content.addView(label("Reports sync through Firebase Realtime Database when connected.", 14, color = 0xFF666666.toInt()))
        content.addView(label(viewModel.firebaseStatus.value ?: "", 13, color = 0xFF1E88E5.toInt()), matchWrap(top = 4))

        val routes = viewModel.routes.value ?: emptyList()
        val totalReports = routes.sumOf { it.reportCount }
        val averageConfidence = if (routes.isNotEmpty()) routes.map { it.confidence }.average().toInt() else 0
        
        content.addView(metricRow("Reports today", totalReports.toString()), matchWrap(top = 20))
        content.addView(metricRow("Average confidence", "$averageConfidence%"), matchWrap(top = 12))
        content.addView(metricRow("Routes monitored", routes.size.toString()), matchWrap(top = 12))

        content.addView(label("Route health", 18, bold = true, color = 0xFF123047.toInt()), matchWrap(top = 24))
        routes.forEach { route -> content.addView(compactRouteMetric(route), matchWrap(top = 12)) }

        setScreen("Analytics", content, selectedTab = 2)
    }

    private fun historicalTrendsChart(routeId: String): View {
        val panel = horizontal(padding = 16)
        panel.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        panel.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180))
        
        val history = viewModel.getRouteHistory(routeId)
        val maxReports = maxOf(history.historyPoints.maxOfOrNull { it.reportCount } ?: 10, 10)

        history.historyPoints.forEachIndexed { index, point ->
            val barContainer = vertical()
            barContainer.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            val barParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(dp(4), 0, dp(4), 0)
            }
            barContainer.layoutParams = barParams

            val barHeightPercent = point.reportCount.toFloat() / maxReports.toFloat()
            val bar = View(this).apply {
                setBackgroundColor(0xFF1E88E5.toInt())
                layoutParams = LinearLayout.LayoutParams(dp(24), (dp(130) * barHeightPercent).toInt()).apply {
                    setMargins(0, dp(4), 0, dp(4))
                }
            }

            val hoursAgo = history.historyPoints.size - index - 1
            val timeLabel = if (hoursAgo == 0) "Now" else "-${hoursAgo}h"

            barContainer.addView(label("${point.reportCount}", 11, bold = true, color = 0xFF444444.toInt()), matchWrap(bottom = 2).apply { gravity = Gravity.CENTER_HORIZONTAL })
            barContainer.addView(bar)
            barContainer.addView(label(timeLabel, 11, color = 0xFF888888.toInt()), matchWrap(top = 2).apply { gravity = Gravity.CENTER_HORIZONTAL })

            panel.addView(barContainer)
        }

        return cardWrapper(panel)
    }

    private fun showReportDialog(routeId: String) {
        val bottomSheet = BottomSheetDialog(this)
        val content = vertical(padding = 24)
        
        content.addView(label("How crowded is the bus?", 22, bold = true, color = 0xFF123047.toInt()), matchWrap(bottom = 20))
        
        val options = listOf(
            Triple(RouteStatus.EMPTY, "🛋️", RouteStatus.EMPTY.summary),
            Triple(RouteStatus.SEATED, "🧍", RouteStatus.SEATED.summary),
            Triple(RouteStatus.FULL, "🚫", RouteStatus.FULL.summary)
        )
        
        options.forEach { (status, emoji, desc) ->
            val inner = horizontal(padding = 16)
            inner.gravity = Gravity.CENTER_VERTICAL
            inner.addView(label(emoji, 32), LinearLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.WRAP_CONTENT))
            
            val textLayout = vertical()
            textLayout.addView(label(status.label, 18, bold = true, color = status.color))
            textLayout.addView(label(desc, 14, color = 0xFF666666.toInt()))
            inner.addView(textLayout, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            
            val card = MaterialCardView(this).apply {
                radius = dp(16).toFloat()
                cardElevation = dp(2).toFloat()
                strokeWidth = dp(2)
                strokeColor = status.color
                setCardBackgroundColor(0xFFFAFAFA.toInt())
                isClickable = true
                isFocusable = true
                val outValue = TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                foreground = getDrawable(outValue.resourceId)
                
                addView(inner)
                
                setOnClickListener {
                    selectedRouteId = routeId
                    bottomSheet.dismiss()
                    submitReportWithValidation(status)
                }
            }
            content.addView(card, matchWrap(bottom = 16))
        }
        
        bottomSheet.setContentView(content)
        bottomSheet.show()
    }

    private fun submitReportWithValidation(status: RouteStatus) {
        if (!hasLocationPermission()) {
            pendingReportStatus = status
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REPORT_LOCATION_PERMISSION
            )
            return
        }

        val location = lastKnownLocation()
        viewModel.submitReport(selectedRouteId!!, status, location)
    }

    private fun refreshCurrentScreen() {
        val routes = viewModel.routes.value ?: emptyList()
        when (currentTab) {
            1 -> {
                val targetId = selectedRouteId ?: routes.firstOrNull()?.id
                if (targetId != null) showDashboard(targetId) else showRoutes()
            }
            2 -> showAnalytics()
            else -> showRoutes()
        }
    }

    private fun hasLocationPermission(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun lastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null
        val locationManager = getSystemService(LOCATION_SERVICE) as? LocationManager ?: return null
        return runCatching {
            locationManager.getProviders(true)
                .mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
                .maxByOrNull { it.time }
        }.getOrNull()
    }

    private fun setScreen(title: String, content: View, selectedTab: Int) {
        currentTab = selectedTab
        val root = vertical()
        root.setBackgroundColor(0xFFF0F4F8.toInt())
        root.addView(topBar(title), ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)))
        val scroll = ScrollView(this).apply { addView(content) }
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottomNav(selectedTab), ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)))
        setContentView(root)
    }

    private fun topBar(title: String): View {
        val bar = vertical()
        bar.setBackgroundColor(0xFF123047.toInt())
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(16), 0, dp(16), 0)
        bar.addView(label(title, 22, bold = true, color = 0xFFFFFFFF.toInt()))
        return bar
    }

    private fun bottomNav(selected: Int): View {
        val row = horizontal(padding = 8)
        row.setBackgroundColor(0xFFFFFFFF.toInt())
        val routes = viewModel.routes.value ?: emptyList()
        val dashboardTarget = selectedRouteId ?: routes.firstOrNull()?.id ?: ""
        row.addView(navButton("Routes", selected == 0) { showRoutes() }, weightParams())
        if (dashboardTarget.isNotEmpty()) {
            row.addView(navButton("Dashboard", selected == 1) { showDashboard(dashboardTarget) }, weightParams())
        }
        row.addView(navButton("Analytics", selected == 2) { showAnalytics() }, weightParams())
        return row
    }

    private fun cardWrapper(inner: View, onClick: (() -> Unit)? = null): MaterialCardView {
        return MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(4).toFloat()
            setCardBackgroundColor(0xFFFFFFFF.toInt())
            strokeWidth = 0
            
            if (onClick != null) {
                isClickable = true
                isFocusable = true
                val outValue = TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                foreground = getDrawable(outValue.resourceId)
                setOnClickListener { onClick() }
            }
            addView(inner)
        }
    }

    private fun routeCard(route: BusRoute): View {
        val inner = vertical(padding = 16)
        val top = horizontal()
        top.gravity = Gravity.CENTER_VERTICAL
        top.addView(label("Route ${route.number}", 18, bold = true, color = 0xFF123047.toInt()), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(statusChip(route.status))
        inner.addView(top)
        inner.addView(label(route.name, 16, bold = true), matchWrap(top = 8))
        inner.addView(label("${route.departureTime} arrival ${route.arrivalTime}", 14, color = 0xFF666666.toInt()), matchWrap(top = 2))
        inner.addView(label("${route.reportCount} reports - ${route.confidence}% confidence", 13, color = 0xFF888888.toInt()), matchWrap(top = 8))
        return cardWrapper(inner) { showDashboard(route.id) }
    }

    private fun statusPanel(route: BusRoute): View {
        val inner = vertical(padding = 16)
        inner.addView(label("Crowd meter", 14, bold = true, color = 0xFF666666.toInt()))
        inner.addView(CrowdMeterView(this).apply { status = route.status }, matchWrap(top = 12))

        val statusRow = horizontal()
        statusRow.gravity = Gravity.CENTER_VERTICAL
        statusRow.addView(label(route.status.label, 36, bold = true, color = route.status.color), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        statusRow.addView(label(route.status.summary, 16, bold = true, color = 0xFF444444.toInt()))
        inner.addView(statusRow, matchWrap(top = 12))

        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = route.confidence
        }
        inner.addView(label("Report confidence", 13, bold = true, color = 0xFF888888.toInt()), matchWrap(top = 20))
        inner.addView(progress, matchWrap(top = 8))
        inner.addView(label("${route.confidence}% confidence from ${route.reportCount} reports", 13, color = 0xFF888888.toInt()), matchWrap(top = 8))
        inner.addView(label("Geofence radius: ${route.geofenceRadiusMeters}m around sample route center", 13, color = 0xFF888888.toInt()), matchWrap(top = 8))
        inner.addView(label("Next hour: ${route.nextHourPrediction.label}. ${route.predictionReason}", 13, color = 0xFF888888.toInt()), matchWrap(top = 12))
        return cardWrapper(inner)
    }

    private fun stopsPanel(route: BusRoute): View {
        val inner = vertical(padding = 16)
        inner.addView(label("Stops", 18, bold = true, color = 0xFF123047.toInt()))
        route.stops.forEachIndexed { index, stop ->
            inner.addView(label("${index + 1}. $stop", 15, color = 0xFF444444.toInt()), matchWrap(top = 8))
        }
        return cardWrapper(inner)
    }

    private fun alternativePanel(): View {
        val inner = vertical(padding = 16)
        inner.addView(label("Alternative options", 18, bold = true, color = 0xFFC62828.toInt()))
        inner.addView(label("Bus is full. Consider sharing an auto or booking a cab from the contacts below:", 14, color = 0xFF444444.toInt()), matchWrap(top = 8))
        
        val contacts = listOf(
            "Main Gate Auto Stand" to "+91 98765 43210",
            "City Cab Service" to "+91 99887 76655",
            "Student Rideshare" to "Join WhatsApp"
        )
        
        val contactsList = vertical(padding = 8)
        contacts.forEach { (name, contact) ->
            val contactRow = horizontal(padding = 12)
            contactRow.gravity = Gravity.CENTER_VERTICAL
            contactRow.addView(label(name, 15, bold = true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            contactRow.addView(label(contact, 14, bold = true, color = 0xFF1E88E5.toInt()))
            
            val rowCard = MaterialCardView(this).apply {
                radius = dp(8).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(0xFFF0F4F8.toInt())
                strokeWidth = 0
                isClickable = true
                isFocusable = true
                val outValue = TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                foreground = getDrawable(outValue.resourceId)
                addView(contactRow)
                setOnClickListener {
                    if (contact.startsWith("+")) {
                        startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$contact") })
                    }
                }
            }
            contactsList.addView(rowCard, matchWrap(top = 8))
        }
        
        inner.addView(contactsList, matchWrap(top = 8))
        return cardWrapper(inner)
    }

    private fun metricRow(name: String, value: String): View {
        val inner = horizontal(padding = 16)
        inner.gravity = Gravity.CENTER_VERTICAL
        inner.addView(label(name, 16, color = 0xFF444444.toInt()), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        inner.addView(label(value, 24, bold = true, color = 0xFF1E88E5.toInt()))
        return cardWrapper(inner)
    }

    private fun compactRouteMetric(route: BusRoute): View {
        val inner = horizontal(padding = 16)
        inner.gravity = Gravity.CENTER_VERTICAL
        inner.addView(label("${route.number} ${route.name}", 15, bold = true, color = 0xFF123047.toInt()), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        inner.addView(label("${route.status.label} / ${route.confidence}%", 14, bold = true, color = route.status.color))
        return cardWrapper(inner)
    }

    private fun statusChip(status: RouteStatus): View =
        MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = 0f
            setCardBackgroundColor((status.color and 0x00FFFFFF) or 0x22000000) // 15% opacity of the color
            strokeWidth = 0
            val pad = dp(10)
            val lbl = label(status.label, 13, bold = true, color = status.color)
            lbl.setPadding(pad, dp(4), pad, dp(4))
            addView(lbl)
        }

    private fun primaryButton(text: String): MaterialButton =
        MaterialButton(this).apply {
            this.text = text
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1E88E5.toInt())
            cornerRadius = dp(24)
            minHeight = dp(56)
            textSize = 16f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
        }

    private fun navButton(text: String, selected: Boolean, onClick: () -> Unit): MaterialButton =
        MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            this.text = text
            textSize = 13f
            isAllCaps = false
            typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setTextColor(if (selected) 0xFF1E88E5.toInt() else 0xFF666666.toInt())
            setOnClickListener { onClick() }
        }

    private fun label(text: String, sp: Int, bold: Boolean = false, color: Int = 0xFF212121.toInt()): TextView =
        TextView(this).apply {
            this.text = text
            textSize = sp.toFloat()
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = true
        }

    private fun vertical(padding: Int = 0): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            if (padding > 0) setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
        }

    private fun horizontal(padding: Int = 0): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            if (padding > 0) setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
        }

    private fun matchWrap(top: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dp(top), 0, dp(bottom))
        }

    private fun weightParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
            setMargins(dp(4), dp(4), dp(4), dp(4))
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val REPORT_LOCATION_PERMISSION = 2001
    }
}
