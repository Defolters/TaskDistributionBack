package io.defolters.models

data class Order(
    val id: Int,
    val customerName: String,
    val customerEmail: String,
    val price: Double,
    val createdAt: Long
)
