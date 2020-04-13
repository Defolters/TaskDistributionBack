package io.defolters.repository.interfaces

import io.defolters.models.Order

interface OrderRepository : RepositoryInterface {
    suspend fun addOrder(
        customerName: String,
        customerEmail: String,
        price: Float,
        createdAt: Long
    ): Order?

    suspend fun deleteOrder(id: Int)
    suspend fun getOrders(): List<Order>
}