// =============================================================================================
// FILE: ui/calendar/LaneLayout.kt
// VAI TRÒ: thuật toán CHIA LÀN cho các khối lịch chồng giờ lên nhau. Đây là hàm thuần (chỉ nhận dữ liệu,
// trả dữ liệu; không có giao diện, không có state), nên dễ đọc và dễ kiểm thử một mình.
// NỘI DUNG:
//   • `assignLanes`: với mỗi khối trả về (số thứ tự làn, tổng số làn của nhóm).
// VÍ DỤ:
//   A: 9:00–10:30,  B: 10:00–11:00,  C: 12:00–13:00
//   -> A và B chồng nhau thành một nhóm 2 làn: A = (0, 2), B = (1, 2). C đứng riêng: C = (0, 1).
//   DayColumn dùng kết quả này để chia bề rộng cột: A và B mỗi khối rộng một nửa, C rộng cả cột.
// XỬ LÝ (3 bước):
//   1. Sắp các khối theo giờ bắt đầu (rồi giờ kết thúc).
//   2. Khối nào bắt đầu SAU KHI cả nhóm hiện tại đã kết thúc thì chốt nhóm cũ, mở nhóm mới.
//   3. Trong một nhóm, mỗi khối vào làn đầu tiên đã trống; không có làn trống thì mở làn mới.
//   Khi chốt nhóm, mọi khối trong nhóm cùng nhận "tổng số làn" của nhóm đó.
// LƯU Ý: đây là cách XẾP VỊ TRÍ để hai khối không đè nhau. Việc KẾT LUẬN "hai khối trùng giờ" (viền đỏ,
// dải cảnh báo) là việc khác, do `Scheduling.conflictKeys` trong data/Scheduling.kt đảm nhiệm.
// LIÊN QUAN: calendar/DayColumn.kt (nơi gọi).
// =============================================================================================

package com.example.notes.ui.calendar

import com.example.notes.data.TimeBlock

// ---------------------------------------------------------------------------------------------
// Chia làn
// ---------------------------------------------------------------------------------------------

/**
 * Chia "làn" để các khối trùng giờ nằm cạnh nhau thay vì đè lên nhau.
 *
 * Kết quả: với mỗi khối (theo `key`) trả về (số thứ tự làn, tổng số làn của nhóm).
 * Ví dụ hai khối trùng nhau: khối A -> (0, 2), khối B -> (1, 2): mỗi khối rộng một nửa cột.
 *
 * Thuật toán: duyệt các khối theo giờ bắt đầu. Các khối liên tiếp chồng thời gian với nhau
 * tạo thành một "nhóm"; trong nhóm, mỗi khối vào làn đầu tiên đã trống.
 */
internal fun assignLanes(blocks: List<TimeBlock>): Map<String, Pair<Int, Int>> {
    val result = mutableMapOf<String, Pair<Int, Int>>()

    // ----- Biến theo dõi nhóm đang xét -----
    var groupMembers = mutableListOf<TimeBlock>() // các khối trong nhóm hiện tại
    val laneEndTimes = mutableListOf<Long>() // laneEndTimes[i] = giờ kết thúc khối cuối cùng ở làn i
    val laneOfBlock = mutableMapOf<String, Int>()
    var groupEnd = Long.MIN_VALUE // giờ kết thúc muộn nhất của nhóm

    /** Chốt nhóm hiện tại: ghi kết quả cho từng khối rồi bắt đầu nhóm mới. */
    fun finishGroup() {
        for (member in groupMembers) {
            result[member.key] = laneOfBlock.getValue(member.key) to laneEndTimes.size
        }
        groupMembers = mutableListOf()
        laneEndTimes.clear()
        laneOfBlock.clear()
    }

    // ----- Duyệt từng khối theo thứ tự thời gian -----
    for (block in blocks.sortedWith(compareBy({ it.start }, { it.end }))) {
        // Khối này bắt đầu sau khi cả nhóm đã kết thúc -> chốt nhóm cũ, bắt đầu nhóm mới.
        if (groupMembers.isNotEmpty() && block.start >= groupEnd) finishGroup()

        groupEnd = if (groupMembers.isEmpty()) block.end else maxOf(groupEnd, block.end)

        // Tìm làn đầu tiên đã trống (khối cuối của làn đó đã kết thúc trước khi khối này bắt đầu).
        var lane = laneEndTimes.indexOfFirst { it <= block.start }
        if (lane < 0) {
            laneEndTimes += block.end // không có làn trống -> mở làn mới
            lane = laneEndTimes.lastIndex
        } else {
            laneEndTimes[lane] = block.end
        }

        laneOfBlock[block.key] = lane
        groupMembers += block
    }
    // Nhóm cuối cùng chưa được chốt trong vòng lặp.
    if (groupMembers.isNotEmpty()) finishGroup()

    return result
}
