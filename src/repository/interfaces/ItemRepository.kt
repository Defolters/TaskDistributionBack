package io.defolters.repository.interfaces

import io.defolters.models.Item

interface ItemRepository : RepositoryInterface {

    suspend fun deleteItem(id: Int)
    suspend fun getItems(): List<Item>
    suspend fun getItems(orderId: Int): List<Item>
}