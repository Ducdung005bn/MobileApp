// =============================================================================================
// FILE: ui/insights/ChartCard.kt
// VAI TRÒ: "khung thẻ" dùng chung cho mọi biểu đồ ở màn Thống kê, để các biểu đồ có cùng diện mạo.
// NỘI DUNG:
//   • `ChartCard`: một thẻ có tiêu đề, dòng phụ, (tuỳ chọn) một vùng nút bên phải tiêu đề, và phần nội dung.
// XỬ LÝ:
//   • Đây là mẫu "slot" của Compose: khung lo phần chung (nền, bo góc, tiêu đề), còn PHẦN RUỘT được nơi
//     gọi truyền vào qua tham số `content` (một lambda composable). Nhờ vậy cùng một khung chứa được
//     biểu đồ cột, biểu đồ tròn, heatmap...
//   • `header` cũng là một "slot" tuỳ chọn (có thể null): WeeklyCompletionCard dùng nó để đặt nút chuyển tuần.
// LIÊN QUAN: insights/InsightsScreen.kt, insights/WeeklyCompletionCard.kt (các nơi gọi).
// =============================================================================================

package com.example.notes.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------------------------
// Khung chung cho các biểu đồ
// ---------------------------------------------------------------------------------------------

/**
 * Thẻ có tiêu đề, dòng phụ, (tuỳ chọn) một nút bên phải tiêu đề, và nội dung bên dưới.
 *
 * @param title tiêu đề thẻ.
 * @param subtitle dòng chữ nhỏ dưới tiêu đề; null thì không hiện.
 * @param header vùng bên phải tiêu đề (ví dụ nút ‹ ›); null thì không có.
 * @param content phần ruột của thẻ (biểu đồ, chữ...), đặt bên dưới tiêu đề.
 */
@Composable
internal fun ChartCard(
    title: String,
    subtitle: String? = null,
    header: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Hàng tiêu đề: chữ bên trái (chiếm hết chỗ thừa), `header` bên phải.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                header?.invoke()
            }
            // Phần ruột do nơi gọi quyết định.
            content()
        }
    }
}
