// =============================================================================================
// FILE: ui/calendar/TimeGrid.kt
// VAI TRÒ: "khung tổng" của lưới giờ (chế độ Ngày và Tuần). File này chỉ LẮP RÁP; từng mảnh nhỏ nằm ở
// các file riêng (xem bản đồ bên dưới).
// NỘI DUNG:
//   • Hai hằng số kích thước dùng chung cho cả nhóm file lưới: `HourHeight`, `GutterWidth`.
//   • `TimeGrid`: ghép hàng tiêu đề ngày, hàng "cả ngày", cột nhãn giờ và các cột ngày.
// XỬ LÝ:
//   • Lưới cao 24 x 56 dp, cuộn dọc, mở lịch thì tự cuộn tới 7 giờ sáng. 25 đường kẻ giờ vẽ bằng
//     drawBehind. Vị trí dọc = số phút kể từ nửa đêm x (56 dp / 60); chiều cao khối tính cùng công thức.
//   • Tách các khối "cả ngày" (hiện ở hàng riêng phía trên) khỏi khối có giờ cụ thể (vẽ trong cột ngày).
//   • Chế độ Ngày chỉ có 1 cột nên `canMoveAcrossDays = false` (không kéo ngang sang ngày khác được).
// BẢN ĐỒ CÁC MẢNH (từ ngoài vào trong):
//   TimeGrid ─┬─ DayHeader          (T2, 24 ...)          TimeGridFrame.kt
//             ├─ AllDayRow          (sự kiện cả ngày)     TimeGridFrame.kt
//             └─ vùng cuộn ─┬─ HourLabels   (cột giờ)     TimeGridFrame.kt
//                           └─ DayColumn ─┬─ NowLine        DayColumn.kt
//                                         ├─ assignLanes    LaneLayout.kt (chia làn khi trùng giờ)
//                                         └─ EventBlock     EventBlock.kt (từng khối, có kéo thả)
// BACK / ĐÓNG: không xử lý Back (xem EventBlock.kt cho các trường hợp huỷ kéo thả).
// LIÊN QUAN: calendar/CalendarScreen.kt (nơi gọi và nối callback), calendar/CalendarViewModel.kt (moveBlock),
//            data/Scheduling.kt (tính trùng giờ).
// =============================================================================================

package com.example.notes.ui.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

// ===============================================================================================
// LƯỚI THỜI GIAN (chế độ Ngày và Tuần của màn Lịch)
//
// Hình dung: một bảng có 24 hàng (24 giờ) và N cột (N ngày).
//   • Mỗi khối lịch là một hình chữ nhật đặt vào đúng cột (ngày) và đúng độ cao (giờ).
//   • "Vị trí dọc"  = số phút kể từ nửa đêm × (độ cao một giờ ÷ 60).
//   • "Độ cao khối" = thời lượng (phút) × (độ cao một giờ ÷ 60).
//   • Nhấn giữ một khối rồi kéo = dời lịch (kéo thả).
// ===============================================================================================

// ---------------------------------------------------------------------------------------------
// Hằng số kích thước. `internal` = các file khác trong module (EventBlock, DayColumn, TimeGridFrame...)
// dùng chung, để đổi một chỗ là cả lưới đổi theo.
// ---------------------------------------------------------------------------------------------

/** Độ cao của một giờ trên lưới. Mọi phép tính "phút -> vị trí dọc" đều dựa vào số này. */
internal val HourHeight = 56.dp

/** Bề rộng cột nhãn giờ bên trái (01:00, 02:00...). Các hàng phía trên cũng chừa đúng bề rộng này. */
internal val GutterWidth = 42.dp

private const val FirstVisibleHour = 7 // mở lịch thì tự cuộn tới 7 giờ sáng

// ---------------------------------------------------------------------------------------------
// Khung tổng của lưới
// ---------------------------------------------------------------------------------------------

/**
 * Lưới giờ cho chế độ Ngày / Tuần.
 *
 * @param firstDay ngày (epoch-day) của cột đầu tiên.
 * @param dayCount số cột: 1 (chế độ Ngày) hoặc 7 (chế độ Tuần).
 * @param selectedDay ngày đang được chọn (tiêu đề ngày đó có viền).
 * @param blocks mọi khối lịch trong khoảng đang hiển thị (đã mở rộng lịch lặp).
 * @param conflictKeys khoá của các khối bị trùng giờ (để vẽ viền đỏ).
 * @param onSelectDay bấm tiêu đề một ngày.
 * @param onBlockClick bấm một khối lịch.
 * @param onMove thả tay sau khi kéo: (khối, số ngày dời, số phút dời).
 */
@Composable
fun TimeGrid(
    firstDay: Long,
    dayCount: Int,
    selectedDay: Long,
    blocks: List<TimeBlock>,
    conflictKeys: Set<String>,
    onSelectDay: (Long) -> Unit,
    onBlockClick: (TimeBlock) -> Unit,
    onMove: (block: TimeBlock, deltaDays: Int, deltaMinutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { TimeUtils.today() }
    // Danh sách các ngày (cột) cần vẽ; chỉ tính lại khi firstDay hoặc dayCount đổi.
    val days = remember(firstDay, dayCount) { (0 until dayCount).map { firstDay + it } }

    // Trạng thái cuộn dọc; ban đầu cuộn sẵn tới 7 giờ sáng cho tiện.
    val density = LocalDensity.current
    val scrollState = rememberScrollState(initial = with(density) { (HourHeight * FirstVisibleHour).roundToPx() })

    // Tách khối "cả ngày" (hiện ở hàng riêng phía trên) khỏi khối có giờ cụ thể.
    val allDayBlocks = blocks.filter { it.allDay }
    val timedBlocks = blocks.filter { !it.allDay }

    Column(modifier = modifier.fillMaxSize()) {
        // Hàng tiêu đề các ngày (chừa cột giờ bên trái bằng padding)
        Row(modifier = Modifier.fillMaxWidth().padding(start = GutterWidth)) {
            for (day in days) {
                DayHeader(
                    day = day,
                    isToday = day == today,
                    isSelected = day == selectedDay,
                    onClick = { onSelectDay(day) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Hàng các sự kiện cả ngày (chỉ hiện khi có)
        if (allDayBlocks.isNotEmpty()) {
            AllDayRow(days = days, allDayBlocks = allDayBlocks, onBlockClick = onBlockClick)
        }

        // Vùng cuộn dọc chứa cột giờ + các cột ngày
        val lineColor = MaterialTheme.colorScheme.outlineVariant
        val hourHeightPx = with(density) { HourHeight.toPx() }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = 6.dp)
        ) {
            HourLabels()

            // Các cột ngày. drawBehind vẽ 25 đường kẻ ngang (mỗi giờ một đường) làm nền.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(HourHeight * 24)
                    .drawBehind {
                        for (hour in 0..24) {
                            val y = hour * hourHeightPx
                            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        }
                    }
            ) {
                for (day in days) {
                    DayColumn(
                        day = day,
                        isToday = day == today,
                        blocks = timedBlocks,
                        conflictKeys = conflictKeys,
                        canMoveAcrossDays = dayCount > 1, // chế độ Ngày chỉ có 1 cột nên không kéo ngang
                        onBlockClick = onBlockClick,
                        onMove = onMove,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}
