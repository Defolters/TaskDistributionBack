package io.defolters.optimization

import com.google.ortools.sat.*
import com.skaggsm.ortools.OrToolsHelper
import io.defolters.models.TaskStatus
import io.defolters.routes.ScheduleTaskData
import org.joda.time.DateTime
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


data class TaskType(val start: IntVar, val end: IntVar, val interval: IntervalVar)
data class AssignedTaskType(
    val start: Long,
    val job: Int,
    val index: Int,
    val duration: Int,
    val title: String,
    val color: String,
    val item_id: Int,
    val taskStatus: TaskStatus = TaskStatus.NEW,
    val taskDependency: Int? = null
)

data class ItemData(val tasks: List<TaskData>)
data class TaskData(
    val task_id: Int,
    val item_id: Int,
    val workerType: Int,
    val timeToComplete: Int,
    val taskDependency: Int? = null,
    val isAdditional: Boolean = false,
    val title: String = "",
    val color: String = "",
    val status: TaskStatus = TaskStatus.NEW
)

object TaskOptimizer {

    fun optimizeTest() {
        val orders3 = listOf(
            ItemData(
                listOf(
                    TaskData(
                        task_id = 1, item_id = 0, workerType = 0, timeToComplete = 10,
                        taskDependency = null, isAdditional = false
                    ),
                    TaskData(
                        task_id = 2, item_id = 0, workerType = 1, timeToComplete = 10,
                        taskDependency = 1, isAdditional = false
                    ),
                    TaskData(
                        task_id = 3, item_id = 0, workerType = 2, timeToComplete = 10,
                        taskDependency = 1, isAdditional = false
                    ),
                    TaskData(
                        task_id = 4, item_id = 0, workerType = 1, timeToComplete = 10,
                        taskDependency = 2, isAdditional = false
                    ),
                    TaskData(
                        task_id = 5, item_id = 0, workerType = 2, timeToComplete = 10,
                        taskDependency = 4, isAdditional = false
                    ),//
                    TaskData(
                        task_id = 6, item_id = 0, workerType = 0, timeToComplete = 10,
                        taskDependency = null, isAdditional = true
                    ),
                    TaskData(
                        task_id = 7, item_id = 0, workerType = 1, timeToComplete = 10,
                        taskDependency = null, isAdditional = true
                    ),
                    TaskData(
                        task_id = 8, item_id = 0, workerType = 2, timeToComplete = 10,
                        taskDependency = null, isAdditional = true
                    ),
                    TaskData(
                        task_id = 9, item_id = 0, workerType = 0, timeToComplete = 10,
                        taskDependency = 8, isAdditional = true
                    ),
                    TaskData(
                        task_id = 10, item_id = 0, workerType = 1, timeToComplete = 10,
                        taskDependency = 8, isAdditional = true
                    )
                )
            ),
            ItemData(
                listOf(
                    TaskData(
                        task_id = 1, item_id = 1, workerType = 0, timeToComplete = 10,
                        taskDependency = null, isAdditional = false
                    ),
                    TaskData(
                        task_id = 2, item_id = 1, workerType = 1, timeToComplete = 10,
                        taskDependency = 1, isAdditional = false
                    ),
                    TaskData(
                        task_id = 3, item_id = 1, workerType = 2, timeToComplete = 10,
                        taskDependency = 1, isAdditional = false
                    ),
                    TaskData(
                        task_id = 4, item_id = 1, workerType = 1, timeToComplete = 10,
                        taskDependency = 2, isAdditional = false
                    ),
                    TaskData(
                        task_id = 5, item_id = 1, workerType = 2, timeToComplete = 10,
                        taskDependency = 4, isAdditional = false
                    ),//
                    TaskData(
                        task_id = 6, item_id = 1, workerType = 0, timeToComplete = 10,
                        taskDependency = null, isAdditional = true
                    ),
                    TaskData(
                        task_id = 7, item_id = 1, workerType = 1, timeToComplete = 10,
                        taskDependency = null, isAdditional = true
                    ),
                    TaskData(
                        task_id = 8, item_id = 1, workerType = 2, timeToComplete = 10,
                        taskDependency = null, isAdditional = true
                    ),
                    TaskData(
                        task_id = 9, item_id = 1, workerType = 0, timeToComplete = 10,
                        taskDependency = 8, isAdditional = true
                    ),
                    TaskData(
                        task_id = 10, item_id = 1, workerType = 1, timeToComplete = 10,
                        taskDependency = 8, isAdditional = true
                    )
                )
            )
        )
//        optimizeFourth(orders3)
    }

    fun optimizeThird(items: List<ItemData>): List<ScheduleTaskData> {
        OrToolsHelper.loadLibrary()
        // Create the model.
        val model = CpModel()

        val workerTypesSet = items.map { it.tasks.map { it.workerType } }.flatten().toSortedSet()

        // Computes horizon dynamically as the sum of all durations.
        val horizon = items.sumBy { it.tasks.sumBy { it.timeToComplete } }.toLong()

        // Creates job intervals and add to the corresponding machine lists.
        val allTasks = mutableMapOf<Pair<Int, Int>, TaskType>()
        val workerTypeToIntervals = mutableMapOf<Int, MutableList<IntervalVar>>()

        items.forEachIndexed { order_id, order ->
            order.tasks.forEach { task ->
                val task_id = task.task_id
                println(" task id $task_id")
                val machine = task.workerType
                val duration = task.timeToComplete.toLong()
                val suffix = "_${order_id}_$task_id"
                val startVar = model.newIntVar(0, horizon, "start" + suffix)
                val endVar = model.newIntVar(0, horizon, "end" + suffix)
                val intervalVar = model.newIntervalVar(startVar, duration, endVar, "interval" + suffix)

                allTasks[Pair(order_id, task_id)] = TaskType(start = startVar, end = endVar, interval = intervalVar)
                if (workerTypeToIntervals[machine] == null) workerTypeToIntervals[machine] = mutableListOf()
                workerTypeToIntervals[machine]!!.add(intervalVar)
            }
        }
        // Create and add disjunctive constraints.
        workerTypesSet.forEach { machine ->
            model.addNoOverlap(workerTypeToIntervals[machine]!!.toTypedArray())
        }
        // Tasks in one order should not overlap
        items.forEachIndexed { order_id, order ->
            val intervals = mutableListOf<IntervalVar>()
            order.tasks.forEach { task ->
                intervals.add(
                    allTasks[Pair(order_id, task.task_id)]!!.interval
                )
            }
            model.addNoOverlap(intervals.toTypedArray())
        }
        // Precedences inside a order.
        items.forEachIndexed { order_id, order ->
            val notAdditionalTasks = order.tasks.filter { !it.isAdditional }
            val additionalTasks = order.tasks.filter { it.isAdditional }

            val setOfTaskWithDependentTasks = mutableSetOf<Int>()

            notAdditionalTasks.forEach { task ->
                if (task.taskDependency != null) {
                    setOfTaskWithDependentTasks.add(task.taskDependency)
                    model.addGreaterOrEqual(
                        allTasks[Pair(order_id, task.task_id)]!!.start,
                        allTasks[Pair(order_id, task.taskDependency)]!!.end
                    )
                }
            }

            val lastNotAdditionalTasks =
                order.tasks.filter { !it.isAdditional && (it.task_id !in setOfTaskWithDependentTasks) }

            additionalTasks.forEach { task ->
                if (task.taskDependency != null) {
                    model.addGreaterOrEqual(
                        allTasks[Pair(order_id, task.task_id)]!!.start,
                        allTasks[Pair(order_id, task.taskDependency)]!!.end
                    )
                } else {
                    lastNotAdditionalTasks.forEach { lastTask ->
                        model.addGreaterOrEqual(
                            allTasks[Pair(order_id, task.task_id)]!!.start,
                            allTasks[Pair(order_id, lastTask.task_id)]!!.end
                        )
                    }
                }
            }


        }

        // Makespan objective.
        val objVar = model.newIntVar(0, horizon, "makespan")
        model.addMaxEquality(
            objVar,
            allTasks.values.map { it.end }.toTypedArray()
        )
        model.minimize(objVar)

        // Solve model.
        val solver = CpSolver()
        val status = solver.solve(model)

        print(status)
        if (status == CpSolverStatus.OPTIMAL) {
            //Create one list of assigned tasks per machine.
            val assignedTasks = mutableMapOf<Int, MutableList<AssignedTaskType>>()
            items.forEachIndexed { order_id, order ->
                order.tasks.forEach { task ->
                    val task_id = task.task_id
                    val machine = task.workerType
                    if (assignedTasks[machine] == null) assignedTasks[machine] = mutableListOf()
                    assignedTasks[machine]?.add(
                        AssignedTaskType(
                            start = solver.value(allTasks[Pair(order_id, task_id)]?.start),
                            job = order_id, index = task_id, duration = task.timeToComplete,
                            title = task.title, color = task.color, item_id = task.item_id
                        )
                    )
                }
            }
            // Create per machine output lines.
            val tasks = mutableListOf<ScheduleTaskData>()
            val pattern = "yyyy-MM-dd HH:mm:ss"
            val df: DateFormat = SimpleDateFormat(pattern)

            var output = ""
            workerTypesSet.forEach { machine ->
                // Sort by starting time.
                assignedTasks[machine]?.sortBy { it.start }
                var solLineTasks = "Machine $machine: "
                var solLine = ""

                assignedTasks[machine]?.forEach { assigned_task ->
                    val name = " order_${assigned_task.job}_${assigned_task.index}"
                    // Add spaces to output to align columns.
                    solLineTasks += name

                    val start = assigned_task.start
                    val duration = assigned_task.duration
                    val solTmp = "[$start ${start + duration}]"
                    solLine += solTmp

                    // current day + hour + hours from duration
                    val today = Calendar.getInstance().time
                    val newCal = Calendar.getInstance()
//                    newCal.time = today
                    newCal.add(Calendar.HOUR, 1 + start.toInt())
                    val startString = df.format(newCal.time)
                    newCal.add(Calendar.HOUR, duration)
                    val endString = df.format(newCal.time)
                    println("today ${df.format(today)}")
                    println("duration ${duration}")
                    println("start ${start}")
                    println("start $startString")
                    println("end $endString")

                    tasks.add(
                        ScheduleTaskData(
                            id = assigned_task.index,
                            resourceId = machine,
                            itemId = assigned_task.item_id,
                            taskId = assigned_task.index,
                            taskDependencyId = assigned_task.taskDependency,
                            taskStatus = assigned_task.taskStatus,
                            start = startString,
                            end = endString,
                            title = "${assigned_task.title} ${duration}h",
                            bgColor = assigned_task.color
                        )
                    )
                }

                solLine += "\n"
                solLineTasks += "\n"
                output += solLineTasks
                output += solLine
            }

            // Finally print the solution found.
            print("Optimal Schedule Length: ${solver.objectiveValue()}\n")
            print(output)

            return tasks
        }
        return emptyList()
    }

    fun optimizeFourth(items: List<ItemData>): List<ScheduleTaskData> {
        OrToolsHelper.loadLibrary()
        // Create the model.
        val model = CpModel()

        val workerTypesSet = items.map { it.tasks.map { it.workerType } }.flatten().toSortedSet()

        // Computes horizon dynamically as the sum of all durations.
        val horizon = items.sumBy { it.tasks.sumBy { it.timeToComplete } }.toLong()

        // Creates job intervals and add to the corresponding machine lists.
        val allTasks = mutableMapOf<Pair<Int, Int>, TaskType>()
        val workerTypeToIntervals = mutableMapOf<Int, MutableList<IntervalVar>>()

        items.forEach { item ->
            item.tasks.forEach { task ->
                val task_id = task.task_id
                val item_id = task.item_id
                println(" task id $task_id")
                val machine = task.workerType
                val duration = task.timeToComplete.toLong()
                val suffix = "_${item_id}_$task_id"
                val startVar = model.newIntVar(0, horizon, "start" + suffix)
                val endVar = model.newIntVar(0, horizon, "end" + suffix)
                val intervalVar = model.newIntervalVar(startVar, duration, endVar, "interval" + suffix)

                allTasks[Pair(item_id, task_id)] = TaskType(start = startVar, end = endVar, interval = intervalVar)
                if (workerTypeToIntervals[machine] == null) workerTypeToIntervals[machine] = mutableListOf()
                workerTypeToIntervals[machine]!!.add(intervalVar)
            }
        }
        // Create and add disjunctive constraints.
        workerTypesSet.forEach { machine ->
            model.addNoOverlap(workerTypeToIntervals[machine]!!.toTypedArray())
        }
        // Tasks in one item should not overlap
        items.forEach { item ->
            val intervals = mutableListOf<IntervalVar>()
            item.tasks.forEach { task ->
                intervals.add(
                    allTasks[Pair(task.item_id, task.task_id)]!!.interval
                )
            }
            model.addNoOverlap(intervals.toTypedArray())
        }
        // Precedences inside a order.
        items.forEach { item ->
            val notAdditionalTasks = item.tasks.filter { !it.isAdditional }
            val additionalTasks = item.tasks.filter { it.isAdditional }

            val setOfTaskWithDependentTasks = mutableSetOf<Int>()

            notAdditionalTasks.forEach { task ->
                if (task.taskDependency != null) {
                    setOfTaskWithDependentTasks.add(task.taskDependency)
                    model.addGreaterOrEqual(
                        allTasks[Pair(task.item_id, task.task_id)]!!.start,
                        allTasks[Pair(task.item_id, task.taskDependency)]!!.end
                    )
                }
            }

            val lastNotAdditionalTasks =
                item.tasks.filter { !it.isAdditional && (it.task_id !in setOfTaskWithDependentTasks) }

            additionalTasks.forEach { task ->
                if (task.taskDependency != null) {
                    model.addGreaterOrEqual(
                        allTasks[Pair(task.item_id, task.task_id)]!!.start,
                        allTasks[Pair(task.item_id, task.taskDependency)]!!.end
                    )
                } else {
                    lastNotAdditionalTasks.forEach { lastTask ->
                        model.addGreaterOrEqual(
                            allTasks[Pair(task.item_id, task.task_id)]!!.start,
                            allTasks[Pair(lastTask.item_id, lastTask.task_id)]!!.end
                        )
                    }
                }
            }
        }

        // Task IN_WORK should be at the start of list
        // TODO: CHECK IT
        val flatTasks = items.flatMap { it.tasks }
        val inWork = flatTasks.filter { it.status == TaskStatus.IN_WORK }
        inWork.forEach { task ->
            flatTasks.filter { it.workerType == task.workerType }.forEach { laterTask ->
                model.addGreaterOrEqual(
                    allTasks[Pair(laterTask.item_id, laterTask.task_id)]!!.start,
                    allTasks[Pair(task.item_id, task.task_id)]!!.start
                )
            }
        }

        // Makespan objective.
        val objVar = model.newIntVar(0, horizon, "makespan")
        model.addMaxEquality(
            objVar,
            allTasks.values.map { it.end }.toTypedArray()
        )
        model.minimize(objVar)

        // Solve model.
        val solver = CpSolver()
        val status = solver.solve(model)

        print(status)
        if (status == CpSolverStatus.OPTIMAL) {
            //Create one list of assigned tasks per machine.
            val assignedTasks = mutableMapOf<Int, MutableList<AssignedTaskType>>()
            items.forEach { item ->
                item.tasks.forEach { task ->
                    val task_id = task.task_id
                    val machine = task.workerType
                    if (assignedTasks[machine] == null) assignedTasks[machine] = mutableListOf()
                    assignedTasks[machine]?.add(
                        AssignedTaskType(
                            start = solver.value(allTasks[Pair(task.item_id, task_id)]?.start),
                            job = task.item_id, index = task_id, duration = task.timeToComplete,
                            title = task.title, color = task.color, taskStatus = task.status, item_id = task.item_id,
                            taskDependency = task.taskDependency
                        )
                    )
                }
            }
            // Create per machine output lines.
            val tasks = mutableListOf<ScheduleTaskData>()
            val pattern = "yyyy-MM-dd HH:mm:ss"
            val df: DateFormat = SimpleDateFormat(pattern)
            val today = getNextWorkingDate(Calendar.getInstance().time) // start working date

            var output = ""
            workerTypesSet.forEach { machine ->
                // Sort by starting time.
                assignedTasks[machine]?.sortBy { it.start }
                var solLineTasks = "Machine $machine: "
                var solLine = ""

                assignedTasks[machine]?.forEach { assigned_task ->
                    val name = " item_${assigned_task.job}_${assigned_task.index}"
                    // Add spaces to output to align columns.
                    solLineTasks += name

                    val start = assigned_task.start
                    val duration = assigned_task.duration
                    val solTmp = "[$start ${start + duration}]"
                    solLine += solTmp

                    // current day + hour + hours from duration

                    val newCal = Calendar.getInstance()
//                    newCal.time = today
//                    newCal.add(Calendar.HOUR, 1 + start.toInt())
                    val dateStart = addWorkingHourSecond(today, start.toInt())
                    val dateEnd = addWorkingHourSecond(today, start.toInt() + duration)

                    val startString = df.format(dateStart)
//                    newCal.add(Calendar.HOUR, duration)
                    val endString = df.format(dateEnd)
                    println("today ${df.format(today)}")
                    println("duration ${duration}")
                    println("start ${start}")
                    println("start $startString")
                    println("end $endString")

                    tasks.add(
                        ScheduleTaskData(
                            id = assigned_task.index,
                            resourceId = machine,
                            itemId = assigned_task.item_id,
                            taskId = assigned_task.index,
                            taskDependencyId = assigned_task.taskDependency,
                            taskStatus = assigned_task.taskStatus,
                            start = startString,
                            end = endString,
                            title = "${assigned_task.title} ${duration}h",
                            bgColor = assigned_task.color
                        )
                    )
                }

                solLine += "\n"
                solLineTasks += "\n"
                output += solLineTasks
                output += solLine
            }

            // Finally print the solution found.
            print("Optimal Schedule Length: ${solver.objectiveValue()}\n")
            print(output)

            return tasks
        }
        return emptyList()
    }

    private fun addWorkingHourSecond(startTime: Date, hourToAdd: Int): Date {
        if (hourToAdd == 0) return startTime

        var dateTime = DateTime(startTime)
        var wholeDays = hourToAdd.div(START_WORKING_DAY)
        var additionalMinutes = (hourToAdd % START_WORKING_DAY) * 60

        while (wholeDays > 0 || additionalMinutes > 0) {
            if (wholeDays > 0) {
                dateTime = dateTime.plusDays(1)
                if (dateTime.dayOfWeek != 6 && dateTime.dayOfWeek != 7) {
                    wholeDays--
                }
            } else if (additionalMinutes > 0) {
                val leftToday = END_WORKING_DAY * 60 - dateTime.minuteOfDay
                if (leftToday > 0) {
                    if (additionalMinutes < leftToday) {
                        dateTime = dateTime.plusMinutes(additionalMinutes)
                        additionalMinutes = 0
                    } else {
                        additionalMinutes -= leftToday
                        dateTime = dateTime.plusMinutes(leftToday)
                    }
                } else {
                    wholeDays++
                    dateTime = dateTime.hourOfDay().setCopy(START_WORKING_DAY)
                    dateTime = dateTime.minuteOfHour().setCopy(0)
                    dateTime = dateTime.secondOfMinute().setCopy(0)
                }
            }
        }

        return dateTime.toDate()
    }

    // working time: MON - FRI, 9-18
    private fun getNextWorkingDate(startDate: Date): Date {
        var date = DateTime(startDate)
        date = date.secondOfMinute().setCopy(0)
        date = date.millisOfSecond().setCopy(0)
        val dayOfTheWeek = date.dayOfWeek().get()
        val dayHour = date.hourOfDay().get()

        if (dayOfTheWeek in listOf(5, 6, 7)) {
            when (dayOfTheWeek) {
                5 -> {
                    when {
                        dayHour < START_WORKING_DAY -> { // FRI <START_WORKING_DAY
                            date = date.hourOfDay().setCopy(START_WORKING_DAY)
                            date = date.minuteOfHour().setCopy(0)
                            date = date.secondOfMinute().setCopy(0)

                            return date.toDate()
                        }
                        dayHour >= END_WORKING_DAY -> { // FRI >= END_WORKING_DAY
                            date = date.plusDays(3)
                            date = date.hourOfDay().setCopy(START_WORKING_DAY)
                            date = date.minuteOfHour().setCopy(0)
                            date = date.secondOfMinute().setCopy(0)

                            return date.toDate()
                        }
                        else -> {
                            return date.toDate()
                        }
                    }
                }
                6 -> {
                    date = date.plusDays(2)
                    date = date.hourOfDay().setCopy(START_WORKING_DAY)
                    date = date.minuteOfHour().setCopy(0)
                    date = date.secondOfMinute().setCopy(0)

                    return date.toDate()
                }
                7 -> {
                    date = date.plusDays(1)
                    date = date.hourOfDay().setCopy(START_WORKING_DAY)
                    date = date.minuteOfHour().setCopy(0)
                    date = date.secondOfMinute().setCopy(0)

                    return date.toDate()
                }
            }
        } else if (dayHour !in START_WORKING_DAY until END_WORKING_DAY) { // START_WORKING_DAY <= dayHour < END_WORKING_DAY
            return if (dayHour < START_WORKING_DAY) {
                date = date.hourOfDay().setCopy(START_WORKING_DAY)
                date = date.minuteOfHour().setCopy(0)
                date = date.secondOfMinute().setCopy(0)

                date.toDate()
            } else { // >= END_WORKING_DAY
                date = date.plusDays(1)
                date = date.hourOfDay().setCopy(START_WORKING_DAY)
                date = date.minuteOfHour().setCopy(0)
                date = date.secondOfMinute().setCopy(0)

                date.toDate()
            }
        }
        return date.toDate()
    }

    private const val START_WORKING_DAY = 9
    private const val END_WORKING_DAY = 18

}