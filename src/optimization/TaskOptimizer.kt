package io.defolters.optimization

import com.google.ortools.sat.*
import com.skaggsm.ortools.OrToolsHelper
import io.defolters.routes.ScheduleTaskData
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
    val color: String
)

data class ItemData(val tasks: List<TaskData>)
data class TaskData(
    val task_id: Int,
    val workerType: Int,
    val timeToComplete: Int,
    val taskDependency: Int? = null,
    val isAdditional: Boolean = false,
    val title: String = "",
    val color: String = ""
)

object TaskOptimizer {

    fun optimizeTest() {
        val orders = listOf(
            ItemData(
                listOf(
                    TaskData(0, 0, 3),
                    TaskData(1, 1, 2),
                    TaskData(2, 2, 2)
                )
            ),
            ItemData(
                listOf(
                    TaskData(0, 0, 2),
                    TaskData(1, 2, 1),
                    TaskData(2, 1, 4)
                )
            ),
            ItemData(
                listOf(
                    TaskData(0, 1, 4),
                    TaskData(1, 2, 3)
                )
            )
        )

        val orders3 = listOf(
            ItemData(
                listOf(
                    TaskData(
                        task_id = 1, workerType = 0, timeToComplete = 10,
                        taskDependency = null, isAdditional = false
                    ),
                    TaskData(
                        task_id = 2, workerType = 1, timeToComplete = 10,
                        taskDependency = 1, isAdditional = false
                    ),
                    TaskData(
                        task_id = 3, workerType = 2, timeToComplete = 10,
                        taskDependency = 1, isAdditional = false
                    ),
                    TaskData(
                        task_id = 4, workerType = 1, timeToComplete = 10,
                        taskDependency = 2, isAdditional = false
                    ),
                    TaskData(
                        task_id = 5, workerType = 2, timeToComplete = 10,
                        taskDependency = 4, isAdditional = false
                    ),//
                    TaskData(
                        task_id = 6, workerType = 0, timeToComplete = 10,
                        taskDependency = null, isAdditional = true
                    ),
                    TaskData(
                        task_id = 7, workerType = 1, timeToComplete = 10,
                        taskDependency = null, isAdditional = true
                    ),
                    TaskData(
                        task_id = 8, workerType = 2, timeToComplete = 10,
                        taskDependency = null, isAdditional = true
                    ),
                    TaskData(
                        task_id = 9, workerType = 0, timeToComplete = 10,
                        taskDependency = 8, isAdditional = true
                    ),
                    TaskData(
                        task_id = 10, workerType = 1, timeToComplete = 10,
                        taskDependency = 8, isAdditional = true
                    )
                )
            ),
            ItemData(
                listOf(
                    TaskData(
                        task_id = 1, workerType = 0, timeToComplete = 10,
                        taskDependency = null, isAdditional = false
                    ),
                    TaskData(
                        task_id = 2, workerType = 1, timeToComplete = 10,
                        taskDependency = 1, isAdditional = false
                    ),
                    TaskData(
                        task_id = 3, workerType = 2, timeToComplete = 10,
                        taskDependency = 1, isAdditional = false
                    ),
                    TaskData(
                        task_id = 4, workerType = 1, timeToComplete = 10,
                        taskDependency = 2, isAdditional = false
                    ),
                    TaskData(
                        task_id = 5, workerType = 2, timeToComplete = 10,
                        taskDependency = 4, isAdditional = false
                    ),//
                    TaskData(
                        task_id = 6, workerType = 0, timeToComplete = 10,
                        taskDependency = null, isAdditional = true
                    ),
                    TaskData(
                        task_id = 7, workerType = 1, timeToComplete = 10,
                        taskDependency = null, isAdditional = true
                    ),
                    TaskData(
                        task_id = 8, workerType = 2, timeToComplete = 10,
                        taskDependency = null, isAdditional = true
                    ),
                    TaskData(
                        task_id = 9, workerType = 0, timeToComplete = 10,
                        taskDependency = 8, isAdditional = true
                    ),
                    TaskData(
                        task_id = 10, workerType = 1, timeToComplete = 10,
                        taskDependency = 8, isAdditional = true
                    )
                )
            )
        )
        optimizeThird(orders3)
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
                            title = task.title, color = task.color
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

}