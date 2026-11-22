// =============================================================================================
// FILE: ui/calendar/TimeGridFrame.kt
// VAI TRÒ: phần "KHUNG" bao quanh lưới giờ: hàng tiêu đề phía TRÊN, hàng cả ngày phía trên nữa, và cột
// nhãn giờ phía TRÁI. Không chứa các khối lịch (chúng nằm trong DayColumn.kt / EventBlock.kt).
// NỘI DUNG:
//   • `DayHeader`: ô tiêu đề một ngày (tên thứ "T2" + số ngày trong vòng tròn). Bấm để chọn ngày.
//   • `AllDayRow`: hàng sự kiện "cả ngày" (mỗi cột tối đa 2 nhãn màu, bấm để mở).
//   • `HourLabels`: cột nhãn giờ 01:00 ... 23:00.
// XỬ LÝ:
//   • Hôm nay: chữ thứ và vòng tròn số ngày tô màu chủ đạo. Ngày đang chọn (không phải hôm nay): viền tròn.
//   • AllDayRow lọc khối của từng ngày bằng điều kiện giao khoảng: start < cuối ngày && end > đầu ngày.
//   • HourLabels dùng `HourHeight` từ TimeGrid.kt nên mỗi nhãn luôn thẳng hàng với đường kẻ giờ.
// LIÊN QUAN: calendar/TimeGrid.kt (nơi gọi và định nghĩa HourHeight, GutterWidth).
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.notes.data.TimeBlock
import com.example.notes.domain.TimeUtils
import com.example.notes.ui.theme.categoryColor
import com.example.notes.ui.util.Fmt
import java.time.LocalDate

// ---------------------------------------------------------------------------------------------
// Tiêu đề một ngày
// ---------------------------------------------------------------------------------------------

/** Một ô tiêu đề: tên thứ ("T2") và số ngày trong vòng tròn (tô đặc nếu là hôm nay). */
@Composable
internal fun DayHeader(day: Long, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val dayOfMonth = LocalDate.ofEpochDay(day).dayOfMonth

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tên thứ
        Text(
            Fmt.weekdayShort(day),
            style = MaterialTheme.typography.labelMedium,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Số ngày. Thứ tự modifier: size -> clip(tròn) -> background -> border(viền nếu được chọn).
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent)
                .then(
                    // Ngày đang chọn (không phải hôm nay) có viền tròn.
                    if (isSelected && !isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                dayOfMonth.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Hàng sự kiện cả ngày
// ---------------------------------------------------------------------------------------------

/** Hàng các sự kiện "cả ngày": mỗi cột hiện tối đa 2 nhãn màu. */
@Composable
internal fun AllDayRow(days: List<Long>, allDayBlocks: List<TimeBlock>, onBlockClick: (TimeBlock) -> Unit) {
    // padding(start = GutterWidth): chừa chỗ cho cột nhãn giờ để các cột thẳng hàng với DayHeader bên trên.
    Row(modifier = Modifier.fillMaxWidth().padding(start = GutterWidth, top = 2.dp)) {
        for (day in days) {
            val dayStart = TimeUtils.dayStart(day)
            val dayEnd = TimeUtils.dayEnd(day)
            val blocksOfDay = allDayBlocks.filter { it.start < dayEnd && it.end > dayStart }

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 1.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (block in blocksOfDay.take(2)) {
                    Text(
                        text = block.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(block.categoryColor())
                            .clickable { onBlockClick(block) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Cột nhãn giờ bên trái
// ---------------------------------------------------------------------------------------------

/** Cột nhãn giờ bên trái: 01:00, 02:00, ... 23:00 (giờ 0 để trống vì nằm sát mép trên). */
@Composable
internal fun HourLabels() {
    Column(modifier = Modifier.width(GutterWidth)) {
        for (hour in 0 until 24) {
            // Mỗi ô cao đúng một giờ (HourHeight) nên nhãn luôn thẳng hàng với đường kẻ.
            Box(modifier = Modifier.height(HourHeight), contentAlignment = Alignment.TopEnd) {
                Text(
                    text = if (hour == 0) "" else "%02d:00".format(hour),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Đẩy chữ lên nửa dòng để căn giữa vào đường kẻ giờ.
                    modifier = Modifier.padding(end = 6.dp).offset(y = (-6).dp)
                )
            }
        }
    }
}
