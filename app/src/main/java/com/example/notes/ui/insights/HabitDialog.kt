// =============================================================================================
// FILE: ui/insights/HabitDialog.kt
// VAI TRÒ: hộp thoại THÊM / SỬA một thói quen (AlertDialog) và hộp xác nhận xoá đi kèm.
// NỘI DUNG:
//   • `HabitDialog`: một hàm composable duy nhất, vẽ hộp thoại chính, và (khi cần) hộp xác nhận xoá.
// XỬ LÝ:
//   • Các ô là trạng thái cục bộ: tên, biểu tượng, danh mục (đổi danh mục thì màu biểu tượng đổi theo),
//     các thứ lặp (chip bật/tắt từng bit, mặc định mọi ngày). Nút "Lưu" bị khoá khi tên trống hoặc không
//     chọn thứ nào. Khi đang sửa có thêm nút "Lưu trữ" và "Xoá".
//   • Thêm hay sửa chỉ khác ở tham số: `initial == null` là thêm mới; `onArchive`/`onDelete` chỉ khác null khi sửa.
//   • Lưu: lấy thói quen gốc (hoặc tạo mới), `copy` các trường đã nhập, gọi `onSave` rồi `onDismiss`.
//     Việc kiểm tra hợp lệ và ghi database làm ở ViewModel/Repository, không làm ở đây.
//   • Các thứ lặp lưu trong một số nguyên (bitmask): bit 0 = T2 ... bit 6 = CN. `xor bit` đảo trạng thái một
//     thứ; `daysMask and bit != 0` hỏi "thứ này có đang bật không".
// BACK / ĐÓNG:
//   • Hộp thoại chính: Back, chạm ra ngoài hoặc "Huỷ" gọi `onDismiss`; không lưu, các ô mất.
//     "Lưu" gọi onSave rồi onDismiss; "Lưu trữ" gọi onArchive rồi onDismiss.
//   • "Xoá" không xoá ngay mà đặt `showDeleteConfirm = true` để hiện hộp xác nhận thứ hai đè lên. Ở hộp
//     xác nhận này, Back/chạm ngoài/"Huỷ" chỉ đặt `showDeleteConfirm = false`: đóng hộp xác nhận,
//     hộp sửa thói quen vẫn mở. "Xoá" ở hộp xác nhận mới gọi onDelete rồi đóng cả hai.
// LIÊN QUAN: insights/InsightsScreen.kt (mở/đóng), insights/HabitIcons.kt (IconPicker),
//            insights/InsightsViewModel.kt (saveHabit, archiveHabit, deleteHabit).
// =============================================================================================

package com.example.notes.ui.insights

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.notes.data.HabitEntity
import com.example.notes.domain.Category
import com.example.notes.ui.theme.DangerRed
import com.example.notes.ui.theme.color
import com.example.notes.ui.theme.icon
import com.example.notes.ui.theme.label
import com.example.notes.ui.util.Fmt

// ---------------------------------------------------------------------------------------------
// Hộp thoại thêm / sửa thói quen
// ---------------------------------------------------------------------------------------------

/**
 * @param initial thói quen đang sửa; null = tạo mới
 * @param onDismiss đóng hộp thoại (InsightsScreen đặt `habitDialog = null`)
 * @param onSave gửi thói quen đã chỉnh ra ngoài để lưu
 * @param onArchive / onDelete chỉ có khi đang sửa
 */
@Composable
fun HabitDialog(
    initial: HabitEntity?,
    onDismiss: () -> Unit,
    onSave: (HabitEntity) -> Unit,
    onArchive: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    // ----- Giá trị các ô trong form -----
    // Khởi tạo từ thói quen đang sửa, hoặc giá trị mặc định khi tạo mới.
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var iconKey by remember { mutableStateOf(initial?.icon?.ifBlank { "book" } ?: "book") }
    var category by remember { mutableStateOf(Category.fromKey(initial?.category)) }
    var daysMask by remember { mutableIntStateOf(initial?.daysMask ?: 0b1111111) } // mặc định: mọi ngày
    var showDeleteConfirm by remember { mutableStateOf(false) } // true = đang hiện hộp xác nhận xoá

    // ----- Hộp thoại chính -----
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Thói quen mới" else "Sửa thói quen") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Tên thói quen
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tên thói quen") },
                    placeholder = { Text("Đọc sách, tập thể dục…") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                // Chọn biểu tượng (HabitIcons.kt)
                Text("Biểu tượng", style = MaterialTheme.typography.labelLarge)
                IconPicker(selectedKey = iconKey, accent = category.color, onSelect = { iconKey = it })

                // Chọn danh mục
                Text("Danh mục", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (option in Category.entries) {
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(option.label) },
                            leadingIcon = { Icon(option.icon, null, tint = option.color, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                // Chọn các thứ lặp lại
                Text("Lặp lại vào", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Fmt.weekdayLabels.forEachIndexed { index, label ->
                        val bit = 1 shl index // T2 = bit 0, ..., CN = bit 6
                        FilterChip(
                            selected = daysMask and bit != 0,
                            onClick = { daysMask = daysMask xor bit }, // bật/tắt bit này
                            label = { Text(label) }
                        )
                    }
                }

                // Các nút chỉ có khi đang sửa
                if (initial != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (onArchive != null) {
                            TextButton(onClick = { onArchive(); onDismiss() }) { Text("Lưu trữ") }
                        }
                        if (onDelete != null) {
                            // Chưa xoá ngay: chỉ bật cờ để hiện hộp xác nhận bên dưới.
                            TextButton(onClick = { showDeleteConfirm = true }) { Text("Xoá", color = DangerRed) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && daysMask != 0, // cần có tên và ít nhất một ngày
                onClick = {
                    val base = initial ?: HabitEntity(name = name)
                    onSave(base.copy(name = name.trim(), icon = iconKey, category = category.name, daysMask = daysMask))
                    onDismiss()
                }
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } }
    )

    // ----- Hộp xác nhận xoá (hộp thứ hai, đè lên hộp chính) -----
    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Xoá thói quen?") },
            text = { Text("Toàn bộ lịch sử và chuỗi ngày của thói quen này sẽ bị xoá.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false; onDismiss() }) {
                    Text("Xoá", color = DangerRed)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Huỷ") } }
        )
    }
}
