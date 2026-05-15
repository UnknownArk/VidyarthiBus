package com.vidyarthibus.model

enum class DataSource(val label: String) {
    LIVE_REPORTS("Live reports"),
    PREDICTION("Prediction"),
    HISTORICAL("Historical"),
    NONE("No data")
}
