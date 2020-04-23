package io.defolters.repository.interfaces

import io.defolters.models.Order
import io.defolters.routes.OrderJSON

interface OrderRepository : RepositoryInterface {

    suspend fun addOrder(orderJSON: OrderJSON, time: Long): Order?
    suspend fun addOrder(
        customerName: String,
        customerEmail: String,
        price: Double,
        createdAt: Long
    ): Order?

    suspend fun deleteOrder(id: Int)
    suspend fun getOrders(): List<Order>
    suspend fun findOrder(id: Int): Order?
}