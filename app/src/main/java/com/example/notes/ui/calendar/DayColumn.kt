// =============================================================================================
// FILE: ui/calendar/DayColumn.kt
// VAI TRÒ: MỘT CỘT NGÀY trong lưới giờ: đặt từng khối lịch của ngày đó vào đúng giờ, đúng làn.
// NỘI DUNG:
//   • `DayColumn`: cột của một ngày; vòng lặp tạo `EventBlock` cho từng khối và tính vị trí/kích thước.
//   • `NowLine`: vạch đỏ ngang đánh dấu GIỜ HIỆN TẠI (chỉ hiện ở cột hôm nay).
// XỬ LÝ:
//   • Chỉ lấy khối giao với ngày này; khối qua nửa đêm chỉ vẽ phần nằm trong ngày (cắt bằng max/min).
//   • Công thức vị trí:  y = HourHeight × (số phút từ đầu ngày ÷ 60);  cao = HourHeight × (số phút ÷ 60),
//     tối thiểu 24 dp để khối ngắn vẫn đọc/bấm được.
//   • Chia làn: `assignLanes` (LaneLayout.kt) cho biết khối thuộc làn thứ mấy trong tổng N làn; bề rộng
//     mỗi khối = bề rộng cột ÷ N, dịch ngang = bề rộng làn × số thứ tự làn.
//   • Trùng giờ: `hasConflict = block.key in conflictKeys` được truyền xuống EventBlock để vẽ viền đỏ.
//   • NowLine: `minutesNow` lấy MỘT LẦN khi composable xuất hiện (remember không có khoá) nên vạch không
//     tự di chuyển theo thời gian; rời tab Lịch rồi quay lại thì được tính lại.
// LIÊN QUAN: calendar/TimeGrid.kt (nơi gọi), calendar/LaneLayout.kt, calendar/EventBlock.kt.
// =============================================================================================

package com.example.notes.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.notes.data.TimeBlock
import com.example.notes.domain.TimeUtils
import com.example.notes.ui.theme.DangerRed

// ---------------------------------------------------------------------------------------------
// Một cột ngày
// ---------------------------------------------------------------------------------------------

/**
 * Cột của một ngày: đặt các khối lịch của ngày đó vào đúng vị trí.
 * Hai khối chồng lên nhau về thời gian sẽ được xếp cạnh nhau (xem [assignLanes]).
 *
 * @param canMoveAcrossDays false ở chế độ Ngày (chỉ 1 cột nên không kéo ngang được).
 * @param onMove chuyển tiếp xuống EventBlock; được gọi khi người dùng thả tay sau khi kéo.
 */
@Composable
internal fun DayColumn(
    day: Long,
    isToday: Boolean,
    blocks: List<TimeBlock>,
    conflictKeys: Set<String>,
    canMoveAcrossDays: Boolean,
    onBlockClick: (TimeBlock) -> Unit,
    onMove: (TimeBlock, Int, Int) -> Unit,
    modifier: Modifier
) {
    val dayStart = TimeUtils.dayStart(day)
    val dayEnd = TimeUtils.dayEnd(day)

    // Chỉ lấy các khối có giao với ngày này; tính lại khi danh sách khối hoặc ngày đổi.
    val dayBlocks = remember(blocks, day) { blocks.filter { it.start < dayEnd && it.end > dayStart } }
    // Bảng "khoá khối -> (số thứ tự làn, tổng số làn)".
    val lanes = remember(dayBlocks) { assignLanes(dayBlocks) }

    val separatorColor = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current

    // BoxWithConstraints cho biết bề rộng cột (maxWidth) để chia cho các làn.
    BoxWithConstraints(
        modifier = modifier
            .drawBehind { drawLine(separatorColor, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1f) } // đường dọc ngăn cột
            .then(if (isToday) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)) else Modifier)
    ) {
        val columnWidth = maxWidth
        val columnWidthPx = with(density) { columnWidth.toPx() } // EventBlock cần pixel để đổi quãng kéo ngang thành số ngày

        for (block in dayBlocks) {
            val (lane, laneCount) = lanes[block.key] ?: (0 to 1)

            // Nếu khối kéo dài qua nửa đêm thì chỉ vẽ phần nằm trong ngày này.
            val shownStart = maxOf(block.start, dayStart)
            val shownEnd = minOf(block.end, dayEnd)
            val startMinute = (shownStart - dayStart) / 60_000f // phút kể từ nửa đêm
            val lengthMinutes = (shownEnd - shownStart) / 60_000f

            val laneWidth = columnWidth / laneCount

            EventBlock(
                block = block,
                hasConflict = block.key in conflictKeys,
                columnWidthPx = columnWidthPx,
                canMoveAcrossDays = canMoveAcrossDays,
                onClick = { onBlockClick(block) },
                onMove = onMove,
                modifier = Modifier
                    .width(laneWidth - 2.dp)
                    // offset: dịch khối tới đúng làn (ngang) và đúng giờ (dọc)
                    .offset(x = laneWidth * lane + 1.dp, y = HourHeight * (startMinute / 60f))
                    // Khối quá ngắn vẫn cao tối thiểu 24dp để còn đọc/bấm được.
                    .height((HourHeight * (lengthMinutes / 60f)).coerceAtLeast(24.dp))
            )
        }

        // Vạch giờ hiện tại: vẽ SAU các khối nên nằm đè lên trên.
        if (isToday) NowLine()
    }
}

// ---------------------------------------------------------------------------------------------
// Vạch giờ hiện tại
// ---------------------------------------------------------------------------------------------

/**
 * Đường đỏ ngang đánh dấu thời điểm hiện tại (chỉ hiện ở cột hôm nay).
 * Không liên quan đến trùng giờ dù cùng màu đỏ (DangerRed).
 */
@Composable
internal fun NowLine() {
    val minutesNow = remember { TimeUtils.minutesOfDay(System.currentTimeMillis()) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = HourHeight * (minutesNow / 60f)) // dịch xuống đúng giờ hiện tại
            .height(2.dp)
            .background(DangerRed)
    )
}
