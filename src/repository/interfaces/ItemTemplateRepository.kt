package io.defolters.repository.interfaces

import io.defolters.models.ItemTemplate

interface ItemTemplateRepository : RepositoryInterface {
    suspend fun addItemTemplate(title: String): ItemTemplate?
    suspend fun deleteItemTemplate(id: Int)
    suspend fun getItemTemplates(): List<ItemTemplate>
    suspend fun findItemTemplate(itemTemplateId: Int?): ItemTemplate?
}