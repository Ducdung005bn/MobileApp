// =============================================================================================
// FILE: ui/theme/CategoryStyle.kt
// NHIỆM VỤ: gắn "hình thức" (màu, tên hiển thị, biểu tượng) vào các enum của domain/ bằng thuộc tính mở
// rộng, để tầng domain không phải biết đến màu hay biểu tượng.
// CHỨA:
//   • `Category.color / .label / .icon`: Học tập, Công việc, Cá nhân, Sức khỏe.
//   • `Priority.label / .color`: Thấp, Vừa, Cao.
//   • `TimeBlock.categoryColor()`: màu của một khối trên lịch theo danh mục của nó.
// BACK / ĐÓNG: không liên quan.
// LIÊN QUAN: theme/Color.kt (nguồn màu), domain/Enums.kt, data/Scheduling.kt (TimeBlock).
// =============================================================================================

package com.example.notes.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.notes.data.TimeBlock
import com.example.notes.domain.Category
import com.example.notes.domain.Priority

// Các enum (Category, Priority) nằm ở tầng domain nên không biết gì về màu hay chữ hiển thị.
// File này thêm "thuộc tính mở rộng" (extension property) để giao diện hỏi được:
//     category.color   category.label   category.icon
// Ví dụ: `Category.STUDY.label` trả về "Học tập".

// ----- Danh mục công việc -----

val Category.color: Color
    get() = when (this) {
        Category.STUDY -> CategoryStudy
        Category.WORK -> CategoryWork
        Category.PERSONAL -> CategoryPersonal
        Category.HEALTH -> CategoryHealth
    }

val Category.label: String
    get() = when (this) {
        Category.STUDY -> "Học tập"
        Category.WORK -> "Công việc"
        Category.PERSONAL -> "Cá nhân"
        Category.HEALTH -> "Sức khỏe"
    }

val Category.icon: ImageVector
    get() = when (this) {
        Category.STUDY -> Icons.Rounded.School
        Category.WORK -> Icons.Rounded.Work
        Category.PERSONAL -> Icons.Rounded.Person
        Category.HEALTH -> Icons.Rounded.FitnessCenter
    }

// ----- Mức ưu tiên -----

val Priority.label: String
    get() = when (this) {
        Priority.LOW -> "Thấp"
        Priority.MEDIUM -> "Vừa"
        Priority.HIGH -> "Cao"
    }

val Priority.color: Color
    get() = when (this) {
        Priority.LOW -> PriorityLow
        Priority.MEDIUM -> PriorityMedium
        Priority.HIGH -> PriorityHigh
    }

/**
 * Màu của một khối trên lịch: nếu lịch có màu riêng (khác 0) thì dùng màu đó,
 * không thì dùng màu của danh mục.
 */
fun TimeBlock.categoryColor(): Color =
    if (color != 0) Color(color) else Category.fromKey(category).color
