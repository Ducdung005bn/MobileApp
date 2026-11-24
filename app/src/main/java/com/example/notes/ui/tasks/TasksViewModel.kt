
package com.example.notes.ui.tasks

import com.example.notes.data.TaskWithSubtasks
import com.example.notes.domain.Category
import com.example.notes.domain.Priority
import com.example.notes.domain.TaskFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notes.NotesApplication
import com.example.notes.domain.TimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn


data class TasksState(
    val filter: TaskFilter = TaskFilter.ALL,
    val search: String = "",
    val category: Category? = null,
    val priority: Priority? = null,
    val tasks: List<TaskWithSubtasks> = emptyList(),
    val loaded: Boolean = false
)


/**
 * ViewModel của màn danh sách công việc.
 *
 * Quản lý trạng thái bộ lọc và danh sách công việc.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class TasksViewModel(private val app: NotesApplication) : ViewModel() {

    // 4 bộ lọc của màn hình
    private val filter = MutableStateFlow(TaskFilter.ALL)
    private val search = MutableStateFlow("")
    private val category = MutableStateFlow<Category?>(null)
    private val priority = MutableStateFlow<Priority?>(null)

    /** Gom 4 bộ lọc thành một đối tượng truy vấn. */
    private data class Query(
        val filter: TaskFilter,
        val search: String,
        val category: Category?,
        val priority: Priority?
    )

    // Kết hợp các bộ lọc và trì hoãn tìm kiếm 200ms
    private val queryFlow = combine(
        filter,
        search.debounce(200),
        category,
        priority
    ) { f, s, c, p ->
        Query(f, s, c, p)
    }

    /**
     * Theo dõi danh sách công việc theo bộ lọc.
     * flatMapLatest huỷ truy vấn cũ khi có truy vấn mới.
     */
    val state: StateFlow<TasksState> = combine(
        queryFlow.flatMapLatest { q ->
            app.taskRepository.observeTasks(
                filter = q.filter,
                search = q.search,
                category = q.category,
                priority = q.priority,
                epochDay = TimeUtils.today()
            )
        },
        filter,
        search,
        category,
        priority
    ) { tasks, f, s, c, p ->
        TasksState(
            filter = f,
            search = s,
            category = c,
            priority = p,
            tasks = tasks,
            loaded = true
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TasksState()
    )
}

