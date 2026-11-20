// =============================================================================================
// FILE: ui/AppViewModels.kt
// NHIỆM VỤ: cung cấp hàm `appViewModel { app -> ... }`, cách duy nhất để các Screen tạo hoặc lấy lại
// ViewModel (vì ViewModel cần NotesApplication nên không dùng được factory mặc định của Android).
// XỬ LÝ:
//   • Lấy NotesApplication từ LocalContext rồi gọi `viewModel(key, factory)` của thư viện.
//   • Cùng `key` thì trả lại đúng ViewModel cũ (sống qua xoay máy). Khác `key` thì là ViewModel khác,
//     ví dụ "task-editor-5" và "task-editor-7" là hai form độc lập.
//   • ViewModel gắn với màn hình trong ngăn xếp điều hướng: khi màn đó bị gỡ khỏi ngăn xếp (ví dụ đóng
//     form soạn thảo bằng popBackStack) thì ViewModel bị huỷ; mở lại form sẽ tạo ViewModel mới.
// BACK / ĐÓNG: file này không xử lý Back, chỉ quyết định vòng đời ViewModel như trên.
// LIÊN QUAN: NotesApplication.kt (nơi giữ repository), mọi XxxScreen.kt (nơi gọi hàm này).
// =============================================================================================

package com.example.notes.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.notes.NotesApplication

/**
 * Tạo (hoặc lấy lại nếu đã có) một ViewModel cho màn hình đang vẽ.
 *
 * Vì sao cần hàm này? ViewModel của chúng ta cần `NotesApplication` (nơi giữ các repository).
 * Mặc định Android chỉ tạo được ViewModel không tham số, nên ta phải cung cấp một "nhà máy" (factory).
 *
 * Cách dùng trong màn hình:
 *     viewModel: TasksViewModel = appViewModel { app -> TasksViewModel(app) }
 *
 * - [key]: tên riêng để phân biệt hai ViewModel cùng loại (ví dụ trình soạn công việc số 1 và số 2).
 * - ViewModel được giữ lại khi xoay màn hình, và bị huỷ khi rời khỏi màn hình đó.
 *
 * (`inline` + `reified` chỉ là để Kotlin biết kiểu VM cụ thể; bạn không cần quan tâm lúc mới học.)
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (NotesApplication) -> VM
): VM {
    // LocalContext.current = context của màn hình hiện tại; .applicationContext là đối tượng Application.
    val app = LocalContext.current.applicationContext as NotesApplication
    return viewModel(
        key = key,
        factory = viewModelFactory { initializer { create(app) } }
    )
}
