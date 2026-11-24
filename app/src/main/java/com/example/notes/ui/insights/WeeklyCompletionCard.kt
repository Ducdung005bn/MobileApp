// =============================================================================================
// FILE: ui/insights/WeeklyCompletionCard.kt
// VAI TRÒ: thẻ "Hoàn thành công việc": số việc đã xong/đến hạn trong tuần và biểu đồ cột theo từng ngày.
// NỘI DUNG:
//   • `WeeklyCompletionCard`: thẻ gồm tiêu đề (khoảng tuần), nút chuyển tuần, dòng tổng kết và biểu đồ cột.
// XỬ LÝ:
//   • Nút ‹ › gọi `viewModel.shiftWeek(-1/+1)`; nút "Tuần này" gọi `viewModel.thisWeek()` và chỉ hiện khi đang
//     xem tuần khác (`isCurrentWeek == false`). ViewModel đổi `weekStart` rồi tải lại dữ liệu tuần đó.
//   • Dòng tổng kết: "đã xong/đến hạn việc • phần trăm". Tuần không có việc nào có hạn chót thì hiện lời giải thích.
//   • Biểu đồ cột (`WeeklyBarChart`) vẽ bằng Canvas ở components/Charts.kt; thẻ này chỉ đưa dữ liệu vào.
// LIÊN QUAN: insights/InsightsScreen.kt (nơi gọi), insights/ChartCard.kt (khung thẻ),
//            insights/InsightsViewModel.kt (shiftWeek, thisWeek), components/Charts.kt (WeeklyBarChart).
// =============================================================================================

package com.example.notes.ui.insights

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.example.notes.domain.TimeUtils
import com.example.notes.ui.components.WeeklyBarChart
import com.example.notes.ui.util.Fmt

// ---------------------------------------------------------------------------------------------
// Hoàn thành công việc trong tuần
// ---------------------------------------------------------------------------------------------

/**
 * Thẻ "Hoàn thành công việc" của tuần đang xem.
 *
 * @param state dữ liệu để vẽ (weekStart, weekDone, weekDue, weekRate, weekly...).
 * @param viewModel chỉ dùng để GỌI hành động chuyển tuần.
 */
@Composable
internal fun WeeklyCompletionCard(state: InsightsState, viewModel: InsightsViewModel) {
    // Đang xem đúng tuần hiện tại? (so sánh thứ Hai của tuần đang xem với thứ Hai của tuần chứa hôm nay)
    val isCurrentWeek = state.weekStart == TimeUtils.weekStart(state.today)

    ChartCard(
        title = "Hoàn thành công việc",
        subtitle = "${Fmt.shortDay(state.weekStart)} – ${Fmt.shortDay(state.weekStart + 6)}",
        // Slot `header`: các nút điều hướng tuần đặt bên phải tiêu đề.
        header = {
            // Nút "Tuần này" (khi đang xem tuần khác) và hai mũi tên chuyển tuần.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isCurrentWeek) TextButton(onClick = viewModel::thisWeek) { Text("Tuần này") }
                IconButton(onClick = { viewModel.shiftWeek(-1) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Tuần trước")
                }
                IconButton(onClick = { viewModel.shiftWeek(1) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Tuần sau")
                }
            }
        }
    ) {
        // Slot `content`: dòng tổng kết rồi biểu đồ cột.
        if (state.weekDue == 0) {
            Text(
                "Tuần này chưa có công việc nào có hạn chót.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "${state.weekDone}/${state.weekDue} việc • ${(state.weekRate * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge
            )
        }
        WeeklyBarChart(days = state.weekly, todayEpochDay = state.today)
    }
}
