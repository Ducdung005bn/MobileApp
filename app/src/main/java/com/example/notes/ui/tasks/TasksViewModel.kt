
package com.example.notes.ui.tasks

import com.example.notes.data.TaskWithSubtasks
import com.example.notes.domain.Category
import com.example.notes.domain.Priority
import com.example.notes.domain.TaskFilter

data class TasksState(
    val filter: TaskFilter = TaskFilter.ALL,
    val search: String = "",
    val category: Category? = null,
    val priority: Priority? = null,
    val tasks: List<TaskWithSubtasks> = emptyList(),
    val loaded: Boolean = false
)
