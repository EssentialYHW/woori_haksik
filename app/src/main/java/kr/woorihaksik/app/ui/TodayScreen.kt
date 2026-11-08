package kr.woorihaksik.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.woorihaksik.app.data.Entry
import kr.woorihaksik.app.data.MenuData
import kr.woorihaksik.app.data.Repository
import kr.woorihaksik.app.data.Restaurant
import kr.woorihaksik.app.ui.theme.LocalSchoolColor
import kr.woorihaksik.app.ui.theme.T
import kr.woorihaksik.app.ui.theme.W
import kr.woorihaksik.app.ui.theme.onColorFor
import kr.woorihaksik.app.util.dowShort
import kr.woorihaksik.app.util.isWeekend
import kr.woorihaksik.app.util.longDate
import kr.woorihaksik.app.util.openStatus
import kr.woorihaksik.app.util.today
import kr.woorihaksik.app.util.updatedLabel
import kr.woorihaksik.app.util.weekDays
import kr.woorihaksik.app.util.weekStart

@Composable
fun TodayScreen(s: UiState, vm: AppViewModel) {
    val school = s.school ?: return
    var picker by remember { mutableStateOf(false) }
    val title = listOfNotNull(school.shortName, s.campus?.name).joinToString(" ")

    Column(Modifier.fillMaxSize().background(W.Bg).statusBarsPadding()) {
        ScreenHeader(
            caption = longDate(s.selectedDate),
            title = title,
            onTitleClick = { picker = true },
            titleDescription = "학교·캠퍼스 바꾸기, 현재 $title",
        )
        DayChips(s, vm)

        val data = s.data
        val campus = s.campus
        when {
            data == null && (s.loading || s.booting) -> Loading()
            data == null || campus == null -> {
                CenterMessage("식단을 불러오지 못했어요", s.error ?: "잠시 후 다시 시도해 주세요.")
                TextButton(onClick = vm::refresh, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("다시 시도", style = T.Label, color = W.Ink)
                }
            }
            else -> AnimatedContent(
                targetState = s.selectedDate to campus.id,
                transitionSpec = { Motion.pagerOrFade(targetState.first.compareTo(initialState.first).coerceIn(-1, 1)) },
                label = "day",
            ) { (date, cid) ->
                DayList(s, vm, data, cid, date)
            }
        }
    }

    if (picker) CampusPicker(s, vm) { picker = false }
}

/** 이번 주 월~금 날짜 고르기. 다른 주는 '주간' 탭에서 본다. */
@Composable
private fun DayChips(s: UiState, vm: AppViewModel) {
    val accent = LocalSchoolColor.current
    val t = today()
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        weekDays(weekStart(s.selectedDate)).forEach { d ->
            val on = d == s.selectedDate
            val bg by animateColorAsState(if (on) accent else W.Bg, tween(Motion.ENTER), label = "chipBg")
            val fg by animateColorAsState(if (on) onColorFor(accent) else W.Ink, tween(Motion.ENTER), label = "chipFg")
            Column(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .semantics { selected = on }
                    .clickable(role = Role.Tab) { vm.selectDate(d) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(if (d == t) "오늘" else dowShort(d), style = T.Small, color = fg)
                Text("${d.dayOfMonth}", style = T.Title, color = fg)
            }
        }
    }
}

@Composable
private fun DayList(s: UiState, vm: AppViewModel, data: MenuData, campusId: String, date: java.time.LocalDate) {
    // 전환 중에는 나가는 화면이 예전 캠퍼스·날짜를 그대로 그려야 하므로 인자로 받은 값을 쓴다.
    val campus = data.campuses.firstOrNull { it.id == campusId } ?: return
    // 식단이 한 번이라도 있는 식당만, 즐겨찾기 먼저.
    val rests = campus.restaurants
        .filter { it.hasMenu }
        .sortedBy { if (favKey(campusId, it.id) in s.favorites) 0 else 1 }
    val rows: List<Pair<Restaurant, Entry?>> = rests.map { it to data.entry(date, campusId, it.id) }
    val withMenu = rows.filter { it.second != null }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (s.origin == Repository.Origin.SAMPLE) {
            item {
                WCard(padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("인터넷 데이터를 아직 못 받아서 샘플 식단을 보여주고 있어요.", style = T.Caption, color = W.Muted)
                }
            }
        }
        if (withMenu.isEmpty()) {
            item {
                WCard {
                    val last = data.lastMenuDate(campusId)
                    when {
                        last != null && date.isAfter(last) ->
                            CenterMessage("아직 식단이 올라오지 않았어요", "학교는 보통 한 주씩 식단을 올려요.\n올라오면 바로 보여드릴게요.")
                        isWeekend(date) -> CenterMessage("주말에는 식단이 없어요")
                        else -> CenterMessage("이 날은 식단 정보가 없어요", "휴일이거나 학교가 식단을 올리지 않은 날이에요.")
                    }
                }
            }
        } else {
            items(withMenu, key = { it.first.id }) { (r, e) ->
                RestaurantCard(r, e!!, s, vm, data, campusId)
            }
            val empty = rows.filter { it.second == null }
            if (empty.isNotEmpty()) {
                item {
                    Text(
                        empty.joinToString(", ") { it.first.name } + "은(는) 이 날 식단이 없어요",
                        style = T.Caption,
                        color = W.Muted,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        item {
            Footer(s, vm)
        }
    }
}

@Composable
private fun RestaurantCard(r: Restaurant, e: Entry, s: UiState, vm: AppViewModel, data: MenuData, campusId: String) {
    val daily = remember(data, e.date, campusId, r.id) { dailyKeys(data, e.date, campusId, r.id) }
    val shaped = remember(e, daily) { shape(e, daily) }
    var expanded by rememberSaveable(e.date.toString(), r.id) { mutableStateOf(false) }
    val status = openStatus(r.hours, e.date)
    val fav = favKey(campusId, r.id) in s.favorites

    WCard(padding = PaddingValues(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) {
        // 머리: 이름 + 상태 / 하트
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(r.name, style = T.CardTitle, color = W.Ink)
                status?.let { StatusLine(it) }
            }
            FavoriteToggle(fav) { vm.toggleFavorite(campusId, r.id) }
        }
        Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            shaped.breakfast.forEach { BreakfastBox(it) }
            if (shaped.groups.isNotEmpty()) {
                MenuGroups(
                    groups = shaped.groups,
                    limit = if (!expanded && shaped.changingCount > 5) 4 else null,
                    onMore = { expanded = true },
                )
            }
            DailyMenuSection(shaped.daily, divider = shaped.groups.isNotEmpty() || shaped.breakfast.isNotEmpty())
        }
    }
}

@Composable
private fun Footer(s: UiState, vm: AppViewModel) {
    val updated = updatedLabel(s.data?.generatedAt)
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            "학교 홈페이지 기준 비공식 정보" + if (updated != null) " · $updated 업데이트" else "",
            style = T.Small,
            color = W.Muted,
            textAlign = TextAlign.Center,
        )
        s.error?.let { Text(it, style = T.Small, color = W.Ink, textAlign = TextAlign.Center) }
        TextButton(onClick = vm::refresh, enabled = !s.loading) {
            Text(if (s.loading) "불러오는 중…" else "새로고침", style = T.Label, color = W.Muted)
        }
    }
}

/** 제목을 누르면: 캠퍼스 고르기 + 학교 바꾸기. */
@Composable
private fun CampusPicker(s: UiState, vm: AppViewModel, onClose: () -> Unit) {
    val accent = LocalSchoolColor.current
    val campuses = s.data?.campuses.orEmpty()
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = W.Card,
        title = { Text(s.school?.name ?: "학교", style = T.Title, color = W.Ink) },
        text = {
            Column {
                campuses.forEach { c ->
                    val on = c.id == s.campus?.id
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(selected = on, role = Role.RadioButton) {
                                vm.selectCampus(c.id)
                                onClose()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = on, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = accent))
                        Text(c.name + "캠퍼스", style = T.Row, color = W.Ink)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onClose()
                vm.openSchoolPicker()
            }) { Text("학교 바꾸기", style = T.Label.copy(fontWeight = FontWeight.Bold), color = accent) }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("닫기", style = T.Label, color = W.Muted) }
        },
    )
}
