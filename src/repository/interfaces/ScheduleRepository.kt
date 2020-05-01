package io.defolters.repository.interfaces

interface ScheduleRepository : RepositoryInterface {

    suspend fun getSchedule()

}