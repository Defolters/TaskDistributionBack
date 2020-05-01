package io.defolters.repository.interfaces

import io.defolters.routes.ScheduleData

interface ScheduleRepository : RepositoryInterface {

    suspend fun getSchedule(): ScheduleData?

}