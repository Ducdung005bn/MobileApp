// =============================================================================================
// FILE: ui/insights/HabitCard.kt
// VAI TRÒ: thẻ hiển thị MỘT thói quen trong danh sách "Thói quen" của màn Thống kê, và các mảnh con tạo nên nó.
// NỘI DUNG:
//   • `HabitCard`: cả thẻ (bấm vào thẻ = mở hộp thoại sửa thói quen).
//   • `StreakLine`: dòng "🔥 5 ngày • dài nhất 12 • 85%/30 ngày".
//   • `TodayCheckButton`: nút tròn lớn để tick/bỏ tick HÔM NAY.
//   • `LastSevenDays`: dải 7 vòng tròn nhỏ cho 7 ngày gần nhất, bấm từng ngày để đánh dấu bù.
// XỬ LÝ:
//   • Thẻ chỉ ĐỌC `HabitWithStats` (đã có sẵn chuỗi ngày, tỉ lệ 30 ngày, tập ngày đã làm). Mọi con số được
//     tính ở lớp dữ liệu/domain (HabitRepository, domain/HabitLogic.kt); thẻ không tự tính chuỗi.
//   • Bấm nút tick hoặc một vòng tròn ngày gọi `onToggle(day)`; InsightsScreen chuyển tiếp thành
//     `viewModel.toggleHabit(habitId, day)`. Database đổi -> state mới -> thẻ vẽ lại.
//   • Màu của vòng tròn trong dải 7 ngày: đặc = đã làm; nhạt = ngày có lịch nhưng chưa làm; xám = ngày không
//     nằm trong lịch của thói quen (so khớp bằng `TimeUtils.isDayInMask`).
//   • Màu chủ đạo của thẻ lấy từ danh mục của thói quen (`Category.fromKey(habit.category).color`).
// LIÊN QUAN: insights/InsightsScreen.kt (nơi gọi), insights/HabitIcons.kt (biểu tượng), insights/HabitDialog.kt
//            (hộp thoại sửa), insights/InsightsViewModel.kt (toggleHabit), domain/HabitLogic.kt (tính chuỗi).
// =============================================================================================

package com.example.notes.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.notes.data.HabitWithStats
import com.example.notes.domain.Category
import com.example.notes.domain.TimeUtils
import com.example.notes.ui.theme.WarningAmber
import com.example.notes.ui.theme.color
import com.example.notes.ui.util.Fmt

// ---------------------------------------------------------------------------------------------
// Cả thẻ thói quen
// ---------------------------------------------------------------------------------------------

/**
 * Thẻ một thói quen: biểu tượng, tên, chuỗi ngày, nút tick hôm nay, và dải 7 ngày gần nhất
 * (bấm vào từng ngày để đánh dấu bù).
 *
 * @param stats thói quen kèm số liệu thống kê (chuỗi, tỉ lệ, các ngày đã làm).
 * @param today epoch-day của hôm nay.
 * @param onToggle tick/bỏ tick thói quen vào một ngày (tham số là epoch-day của ngày đó).
 * @param onEdit bấm vào thẻ: mở hộp thoại sửa.
 */
@Composable
internal fun HabitCard(
    stats: HabitWithStats,
    today: Long,
    onToggle: (Long) -> Unit,
    onEdit: () -> Unit
) {
    val habit = stats.habit
    val color = Category.fromKey(habit.category).color

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onEdit
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Hàng trên: [biểu tượng] [tên + chuỗi ngày] [nút tick hôm nay]
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Biểu tượng tròn
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(HabitIcons.of(habit.icon), contentDescription = null, tint = color)
                }

                // Tên + chuỗi ngày
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(habit.name, style = MaterialTheme.typography.titleSmall)
                    StreakLine(stats)
                }

                // Nút tick lớn cho hôm nay
                TodayCheckButton(done = stats.doneToday, color = color, onClick = { onToggle(today) })
            }

            // Hàng dưới: dải 7 ngày gần nhất
            LastSevenDays(stats = stats, today = today, color = color, onToggle = onToggle)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Các mảnh con của thẻ
// ---------------------------------------------------------------------------------------------

/** "🔥 5 ngày • dài nhất 12 • 85%/30 ngày". Ngọn lửa và chữ chuyển sang màu cam khi chuỗi hiện tại > 0. */
@Composable
private fun StreakLine(stats: HabitWithStats) {
    val hasStreak = stats.currentStreak > 0

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            Icons.Rounded.LocalFireDepartment,
            contentDescription = null,
            tint = if (hasStreak) WarningAmber else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
        Text(
            "${stats.currentStreak} ngày",
            style = MaterialTheme.typography.labelLarge,
            color = if (hasStreak) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "• dài nhất ${stats.longestStreak} • ${(stats.rate30Days * 100).toInt()}%/30 ngày",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Nút tròn tick hôm nay: rỗng có viền khi chưa làm, tô đặc kèm dấu ✓ khi đã làm. */
@Composable
private fun TodayCheckButton(done: Boolean, color: Color, onClick: () -> Unit) {
    // Thứ tự modifier: size -> clip(tròn) -> background -> border -> clickable (vùng bấm cũng tròn).
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (done) color else Color.Transparent)
            .border(2.dp, color, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (done) Icon(Icons.Rounded.Check, contentDescription = "Đã làm hôm nay", tint = Color.White)
    }
}

/**
 * 7 vòng tròn nhỏ cho 7 ngày gần nhất (cũ -> mới):
 * đặc = đã làm; nhạt = ngày có lịch nhưng chưa làm; xám = ngày không nằm trong lịch của thói quen.
 * Bấm vào một vòng tròn để tick/bỏ tick ngày đó (đánh dấu bù).
 */
@Composable
private fun LastSevenDays(stats: HabitWithStats, today: Long, color: Color, onToggle: (Long) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        // 6 downTo 0: bắt đầu từ 6 ngày trước, kết thúc ở hôm nay (cũ -> mới, từ trái sang phải).
        for (daysAgo in 6 downTo 0) {
            val day = today - daysAgo
            val isDone = day in stats.doneDays
            val isScheduled = TimeUtils.isDayInMask(stats.habit.daysMask, day)

            val circleColor = when {
                isDone -> color
                isScheduled -> color.copy(alpha = 0.12f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }

            // Mỗi ngày: chữ thứ bên trên, vòng tròn bên dưới.
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    Fmt.weekdayShort(day),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(circleColor)
                        .clickable { onToggle(day) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}
