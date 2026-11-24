// =============================================================================================
// FILE: ui/insights/InsightsScreen.kt
// VAI TRÒ: "người điều phối" của tab Thống kê. File này sắp xếp các khối (mỗi khối nằm ở một file riêng)
// thành một danh sách cuộn, và quyết định khi nào mở/đóng hộp thoại thói quen.
//
// BẢN ĐỒ THƯ MỤC ui/insights/ (đọc theo thứ tự này sẽ dễ hiểu nhất):
//   InsightsViewModel.kt      BỘ NÃO: gom dữ liệu thành `InsightsState`, nhận hành động về thói quen.
//   InsightsScreen.kt         (file này) ĐIỀU PHỐI: xếp các khối theo thứ tự từ trên xuống.
//   StatTiles.kt              3 ô số liệu đầu trang (hoàn thành tuần này, chuỗi dài nhất, thời gian đã làm).
//   ── Thói quen ──────────────────────────────────────────────────────────────────────────────
//   HabitCard.kt              Thẻ một thói quen: tick hôm nay, chuỗi ngày, 7 ngày gần nhất.
//   HabitDialog.kt            Hộp thoại thêm/sửa thói quen (+ hộp xác nhận xoá).
//   HabitIcons.kt             Bảng biểu tượng cho thói quen + ô chọn biểu tượng (IconPicker).
//   ── Biểu đồ ────────────────────────────────────────────────────────────────────────────────
//   ChartCard.kt              Khung thẻ chung cho các biểu đồ (tiêu đề + nội dung).
//   WeeklyCompletionCard.kt   Biểu đồ cột "Hoàn thành công việc" theo tuần, có nút chuyển tuần.
//   CategoryTimeCharts.kt     Biểu đồ tròn "thời gian theo danh mục" và thanh "kế hoạch so với thực tế".
//   HeatmapLegend.kt          Chú thích "Ít ... Nhiều" dưới heatmap hoạt động.
//   (Phần vẽ biểu đồ thật bằng Canvas nằm ở components/Charts.kt.)
//
// DÒNG CHẢY DỮ LIỆU: InsightsViewModel ─state─► InsightsScreen ─► từng khối. Khối chỉ nhận dữ liệu cần
// dùng và gọi callback; hành động ghi dữ liệu (tick, lưu, xoá thói quen) đi ngược lên ViewModel.
//
// XỬ LÝ CỦA FILE NÀY: một LazyColumn gồm ô số liệu đầu trang, danh sách thói quen, thẻ "Hoàn thành công
// việc", "Thời gian theo danh mục", "Kế hoạch so với thực tế" và heatmap hoạt động.
// BACK / ĐÓNG hộp thoại thói quen:
//   • `habitDialog` là trạng thái cục bộ: null = đóng; HabitEntity id = 0 = tạo mới; id > 0 = đang sửa.
//   • Back, chạm ra ngoài hoặc "Huỷ" gọi `onDismiss = { habitDialog = null }`: đóng, không lưu.
//   • Màn này không có BackHandler; Back từ tab này về tab Hôm nay (xem nav/NotesApp.kt).
// ĐƯỢC GỌI TỪ: nav/NotesApp.kt (composable Tab.INSIGHTS).
// =============================================================================================

package com.example.notes.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notes.data.HabitEntity
import com.example.notes.ui.appViewModel
import com.example.notes.ui.components.ActivityHeatmap
import com.example.notes.ui.components.EmptyState
import com.example.notes.ui.components.SectionHeader

// ===============================================================================================
// MÀN HÌNH "THỐNG KÊ & THÓI QUEN"
//
// Là một danh sách cuộn gồm các khối (mỗi khối là một hàm ở file riêng):
//   StatTiles → Thói quen (HabitCard) → Hoàn thành công việc (cột) → Thời gian theo danh mục (tròn)
//   → Kế hoạch so với thực tế → Hoạt động (heatmap)
// ===============================================================================================

/**
 * Màn hình Thống kê.
 *
 * @param viewModel bộ não của màn; mặc định được tạo bởi `appViewModel { ... }` nên sống qua xoay máy.
 */
@Composable
fun InsightsScreen(viewModel: InsightsViewModel = appViewModel { InsightsViewModel(it) }) {
    // ----- Trạng thái -----
    // `state` là bản chụp mới nhất của ViewModel; mỗi khi ViewModel phát bản mới, màn tự vẽ lại.
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Hộp thoại thói quen: null = đóng; HabitEntity có id = 0 = tạo mới; có id > 0 = đang sửa.
    // Đây là state CỤC BỘ (remember) nên xoay máy sẽ làm hộp thoại đóng.
    var habitDialog by remember { mutableStateOf<HabitEntity?>(null) }

    // ----- Danh sách cuộn gồm các khối -----
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Tiêu đề màn
            item {
                Text("Thống kê", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Thói quen, tiến độ và thời gian của bạn",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 3 ô số liệu (StatTiles.kt)
            item { StatTiles(state) }

            // ----- Thói quen -----
            // Tiêu đề khối + nút "Thêm": bấm là đặt `habitDialog` = thói quen rỗng (id = 0 = tạo mới).
            item {
                SectionHeader(
                    title = "Thói quen",
                    subtitle = "Đánh dấu mỗi ngày để giữ chuỗi",
                    action = {
                        TextButton(onClick = { habitDialog = HabitEntity(name = "") }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Thêm", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                )
            }
            // Chưa có thói quen nào (và đã tải xong) thì hiện lời gợi ý.
            if (state.habits.isEmpty() && state.loaded) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.LocalFireDepartment,
                        title = "Chưa có thói quen nào",
                        subtitle = "Tạo thói quen như đọc sách, tập thể dục, học ngoại ngữ để theo dõi chuỗi ngày."
                    )
                }
            }
            // Mỗi thói quen một thẻ (HabitCard.kt). `key` giúp Compose nhận ra thẻ nào là thẻ nào khi danh sách đổi.
            items(state.habits, key = { it.habit.id }) { habit ->
                HabitCard(
                    stats = habit,
                    today = state.today,
                    onToggle = { day -> viewModel.toggleHabit(habit.habit.id, day) },
                    onEdit = { habitDialog = habit.habit }
                )
            }

            // ----- Biểu đồ -----
            // Biểu đồ cột theo tuần (WeeklyCompletionCard.kt)
            item { WeeklyCompletionCard(state, viewModel) }

            // Biểu đồ tròn (CategoryTimeCharts.kt), đặt trong khung chung ChartCard (ChartCard.kt)
            item {
                ChartCard(
                    title = "Thời gian theo danh mục",
                    subtitle = "Công việc đã xếp giờ và hoàn thành trong tuần"
                ) { TimeByCategory(state) }
            }

            // Kế hoạch so với thực tế (CategoryTimeCharts.kt)
            item {
                ChartCard(
                    title = "Kế hoạch so với thực tế",
                    subtitle = "Thanh nhạt: đã xếp giờ • thanh đậm: đã hoàn thành"
                ) { PlanVsActual(state) }
            }

            // Heatmap hoạt động: ô vuông theo ngày, đậm nhạt theo số hoạt động (vẽ ở components/Charts.kt)
            item {
                ChartCard(
                    title = "Hoạt động",
                    subtitle = "$HEATMAP_WEEKS tuần gần nhất (việc hoàn thành + thói quen)"
                ) {
                    ActivityHeatmap(counts = state.heatmap, lastDay = state.today, weeks = HEATMAP_WEEKS, todayEpochDay = state.today)
                    HeatmapLegend()
                }
            }
        }
    }

    // ----- Hộp thoại thêm / sửa thói quen -----
    // Chỉ được vẽ khi `habitDialog` khác null. Thêm mới thì không có nút Lưu trữ/Xoá (truyền null).
    habitDialog?.let { habit ->
        val isNew = habit.id == 0L
        HabitDialog(
            initial = if (isNew) null else habit,
            onDismiss = { habitDialog = null },
            onSave = viewModel::saveHabit,
            onArchive = if (isNew) null else ({ viewModel.archiveHabit(habit) }),
            onDelete = if (isNew) null else ({ viewModel.deleteHabit(habit) })
        )
    }
}
