// =============================================================================================
// FILE: ui/UiMessenger.kt
// NHIỆM VỤ: "hộp thư" thông báo dùng chung cho cả app, để ViewModel báo tin cho người dùng mà không cần
// biết gì về giao diện.
// XỬ LÝ:
//   • `post("Đã xoá")`: tin ngắn không có nút.
//   • `post(UiMessage(text, actionLabel = "Hoàn tác", onAction = {...}))`: tin có nút; bấm nút thì
//     NotesApp gọi `onAction` (dùng cho hoàn tác xoá công việc, xoá ghi chú, lưu trữ ghi chú/thói quen).
//   • Dùng SharedFlow (phát sự kiện một lần, không giữ tin cuối). Bộ đệm 16 tin nên gửi nhanh không bị
//     chặn; tin gửi lúc chưa có ai lắng nghe thì bị bỏ.
// BACK / ĐÓNG: không xử lý Back. Thông báo do ViewModel gửi sau khi form đóng (ví dụ "Đã xoá công
// việc") vẫn hiện được vì hộp thư là của cả app, không gắn với màn hình nào.
// LIÊN QUAN: nav/NotesApp.kt (ShowMessages hiện Snackbar), các ViewModel gọi `messenger.post`.
// =============================================================================================

package com.example.notes.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Một thông báo ngắn hiện ở dưới màn hình (snackbar).
 * Nếu có [actionLabel] (ví dụ "Hoàn tác") thì snackbar có thêm nút; bấm nút sẽ gọi [onAction].
 */
data class UiMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

/**
 * "Hộp thư" toàn app cho thông báo.
 * Bất kỳ ViewModel nào cũng có thể gọi `messenger.post("Đã xoá")`; khung ứng dụng (NotesApp)
 * lắng nghe [messages] và hiện snackbar. Nhờ vậy ViewModel không cần biết gì về giao diện.
 *
 * SharedFlow khác StateFlow ở chỗ: nó phát "sự kiện" một lần (thông báo), không giữ giá trị cuối.
 */
class UiMessenger {
    // MutableSharedFlow: phía trong (có thể ghi). extraBufferCapacity cho phép gửi nhanh mà không bị chặn.
    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 16)

    // Phía ngoài chỉ đọc (không ghi được).
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    fun post(text: String) {
        _messages.tryEmit(UiMessage(text))
    }

    fun post(message: UiMessage) {
        _messages.tryEmit(message)
    }
}
