package com.jobtracker.android.feature.applications

import com.jobtracker.android.core.data.db.ApplicationDao
import com.jobtracker.android.core.data.db.ApplicationEntity
import com.jobtracker.android.core.domain.model.Application
import com.jobtracker.android.core.domain.model.AppStatus
import com.jobtracker.android.core.domain.model.CreateApplicationRequest
import com.jobtracker.android.core.domain.model.UpdateApplicationRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ApplicationsRepository(
    private val api: ApplicationsApi,
    private val dao: ApplicationDao,
) {

    data class Filters(
        val status: AppStatus? = null,
        val search: String = "",
        val month: Int? = null,
        val year: Int? = null,
        val gotCall: Boolean? = null,
        val sortBy: String = "appliedAt",
    )

    data class Page(
        val items: List<Application>,
        val page: Int,
        val totalPages: Int,
        val totalElements: Long,
    )

    fun observeAll(): Flow<List<Application>> = dao.observeAll()
        .map { entities -> entities.map(ApplicationEntity::toDomain) }
        .distinctUntilChanged()

    fun observeById(id: String): Flow<Application?> = dao.observeById(id)
        .map { it?.toDomain() }
        .distinctUntilChanged()

    suspend fun fetchPage(filters: Filters, page: Int, limit: Int = 20): Page {
        val response = api.list(
            status = filters.status?.name,
            search = filters.search.takeIf { it.isNotBlank() },
            month = filters.month,
            year = filters.year,
            gotCall = filters.gotCall,
            sortBy = filters.sortBy,
            page = page,
            limit = limit,
        )
        dao.upsertAll(response.items.map(ApplicationEntity::from))
        return Page(
            items = response.items,
            page = response.page,
            totalPages = response.totalPages,
            totalElements = response.totalElements,
        )
    }

    suspend fun get(id: String): Application {
        val remote = api.get(id)
        dao.upsert(ApplicationEntity.from(remote))
        return remote
    }

    suspend fun create(request: CreateApplicationRequest): Application {
        val created = api.create(request)
        dao.upsert(ApplicationEntity.from(created))
        return created
    }

    suspend fun update(id: String, request: UpdateApplicationRequest): Application {
        val updated = api.update(id, request)
        dao.upsert(ApplicationEntity.from(updated))
        return updated
    }

    suspend fun delete(id: String) {
        api.delete(id)
        dao.deleteById(id)
    }

    suspend fun clearCache() = dao.clear()
}
