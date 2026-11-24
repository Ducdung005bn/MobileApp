// =============================================================================================
// FILE: ui/insights/CategoryTimeCharts.kt
// VAI TRÒ: hai khối thống kê "thời gian theo danh mục" (Học tập, Công việc, Cá nhân...).
// NỘI DUNG:
//   • `TimeByCategory`: biểu đồ TRÒN + chú thích (chấm màu, tên, thời gian, phần trăm).
//   • `PlanVsActual`: mỗi danh mục một hàng với hai thanh: nhạt = đã xếp giờ, đậm = đã hoàn thành.
// XỬ LÝ:
//   • Thời gian tính từ CÔNG VIỆC ĐÃ XẾP GIỜ trên lịch (tổng thời lượng khối giờ), không phải thời gian bấm giờ.
//   • `TimeByCategory`: gom tổng ms của từng danh mục (danh mục không có dữ liệu = 0); tổng bằng 0 thì hiện lời
//     hướng dẫn rồi dừng (`return`). Phần trăm = ms * 100 / tổng.
//   • `PlanVsActual`: chỉ giữ danh mục có dữ liệu; `maxMs` là giá trị lớn nhất để mọi thanh cùng một tỉ lệ.
//   • Việc VẼ hình tròn và thanh nằm ở components/Charts.kt (DonutChart, PlanActualBars, LegendDot).
// LIÊN QUAN: insights/InsightsScreen.kt (nơi gọi, đặt trong ChartCard), data/InsightsRepository.kt (tính dữ liệu),
//            components/Charts.kt.
// =============================================================================================

package com.example.notes.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.notes.domain.Category
import com.example.notes.ui.components.DonutChart
import com.example.notes.ui.components.LegendDot
import com.example.notes.ui.components.PlanActualBars
import com.example.notes.ui.theme.color
import com.example.notes.ui.theme.label
import com.example.notes.ui.util.Fmt

// ---------------------------------------------------------------------------------------------
// Thời gian theo danh mục (biểu đồ tròn)
// ---------------------------------------------------------------------------------------------

/** Biểu đồ tròn thời gian theo danh mục, ở giữa ghi tổng; bên phải là chú thích từng danh mục. */
@Composable
internal fun TimeByCategory(state: InsightsState) {
    // Thời gian (ms) của từng danh mục; danh mục không có dữ liệu = 0.
    val totals = Category.entries.map { category ->
        category to (state.timeByCategory.firstOrNull { it.category == category.name }?.totalMs ?: 0L)
    }
    val grandTotal = totals.sumOf { it.second }

    // Chưa có dữ liệu thì chỉ hiện lời hướng dẫn, không vẽ biểu đồ.
    if (grandTotal == 0L) {
        Text(
            "Chưa có dữ liệu. Xếp giờ công việc trên lịch và tick hoàn thành để xem biểu đồ.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        // Biểu đồ tròn, ở giữa ghi tổng thời gian
        DonutChart(segments = totals.map { (category, ms) -> category.color to ms.toFloat() }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Fmt.duration(grandTotal), style = MaterialTheme.typography.titleMedium)
                Text("tổng", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Chú thích: chấm màu + tên + thời gian + phần trăm
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((category, ms) in totals) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(category.color, category.label, modifier = Modifier.weight(1f))
                    Text(
                        text = if (ms == 0L) "–" else "${Fmt.duration(ms)} · ${ms * 100 / grandTotal}%",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Kế hoạch so với thực tế (thanh ngang)
// ---------------------------------------------------------------------------------------------

/**
 * Mỗi danh mục một hàng: "đã hoàn thành / đã xếp giờ" bằng chữ, kèm hai thanh so sánh.
 * Thanh nhạt = thời gian đã xếp giờ (kế hoạch); thanh đậm = thời gian đã hoàn thành (thực tế).
 */
@Composable
internal fun PlanVsActual(state: InsightsState) {
    // Chỉ hiện các danh mục có việc đã xếp giờ.
    val rows = state.planVsActual.filter { it.plannedMs > 0 || it.actualMs > 0 }

    if (rows.isEmpty()) {
        Text(
            "Xếp giờ công việc trên lịch và tick hoàn thành để so sánh.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    // Giá trị lớn nhất để các thanh cùng tỉ lệ với nhau.
    val maxMs = rows.maxOf { maxOf(it.plannedMs, it.actualMs) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        for (row in rows) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Dòng chữ: tên danh mục bên trái, "thực tế / kế hoạch" bên phải.
                Row {
                    Text(row.category.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text(
                        "${Fmt.duration(row.actualMs)} / ${Fmt.duration(row.plannedMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PlanActualBars(plannedMs = row.plannedMs, actualMs = row.actualMs, maxMs = maxMs, color = row.category.color)
            }
        }
    }
}
