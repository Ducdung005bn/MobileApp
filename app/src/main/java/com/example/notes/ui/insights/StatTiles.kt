// =============================================================================================
// FILE: ui/insights/StatTiles.kt
// VAI TRÒ: 3 ô số liệu nổi bật ở ĐẦU màn Thống kê (nhìn lướt là biết tình hình tuần này).
// NỘI DUNG:
//   • `StatTiles`: sắp 3 ô: hai ô cạnh nhau ở hàng trên, một ô rộng cả hàng ở dưới.
//   • `StatTile`: MỘT ô (biểu tượng màu, số lớn, nhãn nhỏ).
// XỬ LÝ:
//   • "Hoàn thành tuần này": `state.weekRate` (0..1) nhân 100 và bỏ phần thập phân.
//   • "Chuỗi ngày dài nhất": `state.bestStreak` = chuỗi dài nhất trong mọi thói quen.
//   • "Thời gian đã hoàn thành": `state.doneMs` (mili-giây) đổi bằng `Fmt.duration`; bằng 0 thì hiện "0".
//   • Các số tổng hợp này được TÍNH trong InsightsState (InsightsViewModel.kt); ô chỉ đọc và hiển thị.
//   • Ô không bấm được (không có callback).
// LIÊN QUAN: insights/InsightsScreen.kt (nơi gọi), insights/InsightsViewModel.kt (InsightsState).
// =============================================================================================

package com.example.notes.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.notes.domain.Category
import com.example.notes.ui.theme.WarningAmber
import com.example.notes.ui.theme.color
import com.example.notes.ui.util.Fmt

// ---------------------------------------------------------------------------------------------
// 3 ô số liệu ở đầu trang
// ---------------------------------------------------------------------------------------------

/** Bố cục 3 ô: [Hoàn thành tuần này][Chuỗi dài nhất] ở hàng trên, [Thời gian đã hoàn thành] ở hàng dưới. */
@Composable
internal fun StatTiles(state: InsightsState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // weight(1f) ở cả hai ô: mỗi ô chiếm một nửa bề rộng hàng.
            StatTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.TaskAlt,
                color = MaterialTheme.colorScheme.primary,
                value = "${(state.weekRate * 100).toInt()}%",
                label = "Hoàn thành tuần này"
            )
            StatTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.LocalFireDepartment,
                color = WarningAmber,
                value = "${state.bestStreak}",
                label = "Chuỗi ngày dài nhất"
            )
        }
        StatTile(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Rounded.Timer,
            color = Category.WORK.color,
            value = if (state.doneMs == 0L) "0" else Fmt.duration(state.doneMs),
            label = "Thời gian đã hoàn thành"
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Một ô số liệu
// ---------------------------------------------------------------------------------------------

/**
 * Một ô số liệu: nền tô nhạt theo [color], biểu tượng, số lớn ([value]) và nhãn nhỏ ([label]).
 * Nhận kích thước qua [modifier] nên nơi gọi quyết định ô rộng nửa hàng hay cả hàng.
 */
@Composable
private fun StatTile(modifier: Modifier, icon: ImageVector, color: Color, value: String, label: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
