package io.defolters.optimization

import com.google.ortools.sat.*
import com.skaggsm.ortools.OrToolsHelper

data class TaskType(val start: IntVar, val end: IntVar, val interval: IntervalVar)
data class AssignedTaskType(val start: Long, val job: Int, val index: Int, val duration: Int)

data class ItemData(val tasks: List<TaskData>)
data class TaskData(
    val task_id: Int,
    val workerType: Int,
    val timeToComplete: Int,
    val priority: Int = 0,
    val taskDependency: Int? = null,
    val isAdditional: Boolean = false
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
        optimize(orders)

        val orders2 = listOf(
            ItemData(
                listOf(
                    TaskData(0, 0, 3, 1),
                    TaskData(1, 1, 2, 2),
                    TaskData(2, 2, 2, 2),
                    TaskData(3, 0, 2, 3),
                    TaskData(4, 3, 1, 4)
                )
            ),
            ItemData(
                listOf(
                    TaskData(0, 0, 2, 1),
                    TaskData(1, 2, 1, 2),
                    TaskData(2, 1, 4, 3)
                )
            ),
            ItemData(
                listOf(
                    TaskData(0, 1, 4, 1),
                    TaskData(1, 2, 3, 2)
                )
            )
        )
        optimizeSecond(orders2)

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

    fun optimize(items: List<ItemData>) {
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
            order.tasks.forEachIndexed { task_id, task ->
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
        // Precedences inside a order.
        items.forEachIndexed { order_id, order ->
            for (task_id in 0 until (order.tasks.size - 1)) { // until
                model.addGreaterOrEqual(
                    allTasks[Pair(order_id, task_id + 1)]!!.start,
                    allTasks[Pair(order_id, task_id)]!!.end
                )
            }
        }

        // Makespan objective.
        val objVar = model.newIntVar(0, horizon, "makespan")
        model.addMaxEquality(
            objVar,
            items.mapIndexed { order_id, order ->
                allTasks[Pair(order_id, order.tasks.size - 1)]?.end
            }.toTypedArray()
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
                order.tasks.forEachIndexed { task_id, task ->
                    val machine = task.workerType
                    if (assignedTasks[machine] == null) assignedTasks[machine] = mutableListOf()
                    assignedTasks[machine]?.add(
                        AssignedTaskType(
                            start = solver.value(allTasks[Pair(order_id, task_id)]?.start),
                            job = order_id, index = task_id, duration = task.timeToComplete
                        )
                    )
                }
            }
            // Create per machine output lines.
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
                }

                solLine += "\n"
                solLineTasks += "\n"
                output += solLineTasks
                output += solLine
            }

            // Finally print the solution found.
            print("Optimal Schedule Length: ${solver.objectiveValue()}\n")
            print(output)
        }
    }

    fun optimizeSecond(items: List<ItemData>) {
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
            val tasksByPriority = order.tasks.groupBy { it.priority }.toSortedMap()
            var lastEnd: List<TaskData>? = null

            tasksByPriority.forEach { (_, value) ->

                if (lastEnd != null) {
                    value.forEach { task ->
                        lastEnd!!.forEach { lastTask ->
                            model.addGreaterOrEqual(
                                allTasks[Pair(order_id, task.task_id)]!!.start,
                                allTasks[Pair(order_id, lastTask.task_id)]!!.end
                            )
                        }
                    }
                }
                lastEnd = value
            }
        }

        // Makespan objective.
        val objVar = model.newIntVar(0, horizon, "makespan")
        model.addMaxEquality(
            objVar,
            items.mapIndexed { order_id, order ->
                allTasks[Pair(order_id, order.tasks.size - 1)]?.end
            }.toTypedArray()
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
                            job = order_id, index = task_id, duration = task.timeToComplete
                        )
                    )
                }
            }
            // Create per machine output lines.
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
                }

                solLine += "\n"
                solLineTasks += "\n"
                output += solLineTasks
                output += solLine
            }

            // Finally print the solution found.
            print("Optimal Schedule Length: ${solver.objectiveValue()}\n")
            print(output)
        }
    }

    fun optimizeThird(items: List<ItemData>) {
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
            items.mapIndexed { order_id, order ->
                allTasks[Pair(order_id, order.tasks.size - 1)]?.end
            }.toTypedArray()
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
                            job = order_id, index = task_id, duration = task.timeToComplete
                        )
                    )
                }
            }
            // Create per machine output lines.
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
                }

                solLine += "\n"
                solLineTasks += "\n"
                output += solLineTasks
                output += solLine
            }

            // Finally print the solution found.
            print("Optimal Schedule Length: ${solver.objectiveValue()}\n")
            print(output)
        }
    }

    fun minimalJobshopSat() {
        OrToolsHelper.loadLibrary()
        """Minimal jobshop problem."""
        // Create the model.
        val model = CpModel()

        val jobs_data = listOf(  // task = (machine_id, processing_time).
            listOf(Pair(0, 3), Pair(1, 2), Pair(2, 2)), //Job0
            listOf(Pair(0, 2), Pair(2, 1), Pair(1, 4)), //Job1
            listOf(Pair(1, 4), Pair(2, 3)) //Job2
        )
        val machines_count = 3 //(three machines)
        val all_machines = (0 until machines_count).toMutableList()

        //# Computes horizon dynamically as the sum of all durations.
        val horizon = jobs_data.sumBy { it.sumBy { it.second } }.toLong()

        // Creates job intervals and add to the corresponding machine lists.
        val all_tasks = mutableMapOf<Pair<Int, Int>, TaskType>()
        val machine_to_intervals = mutableMapOf<Int, MutableList<IntervalVar>>()

        jobs_data.forEachIndexed { job_id, job ->
            job.forEachIndexed { task_id, task ->
                val machine = task.first
                val duration = task.second.toLong()
                val suffix = "_${job_id}_$task_id"
                val start_var = model.newIntVar(0, horizon, "start" + suffix)
                val end_var = model.newIntVar(0, horizon, "end" + suffix)
                val interval_var = model.newIntervalVar(start_var, duration, end_var, "interval" + suffix)

                all_tasks[Pair(job_id, task_id)] = TaskType(start = start_var, end = end_var, interval = interval_var)
                if (machine_to_intervals[machine] == null) machine_to_intervals[machine] = mutableListOf()
                machine_to_intervals[machine]!!.add(interval_var)
            }
        }
        // Create and add disjunctive constraints.
        all_machines.forEach { machine ->
            model.addNoOverlap(machine_to_intervals[machine]!!.toTypedArray())
        }
        // Precedences inside a job.
        jobs_data.forEachIndexed { job_id, job ->
            for (task_id in 0 until (job.size - 1)) { // until
                model.addGreaterOrEqual(
                    all_tasks[Pair(job_id, task_id + 1)]!!.start,
                    all_tasks[Pair(job_id, task_id)]!!.end
                )
            }
        }

        // Makespan objective.
        val obj_var = model.newIntVar(0, horizon, "makespan")
        model.addMaxEquality(
            obj_var,
            jobs_data.mapIndexed { job_id, job ->
                all_tasks[Pair(job_id, job.size - 1)]?.end
            }.toTypedArray()
        )
        model.minimize(obj_var)

        // Solve model.
        val solver = CpSolver()
        val status = solver.solve(model)

        print(status)
        if (status == CpSolverStatus.OPTIMAL) {
            //Create one list of assigned tasks per machine.
            val assigned_jobs = mutableMapOf<Int, MutableList<AssignedTaskType>>()
            jobs_data.forEachIndexed { job_id, job ->
                job.forEachIndexed { task_id, task ->
                    val machine = task.first
                    if (assigned_jobs[machine] == null) assigned_jobs[machine] = mutableListOf()
                    assigned_jobs[machine]?.add(
                        AssignedTaskType(
                            start = solver.value(all_tasks[Pair(job_id, task_id)]?.start),
                            job = job_id, index = task_id, duration = task.second
                        )
                    )
                }
            }
            // Create per machine output lines.
            var output = ""
            all_machines.forEach { machine ->
                // Sort by starting time.
                assigned_jobs[machine]?.sortBy { it.start }
                var sol_line_tasks = "Machine $machine: "
                var sol_line = ""

                assigned_jobs[machine]?.forEach { assigned_task ->
                    val name = " job_${assigned_task.job}_${assigned_task.index}"
                    // Add spaces to output to align columns.
                    sol_line_tasks += name

                    val start = assigned_task.start
                    val duration = assigned_task.duration
                    val sol_tmp = "[$start ${start + duration}]"
                    sol_line += sol_tmp
                }

                sol_line += "\n"
                sol_line_tasks += "\n"
                output += sol_line_tasks
                output += sol_line
            }

            // Finally print the solution found.
            print("Optimal Schedule Length: ${solver.objectiveValue()}\n")
            print(output)
        }
    }

}