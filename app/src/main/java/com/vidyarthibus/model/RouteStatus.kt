package com.vidyarthibus.model

enum class RouteStatus(
    val label: String,
    val summary: String,
    val color: Int
) {
    EMPTY("Empty", "Seats available", 0xFF2E7D32.toInt()),
    SEATED("Seated", "Standing likely", 0xFFF9A825.toInt()),
    FULL("Full", "Try an alternative", 0xFFC62828.toInt()),
    UNKNOWN("Unknown", "Waiting for reports", 0xFF757575.toInt())
}
