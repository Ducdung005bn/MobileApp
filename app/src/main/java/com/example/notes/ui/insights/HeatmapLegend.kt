// =============================================================================================
// FILE: ui/insights/HeatmapLegend.kt
// VAI TRÒ: dòng chú thích "Ít ▫▫▫▫ Nhiều" đặt ngay dưới heatmap hoạt động, giải thích đậm nhạt nghĩa là gì.
// NỘI DUNG:
//   • `HeatmapLegend`: chữ "Ít", bốn ô vuông nhỏ đậm dần, chữ "Nhiều", căn sát mép phải.
// XỬ LÝ:
//   • Bốn ô dùng độ trong suốt 0 / 0,35 / 0,65 / 1 của màu chủ đạo; ô đầu (0) dùng màu nền xám nhạt để thể
//     hiện "không có hoạt động". Đây chỉ là chú thích GẦN ĐÚNG: ActivityHeatmap thật tô ô ngày theo thang liên
//     tục từ 30% đến 100% (tuỳ số hoạt động so với ngày nhiều nhất), không chia đúng bốn mức.
//   • Chỉ hiển thị, không có tương tác.
// LIÊN QUAN: insights/InsightsScreen.kt (đặt ngay sau ActivityHeatmap), components/Charts.kt (ActivityHeatmap).
// =============================================================================================

package com.example.notes.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------------------------
// Chú thích heatmap
// ---------------------------------------------------------------------------------------------

/** Chú thích "Ít ... Nhiều" bên dưới heatmap. */
@Composable
internal fun HeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End, // dồn sang phải
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Ít", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Bốn ô từ nhạt đến đậm.
        for (alpha in listOf(0f, 0.35f, 0.65f, 1f)) {
            Box(
                Modifier
                    .padding(start = 4.dp)
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (alpha == 0f) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    )
            )
        }
        Text("  Nhiều", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
