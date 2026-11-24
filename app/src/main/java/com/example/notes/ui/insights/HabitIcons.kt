// =============================================================================================
// FILE: ui/insights/HabitIcons.kt
// VAI TRÒ: mọi thứ liên quan đến BIỂU TƯỢNG của thói quen: bảng "khoá chữ -> hình" và ô chọn biểu tượng.
// NỘI DUNG:
//   • `HabitIcons`: bảng tra cứu khoá chữ (ví dụ "book") sang biểu tượng; `of(key)` là hàm tra.
//   • `IconPicker`: hàng các ô tròn biểu tượng cho người dùng chọn trong HabitDialog.
// XỬ LÝ:
//   • Database CHỈ LƯU khoá chữ (cột `icon` của HabitEntity), không lưu hình. Muốn vẽ thì tra bằng `of(key)`.
//     Lý do: hình (ImageVector) là code, không lưu vào database được; khoá chữ thì lưu được và ổn định.
//   • Khoá lạ hoặc rỗng (ví dụ thói quen cũ) thì `of` trả về hình ngôi sao, nên không bao giờ lỗi.
//   • Muốn thêm biểu tượng mới: thêm một dòng vào `HabitIcons.all`.
//   • `IconPicker`: ô đang chọn được tô màu nhạt và có viền; bấm ô nào gọi `onSelect(key)`. Danh sách có thể
//     vuốt ngang. Màu nhấn (`accent`) lấy theo danh mục đang chọn nên đổi danh mục thì màu ô đổi theo.
// LIÊN QUAN: insights/HabitDialog.kt (nơi dùng IconPicker), insights/HabitCard.kt (dùng HabitIcons.of để vẽ).
// =============================================================================================

package com.example.notes.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------------------------
// Bảng biểu tượng
// ---------------------------------------------------------------------------------------------

/**
 * Các biểu tượng có thể chọn cho một thói quen. Database chỉ lưu "khoá" chữ (ví dụ "book"),
 * hàm [of] đổi khoá đó thành biểu tượng để vẽ.
 */
object HabitIcons {
    // linkedMapOf giữ đúng thứ tự khai báo, nên IconPicker hiện biểu tượng theo thứ tự này.
    val all: Map<String, ImageVector> = linkedMapOf(
        "book" to Icons.AutoMirrored.Rounded.MenuBook,
        "fitness" to Icons.Rounded.FitnessCenter,
        "language" to Icons.Rounded.Translate,
        "water" to Icons.Rounded.WaterDrop,
        "sleep" to Icons.Rounded.Bedtime,
        "meditate" to Icons.Rounded.SelfImprovement,
        "code" to Icons.Rounded.Code,
        "music" to Icons.Rounded.MusicNote
    )

    /** Khoá lạ hoặc rỗng thì dùng hình ngôi sao. */
    fun of(key: String): ImageVector = all[key] ?: Icons.Rounded.Star
}

// ---------------------------------------------------------------------------------------------
// Ô chọn biểu tượng
// ---------------------------------------------------------------------------------------------

/**
 * Hàng các ô tròn biểu tượng; ô đang chọn được tô màu và có viền.
 *
 * @param selectedKey khoá của biểu tượng đang chọn (state do HabitDialog giữ).
 * @param accent màu nhấn cho ô đang chọn.
 * @param onSelect gọi với khoá của ô vừa được bấm.
 */
@Composable
internal fun IconPicker(selectedKey: String, accent: Color, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((key, vector) in HabitIcons.all) {
            val isSelected = key == selectedKey

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) accent else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center
            ) {
                Icon(vector, contentDescription = null, tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
