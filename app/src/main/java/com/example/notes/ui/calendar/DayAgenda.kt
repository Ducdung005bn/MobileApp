// =============================================================================================
// FILE: ui/calendar/DayAgenda.kt
// VAI TRÒ: "danh sách lịch của ngày đang chọn", nằm DƯỚI lưới tháng (chỉ dùng ở chế độ Tháng).
// NỘI DUNG:
//   • `SelectedDayAgenda`: tiêu đề ngày + danh sách các dòng lịch, hoặc thông báo "Ngày trống".
//   • `AgendaRow`: MỘT dòng lịch (vạch màu, tên, giờ, nhãn Công việc/Lặp/Trùng giờ).
// XỬ LÝ:
//   • Lọc `state.blocks` lấy các khối giao với ngày đang chọn (cùng công thức với MonthView).
//   • Bấm một dòng gọi `onBlockClick(block)`: CalendarScreen sẽ mở form công việc (nếu là công việc)
//     hoặc form sửa lịch (nếu là sự kiện).
//   • Nhãn "Trùng giờ" hiện khi khoá của khối nằm trong `state.conflictKeys`.
// LIÊN QUAN: calendar/MonthView.kt (nơi gọi), calendar/CalendarScreen.kt (xử lý bấm).
// =============================================================================================

package com.example.notes.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.notes.data.BlockKind
import com.example.notes.data.TimeBlock
import com.example.notes.domain.TimeUtils
import com.example.notes.ui.components.EmptyState
import com.example.notes.ui.components.Pill
import com.example.notes.ui.theme.DangerRed
import com.example.notes.ui.theme.categoryColor
import com.example.notes.ui.util.Fmt

// ---------------------------------------------------------------------------------------------
// Danh sách của ngày đang chọn
// ---------------------------------------------------------------------------------------------

/** Danh sách lịch của ngày đang chọn, nằm dưới lưới tháng. Không có lịch thì hiện [EmptyState]. */
@Composable
internal fun SelectedDayAgenda(state: CalendarState, onBlockClick: (TimeBlock) -> Unit) {
    // Khối "thuộc ngày này" = giao với khoảng [đầu ngày, cuối ngày).
    val dayStart = TimeUtils.dayStart(state.selectedDay)
    val dayEnd = TimeUtils.dayEnd(state.selectedDay)
    val agenda = state.blocks.filter { it.start < dayEnd && it.end > dayStart }

    // Tiêu đề ngày, ví dụ "Thứ Bảy, 24 tháng 10".
    Text(
        Fmt.dayTitle(state.selectedDay),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
    )

    if (agenda.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.EventAvailable,
            title = "Ngày trống",
            subtitle = "Chưa có lịch nào. Bấm + để thêm lịch học, lịch làm hoặc cuộc hẹn."
        )
    } else {
        for (block in agenda) {
            AgendaRow(block = block, hasConflict = block.key in state.conflictKeys, onClick = { onBlockClick(block) })
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Một dòng trong danh sách
// ---------------------------------------------------------------------------------------------

/**
 * Một dòng trong danh sách lịch của ngày: vạch màu, tên, giờ và các nhãn.
 * Bố cục: [vạch màu] [tên + giờ (chiếm hết chỗ thừa)] [nhãn Công việc] [nhãn Lặp] [nhãn Trùng giờ].
 */
@Composable
internal fun AgendaRow(block: TimeBlock, hasConflict: Boolean, onClick: () -> Unit) {
    val color = block.categoryColor()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Vạch màu theo danh mục
            Box(Modifier.size(width = 4.dp, height = 36.dp).clip(CircleShape).background(color))

            // Tên và giờ
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(block.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    text = if (block.allDay) "Cả ngày" else "${Fmt.time(block.start)} – ${Fmt.time(block.end)}  •  ${Fmt.duration(block.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Các nhãn nhỏ bên phải (mỗi nhãn chỉ hiện khi đúng điều kiện)
            if (block.kind == BlockKind.TASK) Pill("Công việc", color)
            if (block.isRepeating) Pill("Lặp", color)
            if (hasConflict) Pill("Trùng giờ", DangerRed, icon = Icons.Rounded.Warning)
        }
    }
}
