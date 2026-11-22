// =============================================================================================
// FILE: ui/calendar/CalendarHeader.kt
// VAI TRÒ: phần ĐẦU TRANG của màn Lịch, nằm phía trên lưới giờ / lưới tháng.
// NỘI DUNG:
//   • `CalendarHeader`: chữ "Lịch" + dòng phụ (ngày/tuần/tháng đang xem), nút "Hôm nay", hai mũi tên ‹ ›,
//     và ba nút Ngày | Tuần | Tháng.
//   • `ConflictBanner`: dải đỏ báo "N mục đang trùng giờ".
//   • `CalendarMode.title()`: đổi giá trị enum (DAY/WEEK/MONTH) thành chữ hiển thị.
// XỬ LÝ:
//   • Các nút ở đây KHÔNG tự đổi gì. Chúng chỉ gọi hàm của ViewModel (setMode, step, goToday); ViewModel
//     đổi state, rồi CalendarScreen vẽ lại với `state` mới. (Mô hình "state đi xuống, sự kiện đi lên".)
//   • Dòng phụ dưới chữ "Lịch" đổi theo chế độ: Ngày -> "Thứ Bảy, 24 tháng 10"; Tuần -> "19/10 – 25/10"; Tháng -> "Tháng 10, 2026".
//   • ConflictBanner chỉ được CalendarScreen vẽ ở chế độ Ngày/Tuần (chế độ Tháng có dấu riêng trong ô ngày).
// LIÊN QUAN: calendar/CalendarScreen.kt (nơi gọi), calendar/CalendarViewModel.kt (state + hành động).
// =============================================================================================

package com.example.notes.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.notes.ui.theme.DangerRed
import com.example.notes.ui.util.Fmt

// ---------------------------------------------------------------------------------------------
// Tên hiển thị của ba chế độ xem
// ---------------------------------------------------------------------------------------------

/** Đổi enum thành chữ trên nút: DAY -> "Ngày", WEEK -> "Tuần", MONTH -> "Tháng". */
private fun CalendarMode.title(): String = when (this) {
    CalendarMode.DAY -> "Ngày"
    CalendarMode.WEEK -> "Tuần"
    CalendarMode.MONTH -> "Tháng"
}

// ---------------------------------------------------------------------------------------------
// Đầu trang: tiêu đề, điều hướng ngày, chọn chế độ
// ---------------------------------------------------------------------------------------------

/**
 * Đầu trang gồm hai hàng: (1) tiêu đề + nút điều hướng, (2) ba nút chọn chế độ xem.
 *
 * @param state bản chụp trạng thái hiện tại (chỉ ĐỌC từ đây: mode, selectedDay, firstDay).
 * @param viewModel dùng để GỌI hành động khi người dùng bấm (setMode, step, goToday).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarHeader(state: CalendarState, viewModel: CalendarViewModel) {
    // Dòng phụ dưới chữ "Lịch" thay đổi theo chế độ xem.
    val subtitle = when (state.mode) {
        CalendarMode.DAY -> Fmt.dayTitle(state.selectedDay)
        CalendarMode.WEEK -> "${Fmt.shortDay(state.firstDay)} – ${Fmt.shortDay(state.firstDay + 6)}"
        CalendarMode.MONTH -> Fmt.monthTitle(state.selectedDay)
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Hàng 1: tiêu đề bên trái (weight 1f = chiếm hết chỗ thừa), ba nút bên phải.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Lịch", style = MaterialTheme.typography.headlineMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = viewModel::goToday) { Text("Hôm nay") }
            IconButton(onClick = { viewModel.step(-1) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Trước")
            }
            IconButton(onClick = { viewModel.step(1) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Sau")
            }
        }

        // Hàng 2: Ba nút Ngày | Tuần | Tháng.
        // "Chọn đúng một" do code này quyết định: nút nào có `state.mode == mode` thì được tô sáng.
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            CalendarMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.mode == mode,
                    onClick = { viewModel.setMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = CalendarMode.entries.size)
                ) { Text(mode.title()) }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Dải cảnh báo trùng giờ
// ---------------------------------------------------------------------------------------------

/**
 * Dải cảnh báo đỏ: có bao nhiêu mục đang trùng giờ.
 * Số mục lấy từ `state.conflictKeys.size`; việc TÍNH ra khối nào trùng nằm ở Scheduling.conflictKeys.
 */
@Composable
internal fun ConflictBanner(count: Int) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp)) // cắt bo góc TRƯỚC khi tô nền để nền cũng bo góc
            .background(DangerRed.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
        Text(
            "$count mục đang trùng giờ — nhấn giữ để kéo dời",
            style = MaterialTheme.typography.labelMedium,
            color = DangerRed
        )
    }
}
