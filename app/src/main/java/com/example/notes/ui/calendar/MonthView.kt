// =============================================================================================
// FILE: ui/calendar/MonthView.kt
// VAI TRÒ: giao diện chế độ THÁNG của màn Lịch (thay cho lưới giờ TimeGrid).
// NỘI DUNG:
//   • `MonthView`: toàn bộ chế độ Tháng = hàng tên thứ + lưới các ô ngày + danh sách lịch của ngày chọn.
//   • `MonthCell`: MỘT ô ngày trong lưới (số ngày + tối đa 3 chấm màu + gạch đỏ nếu trùng giờ).
// XỬ LÝ:
//   • Lưới có `state.dayCount / 7` hàng, mỗi hàng 7 ô. Ngày của ô = firstDay + hàng*7 + cột.
//   • Với mỗi ô, lọc ra các khối lịch giao với ngày đó (start < cuối ngày && end > đầu ngày).
//   • Ô thuộc tháng khác (đầu/cuối lưới) hiện số mờ đi; hôm nay tô đặc; ngày đang chọn có viền.
//   • Bấm một ô gọi `viewModel.selectDay(day)` -> state đổi -> danh sách bên dưới (DayAgenda.kt) đổi theo.
//   • Trùng giờ: nếu có ít nhất một khối của ngày nằm trong `state.conflictKeys` thì ô hiện gạch đỏ nhỏ.
// LIÊN QUAN: calendar/CalendarScreen.kt (nơi gọi), calendar/DayAgenda.kt (danh sách bên dưới lưới),
//            calendar/CalendarViewModel.kt (state).
// =============================================================================================

package com.example.notes.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.notes.data.TimeBlock
import com.example.notes.domain.TimeUtils
import com.example.notes.ui.theme.DangerRed
import com.example.notes.ui.theme.categoryColor
import com.example.notes.ui.util.Fmt
import java.time.LocalDate

// ---------------------------------------------------------------------------------------------
// Cả chế độ Tháng
// ---------------------------------------------------------------------------------------------

/**
 * Chế độ Tháng. Cả khối này cuộn dọc được (verticalScroll) vì lưới + danh sách có thể dài hơn màn hình.
 *
 * @param onBlockClick gọi khi bấm một dòng trong danh sách lịch của ngày (chuyển tiếp cho DayAgenda).
 */
@Composable
internal fun MonthView(
    state: CalendarState,
    viewModel: CalendarViewModel,
    onBlockClick: (TimeBlock) -> Unit
) {
    val today = remember { TimeUtils.today() }
    val shownMonth = LocalDate.ofEpochDay(state.selectedDay).monthValue // để làm mờ các ô thuộc tháng khác
    val weekRows = state.dayCount / 7 // số hàng tuần của lưới

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hàng tên thứ: T2 ... CN
        Row(modifier = Modifier.fillMaxWidth()) {
            for (label in Fmt.weekdayLabels) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Lưới các ô ngày: mỗi hàng 7 ô.
        repeat(weekRows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val day = state.firstDay + row * 7 + column
                    val dayStart = TimeUtils.dayStart(day)
                    val dayEnd = TimeUtils.dayEnd(day)
                    // Khối nào giao với ngày này (kể cả khối qua nửa đêm) thì thuộc về ô này.
                    val blocksOfDay = state.blocks.filter { it.start < dayEnd && it.end > dayStart }

                    MonthCell(
                        day = day,
                        isToday = day == today,
                        isSelected = day == state.selectedDay,
                        isInShownMonth = LocalDate.ofEpochDay(day).monthValue == shownMonth,
                        blocks = blocksOfDay,
                        hasConflict = blocksOfDay.any { it.key in state.conflictKeys },
                        onClick = { viewModel.selectDay(day) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Danh sách lịch của ngày đang chọn (file DayAgenda.kt)
        SelectedDayAgenda(state = state, onBlockClick = onBlockClick)

        Box(Modifier.height(80.dp)) // chừa chỗ cho nút "+" để nó không che dòng cuối
    }
}

// ---------------------------------------------------------------------------------------------
// Một ô ngày
// ---------------------------------------------------------------------------------------------

/**
 * Một ô ngày trong lưới tháng: số ngày + tối đa 3 chấm màu cho các lịch trong ngày.
 *
 * @param isInShownMonth false nếu ngày này thuộc tháng trước/sau (hiện mờ).
 * @param hasConflict true thì vẽ thêm gạch đỏ nhỏ ở đáy ô.
 */
@Composable
private fun MonthCell(
    day: Long,
    isToday: Boolean,
    isSelected: Boolean,
    isInShownMonth: Boolean,
    blocks: List<TimeBlock>,
    hasConflict: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val dayNumber = LocalDate.ofEpochDay(day).dayOfMonth

    // Màu số ngày: trắng/đen tuỳ nền nếu là hôm nay; mờ nếu thuộc tháng khác.
    val numberColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        isInShownMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    // Thứ tự modifier có ý nghĩa: clip -> border -> clickable để vùng bấm và viền đều bo theo `shape`.
    Column(
        modifier = modifier
            .padding(1.dp)
            .height(62.dp)
            .clip(shape)
            .then(if (isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Số ngày trong vòng tròn (tô đặc nếu là hôm nay)
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(dayNumber.toString(), style = MaterialTheme.typography.labelLarge, color = numberColor)
        }

        // Các chấm màu: mỗi chấm một lịch, nhiều hơn 3 thì ghi "+N".
        Row(
            modifier = Modifier.padding(top = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (block in blocks.take(3)) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(block.categoryColor()))
            }
            if (blocks.size > 3) {
                Text("+${blocks.size - 3}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Gạch đỏ nhỏ nếu ngày này có lịch trùng giờ.
        if (hasConflict) {
            Box(Modifier.padding(top = 3.dp).size(width = 14.dp, height = 3.dp).clip(CircleShape).background(DangerRed))
        }
    }
}
