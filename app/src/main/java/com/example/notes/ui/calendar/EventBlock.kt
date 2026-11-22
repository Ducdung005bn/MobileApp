// =============================================================================================
// FILE: ui/calendar/EventBlock.kt
// VAI TRÒ: MỘT KHỐI LỊCH trên lưới giờ: hình chữ nhật màu, hiển thị chữ, bấm để mở, nhấn giữ rồi kéo để dời.
// Đây là nơi duy nhất trong thư mục xử lý cử chỉ kéo thả.
// NỘI DUNG:
//   • `EventBlock`: khung khối + toàn bộ logic kéo thả.
//   • `BlockText`: chữ bên trong khối (tiêu đề, biểu tượng, khoảng giờ).
// XỬ LÝ:
//   • `EventBlock`: bấm = mở (onClick); nhấn giữ rồi kéo = dời. Trong lúc kéo chỉ dịch hình bằng `dragOffset`
//     (trạng thái cục bộ), chưa ghi database. Thả tay thì đổi pixel thành phút (làm tròn bội của 15) và số
//     ngày rồi gọi onMove; chế độ Ngày không kéo ngang được.
//   • Trùng giờ: `hasConflict = true` thì khối có viền đỏ và `BlockText` thêm biểu tượng cảnh báo.
//   • Khối đã hoàn thành (công việc): nền nhạt hơn, tiêu đề gạch ngang. Khối đang kéo: nền đậm, có bóng.
// BACK / ĐÓNG: không xử lý Back. Các trường hợp huỷ kéo thả:
//   • Thả tay mà dời quá ít (0 phút, 0 ngày): trả khối về chỗ cũ, không ghi gì.
//   • Cử chỉ bị huỷ (`onDragCancel`): trả khối về chỗ cũ.
//   • Đã gọi onMove mà database không cập nhật (lỗi): sau 0,9 giây tự trả khối về chỗ cũ.
// LIÊN QUAN: calendar/DayColumn.kt (nơi gọi và tính vị trí), calendar/CalendarViewModel.kt (moveBlock),
//            calendar/TimeGrid.kt (HourHeight).
// =============================================================================================

package com.example.notes.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.notes.data.BlockKind
import com.example.notes.data.TimeBlock
import com.example.notes.ui.theme.DangerRed
import com.example.notes.ui.theme.categoryColor
import com.example.notes.ui.util.Fmt
import kotlin.math.roundToInt

private const val SnapMinutes = 15 // kéo thả được "hít" vào bội số 15 phút

// ---------------------------------------------------------------------------------------------
// Một khối lịch, có thể kéo thả
// ---------------------------------------------------------------------------------------------

/**
 * Một khối lịch. Bấm = mở. Nhấn giữ rồi kéo = dời.
 *
 * Quy trình kéo thả:
 *  1. Nhấn giữ -> `isDragging = true`, khối nổi lên (có bóng).
 *  2. Ngón tay di chuyển -> `dragOffset` cộng dồn quãng đường, khối đi theo ngón tay.
 *  3. Thả tay -> đổi quãng đường (pixel) thành "số phút" (làm tròn bội 15) và "số ngày",
 *     gọi [onMove]. ViewModel sẽ ghi vào database.
 *  4. Khi database cập nhật, khối được vẽ lại ở vị trí mới và `dragOffset` được đặt về 0.
 *
 * @param columnWidthPx bề rộng một cột ngày (pixel): chia quãng kéo ngang cho số này ra "số ngày dời".
 * @param canMoveAcrossDays false thì bỏ qua chuyển động ngang (chế độ Ngày).
 * @param modifier do DayColumn truyền vào: đã chứa bề rộng/chiều cao/vị trí của khối.
 */
@Composable
internal fun EventBlock(
    block: TimeBlock,
    hasConflict: Boolean,
    columnWidthPx: Float,
    canMoveAcrossDays: Boolean,
    onClick: () -> Unit,
    onMove: (TimeBlock, Int, Int) -> Unit,
    modifier: Modifier
) {
    val color = block.categoryColor()
    val hourHeightPx = with(LocalDensity.current) { HourHeight.toPx() }

    // ----- Trạng thái kéo thả (cục bộ cho từng khối) -----
    var dragOffset by remember { mutableStateOf(Offset.Zero) } // quãng đường đã kéo (pixel)
    var isDragging by remember { mutableStateOf(false) }
    var isWaitingForSave by remember { mutableStateOf(false) } // đã thả tay, chờ database cập nhật

    // Luôn dùng phiên bản mới nhất của `block` và `onMove` bên trong cử chỉ kéo
    // (cử chỉ được tạo một lần nên nếu không dùng cách này sẽ giữ bản cũ).
    val latestBlock by rememberUpdatedState(block)
    val latestOnMove by rememberUpdatedState(onMove)

    // ----- Hai hiệu ứng phụ để dọn trạng thái kéo -----
    // Khi giờ của khối đổi (database đã cập nhật) -> bỏ phần dịch tạm thời.
    LaunchedEffect(block.start, block.end) {
        dragOffset = Offset.Zero
        isWaitingForSave = false
    }
    // Phòng trường hợp database không cập nhật (lỗi): sau 0,9 giây trả khối về chỗ cũ.
    LaunchedEffect(isWaitingForSave) {
        if (isWaitingForSave) {
            kotlinx.coroutines.delay(900)
            dragOffset = Offset.Zero
            isWaitingForSave = false
        }
    }

    val shape = RoundedCornerShape(8.dp)

    // ----- Khung khối + cử chỉ -----
    // Thứ tự modifier quan trọng: offset (đi theo ngón tay) -> zIndex/shadow (nổi lên khi kéo)
    // -> clip + background (hình dạng và màu) -> border (viền đỏ nếu trùng) -> clickable -> pointerInput (kéo).
    Box(
        modifier = modifier
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) } // đi theo ngón tay
            .zIndex(if (isDragging) 10f else 0f) // khối đang kéo nằm trên cùng
            .then(if (isDragging) Modifier.shadow(10.dp, shape) else Modifier)
            .clip(shape)
            .background(color.copy(alpha = if (isDragging) 0.95f else if (block.isDone) 0.12f else 0.22f))
            .then(if (hasConflict) Modifier.border(1.5.dp, DangerRed, shape) else Modifier)
            .clickable(onClick = onClick)
            .pointerInput(block.key) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { isDragging = true },
                    onDrag = { change, amount ->
                        change.consume()
                        val newX = if (canMoveAcrossDays) dragOffset.x + amount.x else 0f
                        dragOffset = Offset(newX, dragOffset.y + amount.y)
                    },
                    onDragEnd = {
                        isDragging = false
                        // pixel -> phút: (pixel ÷ pixel mỗi giờ) × 60, rồi làm tròn về bội của 15 phút.
                        val rawMinutes = dragOffset.y / hourHeightPx * 60f
                        val deltaMinutes = (rawMinutes / SnapMinutes).roundToInt() * SnapMinutes
                        // pixel -> ngày: chia cho bề rộng một cột.
                        val deltaDays = if (canMoveAcrossDays) (dragOffset.x / columnWidthPx).roundToInt() else 0

                        if (deltaMinutes == 0 && deltaDays == 0) {
                            dragOffset = Offset.Zero // kéo quá ít, coi như không dời
                        } else {
                            isWaitingForSave = true
                            latestOnMove(latestBlock, deltaDays, deltaMinutes)
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffset = Offset.Zero
                    }
                )
            }
    ) {
        // Vạch màu đậm bên trái khối
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(color)
        )
        BlockText(block = block, hasConflict = hasConflict, isDragging = isDragging, color = color)
    }
}

// ---------------------------------------------------------------------------------------------
// Chữ bên trong khối
// ---------------------------------------------------------------------------------------------

/** Chữ bên trong khối: tiêu đề (kèm biểu tượng) và khoảng giờ. */
@Composable
private fun BlockText(block: TimeBlock, hasConflict: Boolean, isDragging: Boolean, color: Color) {
    // Khi đang kéo nền khối đậm nên đổi chữ sang màu trắng cho dễ đọc.
    val titleColor = if (isDragging) Color.White else MaterialTheme.colorScheme.onSurface
    val timeColor = if (isDragging) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 3.dp)) {
        // Dòng 1: [biểu tượng công việc] tiêu đề [biểu tượng cảnh báo trùng giờ]
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (block.kind == BlockKind.TASK) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            }
            Text(
                text = block.title,
                style = MaterialTheme.typography.labelMedium,
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (block.isDone) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (hasConflict) {
                Icon(Icons.Rounded.Warning, contentDescription = "Trùng giờ", tint = DangerRed, modifier = Modifier.size(12.dp))
            }
        }
        // Dòng 2: khoảng giờ "09:00–10:30"
        Text(
            text = "${Fmt.time(block.start)}–${Fmt.time(block.end)}",
            style = MaterialTheme.typography.labelSmall,
            color = timeColor,
            maxLines = 1
        )
    }
}
