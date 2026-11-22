// =============================================================================================
// FILE: ui/calendar/CalendarScreen.kt
// VAI TRÒ: "người điều phối" của tab Lịch. File này KHÔNG tự vẽ chi tiết; nó ghép các mảnh nhỏ
// (nằm ở các file khác trong thư mục) thành một màn hình hoàn chỉnh, và quyết định khi nào mở/đóng
// "form lịch" (EventEditorSheet).
//
// BẢN ĐỒ THƯ MỤC ui/calendar/ (đọc theo thứ tự này sẽ dễ hiểu nhất):
//   CalendarViewModel.kt   BỘ NÃO: giữ trạng thái (chế độ, ngày chọn, danh sách khối lịch), lưu/xoá/dời lịch.
//   CalendarScreen.kt      (file này) ĐIỀU PHỐI: nối ViewModel với các mảnh giao diện bên dưới.
//   CalendarHeader.kt      Đầu trang: tiêu đề, nút Hôm nay/‹ ›, chọn Ngày|Tuần|Tháng, dải cảnh báo trùng giờ.
//   ── Chế độ NGÀY / TUẦN (lưới giờ) ──────────────────────────────────────────────────────────
//   TimeGrid.kt            Khung lưới giờ: ghép tiêu đề ngày + cột giờ + các cột ngày.
//   TimeGridFrame.kt       Phần "khung" quanh lưới: DayHeader (T2, 24), AllDayRow (cả ngày), HourLabels (01:00...).
//   DayColumn.kt           Một cột ngày: đặt từng khối lịch vào đúng giờ; có NowLine (vạch giờ hiện tại).
//   LaneLayout.kt          Thuật toán chia "làn" để khối trùng giờ nằm cạnh nhau (hàm thuần, không có giao diện).
//   EventBlock.kt          Một khối lịch: hiển thị + bấm để mở + nhấn giữ rồi kéo để dời.
//   ── Chế độ THÁNG ───────────────────────────────────────────────────────────────────────────
//   MonthView.kt           Lưới tháng: MonthView (cả lưới) + MonthCell (một ô ngày).
//   DayAgenda.kt           Danh sách lịch của ngày đang chọn, nằm dưới lưới tháng.
//   ── Form thêm/sửa lịch ─────────────────────────────────────────────────────────────────────
//   EventEditorSheet.kt    Tờ giấy trượt từ đáy để thêm/sửa lịch.
//   EventEditorFields.kt   Hai thành phần nhỏ dùng trong form: ChipRow, SwitchRow.
//
// DÒNG CHẢY DỮ LIỆU (quan trọng để hiểu cả thư mục):
//   • Dữ liệu đi XUỐNG:  CalendarViewModel ─state─► CalendarScreen ─► TimeGrid/MonthView ─► ... ─► EventBlock
//   • Sự kiện đi LÊN:    EventBlock (bấm/kéo) ─► callback ─► CalendarScreen ─► viewModel.moveBlock / mở form
//   Các mảnh vẽ chỉ nhận dữ liệu và gọi callback; chúng không biết ViewModel tồn tại.
//
// XỬ LÝ CỦA FILE NÀY:
//   • Lấy `state` từ ViewModel; chọn vẽ MonthView (chế độ Tháng) hoặc TimeGrid (Ngày/Tuần).
//   • Bấm một khối lịch (`openBlock`): công việc thì onOpenTask(id) để mở form công việc; sự kiện thì
//     tải về (`loadEvent`) rồi đặt `editingEvent` để mở EventEditorSheet ở chế độ Sửa.
//   • Nút `+` đặt `editingEvent` = lịch mới mặc định 9:00-10:00 của ngày đang chọn.
// BACK / ĐÓNG form lịch:
//   • `editingEvent` khác null thì EventEditorSheet được vẽ; `editingEvent = null` thì sheet bị gỡ khỏi cây.
//   • Back, vuốt sheet xuống hoặc chạm ra ngoài gọi `onDismiss = { editingEvent = null }`: sheet đóng,
//     mọi ô đang điền mất, KHÔNG gọi `viewModel.saveEvent`. Chỉ nút "Lưu" trong sheet mới lưu.
//   • Khi sheet đóng, Back tiếp theo là Back của tab (về tab Hôm nay, xem nav/NotesApp.kt).
// ĐƯỢC GỌI TỪ: nav/NotesApp.kt (composable Tab.CALENDAR).
// =============================================================================================

package com.example.notes.ui.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notes.data.BlockKind
import com.example.notes.data.EventEntity
import com.example.notes.data.TimeBlock
import com.example.notes.domain.TimeUtils
import com.example.notes.ui.appViewModel
import kotlinx.coroutines.launch

// ===============================================================================================
// MÀN HÌNH "LỊCH"
//   • Phần đầu: tiêu đề, nút Hôm nay / trước / sau, chọn chế độ Ngày-Tuần-Tháng   (CalendarHeader.kt)
//   • Chế độ Ngày và Tuần: lưới thời gian                                          (TimeGrid.kt)
//   • Chế độ Tháng: lưới các ô ngày + danh sách lịch của ngày đang chọn            (MonthView.kt)
//   • Nút "+" mở tờ giấy thêm lịch                                                 (EventEditorSheet.kt)
// ===============================================================================================

/**
 * Màn hình Lịch.
 *
 * @param onOpenTask gọi khi người dùng bấm một khối là CÔNG VIỆC; việc điều hướng sang form công việc
 *   do nav/NotesApp.kt quyết định (màn này chỉ "báo" id).
 * @param viewModel bộ não của màn; mặc định được tạo bởi `appViewModel { ... }` nên tự sống qua xoay máy.
 */
@Composable
fun CalendarScreen(
    onOpenTask: (Long) -> Unit,
    viewModel: CalendarViewModel = appViewModel { CalendarViewModel(it) }
) {
    // ----- Trạng thái -----
    // `state` là bản chụp mới nhất của ViewModel; mỗi khi ViewModel phát bản mới, màn tự vẽ lại.
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope() // để gọi hàm suspend từ sự kiện bấm

    // Sự kiện đang được soạn trong tờ giấy: null = tờ giấy đang đóng; id = 0 = thêm mới.
    // Đây là state CỤC BỘ (remember) chứ không nằm trong ViewModel, nên xoay máy sẽ làm tờ giấy đóng.
    var editingEvent by remember { mutableStateOf<EventEntity?>(null) }

    // ----- Hai hàm hỗ trợ cho sự kiện bấm -----

    /** Sự kiện mới mặc định: 9:00–10:00 của ngày đang chọn. */
    fun newEventTemplate(): EventEntity {
        val start = TimeUtils.at(state.selectedDay, 9 * 60)
        return EventEntity(title = "", startAt = start, endAt = start + 3_600_000L)
    }

    /** Bấm vào một khối: công việc -> mở form công việc; sự kiện -> mở tờ giấy sửa lịch. */
    fun openBlock(block: TimeBlock) {
        when (block.kind) {
            BlockKind.TASK -> onOpenTask(block.id)
            BlockKind.EVENT -> scope.launch { viewModel.loadEvent(block.id)?.let { editingEvent = it } }
        }
    }

    // ----- Khung màn hình: nút "+" nổi và nội dung chính -----
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingEvent = newEventTemplate() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Rounded.Add, contentDescription = "Thêm lịch") }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 1) Đầu trang (tiêu đề + chọn chế độ)
            CalendarHeader(state = state, viewModel = viewModel)

            // 2) Dải cảnh báo trùng giờ (chế độ Tháng đã có dấu hiệu riêng trong từng ô ngày nên không hiện dải)
            if (state.conflictKeys.isNotEmpty() && state.mode != CalendarMode.MONTH) {
                ConflictBanner(count = state.conflictKeys.size)
            }

            // 3) Phần thân: Tháng dùng MonthView, Ngày/Tuần dùng TimeGrid.
            if (state.mode == CalendarMode.MONTH) {
                MonthView(state = state, viewModel = viewModel, onBlockClick = ::openBlock)
            } else {
                TimeGrid(
                    firstDay = state.firstDay,
                    dayCount = state.dayCount,
                    selectedDay = state.selectedDay,
                    blocks = state.blocks,
                    conflictKeys = state.conflictKeys,
                    onSelectDay = viewModel::selectDay,
                    onBlockClick = ::openBlock,
                    onMove = viewModel::moveBlock
                )
            }
        }
    }

    // ----- Tờ giấy thêm / sửa lịch -----
    // Chỉ hiện khi `editingEvent` khác null. Nó không phải một "màn hình" riêng nên không đi qua NavHost:
    // mở = gán `editingEvent`, đóng = gán lại null.
    editingEvent?.let { event ->
        EventEditorSheet(
            initial = event,
            onDismiss = { editingEvent = null },
            onSave = viewModel::saveEvent,
            onDelete = { viewModel.deleteEvent(event.id) }
        )
    }
}
