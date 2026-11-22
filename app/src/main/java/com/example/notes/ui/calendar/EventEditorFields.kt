// =============================================================================================
// FILE: ui/calendar/EventEditorFields.kt
// VAI TRÒ: hai thành phần giao diện NHỎ, tái sử dụng được, chỉ phục vụ form lịch (EventEditorSheet.kt).
// Tách riêng để file form chính chỉ còn logic nhập liệu, dễ đọc hơn.
// NỘI DUNG:
//   • `ChipRow`: một hàng chip có thể vuốt ngang khi không đủ chỗ (dùng cho chọn danh mục và chọn thứ lặp).
//   • `SwitchRow`: một dòng "chữ bên trái + công tắc bật/tắt bên phải" (dùng cho "Cả ngày", "Lặp lại hằng tuần").
// XỬ LÝ:
//   • Cả hai đều "controlled": không tự giữ trạng thái. `SwitchRow` nhận `checked` từ cha và báo thay đổi
//     qua `onCheckedChange`; cha mới là nơi đổi giá trị (xem EventEditorSheet.kt).
//   • `ChipRow` nhận nội dung bằng tham số `content` (một lambda composable), nên nơi gọi tự quyết định
//     đặt chip nào vào trong.
// LIÊN QUAN: calendar/EventEditorSheet.kt (nơi gọi).
// =============================================================================================

package com.example.notes.ui.calendar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------------------------
// Hàng chip
// ---------------------------------------------------------------------------------------------

/**
 * Hàng chip cuộn ngang được.
 *
 * @param spacing khoảng cách giữa các chip (dp).
 * @param content các chip cần đặt vào hàng, ví dụ nhiều `FilterChip`.
 */
@Composable
internal fun ChipRow(spacing: Int = 8, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing.dp)
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------------------------
// Dòng có công tắc
// ---------------------------------------------------------------------------------------------

/**
 * Một dòng gồm chữ (và dòng phụ) bên trái, công tắc bật/tắt bên phải.
 *
 * @param checked trạng thái hiện tại của công tắc (do cha giữ).
 * @param onCheckedChange gọi với giá trị MỚI khi người dùng chạm công tắc.
 * @param subtitle dòng chữ nhỏ giải thích; null thì không hiện.
 */
@Composable
internal fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // weight(1f): cột chữ chiếm hết chỗ thừa, đẩy công tắc sang sát mép phải.
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
