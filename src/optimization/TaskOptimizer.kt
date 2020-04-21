package io.defolters.optimization

import com.google.ortools.sat.*
import com.skaggsm.ortools.OrToolsHelper

data class TaskType(val start: IntVar, val end: IntVar, val interval: IntervalVar)
data class AssignedTaskType(val start: Long, val job: Int, val index: Int, val duration: Int)

object TaskOptimizer {
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