package io.defolters.repository.interfaces

import io.defolters.models.Item

interface ItemRepository : RepositoryInterface {
    suspend fun addItem(
        orderId: Int,
        title: String,
        info: String,
        price: Float
    ): Item?

    suspend fun deleteItem(id: Int)
    suspend fun getItems(): List<Item>
    suspend fun getItems(orderId: Int): List<Item>
}