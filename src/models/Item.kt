package io.defolters.models

data class Item(
    val id: Int,
    val orderId: Int,
    val title: String,
    val info: String,
    val price: Double,
    val isReady: Boolean,
    val color: String
)