package kr.woorihaksik.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.woorihaksik.app.R
import kr.woorihaksik.app.ui.theme.T
import kr.woorihaksik.app.ui.theme.W
import kr.woorihaksik.app.util.today
import kr.woorihaksik.app.util.weekDays
import kr.woorihaksik.app.util.weekStart
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeeklyScreen(s: UiState, vm: AppViewModel) {
    val school = s.school ?: return
    Column(Modifier.fillMaxSize().background(W.Bg).statusBarsPadding()) {
        ScreenHeader(caption = listOfNotNull(school.shortName, s.campus?.name).joinToString(" "), title = "이번 주 메뉴")
        WeeklyBody(s, vm)
    }
}

@Composable
private fun WeeklyBody(s: UiState, vm: AppViewModel) {
    val data = s.data
    val campus = s.campus
    if (data == null || campus == null) {
        if (s.loading || s.booting) Loading() else CenterMessage("식단을 불러오지 못했어요", s.error)
        return
    }
    val rests = campus.restaurants.filter { it.hasMenu }
    val restaurant = rests.firstOrNull { it.id == s.weeklyRestaurantId }
        ?: rests.firstOrNull { favKey(campus.id, it.id) in s.favorites }
        ?: rests.firstOrNull()
    if (restaurant == null) {
        CenterMessage("이 캠퍼스는 아직 식단 정보가 없어요")
        return
    }

    Segmented(
        items = rests.map { it.id to it.name },
        selectedId = restaurant.id,
        onSelect = vm::selectWeeklyRestaurant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
    )

    // 주 이동: 데이터가 있는 범위 안에서만.
    val start = s.weekStart
    val minStart = data.from?.let { weekStart(it) }
    val maxStart = listOfNotNull(data.to, today()).maxOrNull()?.let { weekStart(it) }
    val canPrev = minStart == null || start.isAfter(minStart)
    val canNext = maxStart == null || start.isBefore(maxStart)
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { vm.moveWeek(-1) }, enabled = canPrev, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_chevron_left), "지난주", tint = if (canPrev) W.Muted else W.Line, modifier = Modifier.size(20.dp))
        }
        val end = start.plusDays(4)
        val range = if (start.monthValue == end.monthValue) {
            "${start.monthValue}월 ${start.dayOfMonth}일 – ${end.dayOfMonth}일"
        } else {
            "${start.monthValue}월 ${start.dayOfMonth}일 – ${end.monthValue}월 ${end.dayOfMonth}일"
        }
        Text(range, style = T.BodyStrong, color = W.Muted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        IconButton(onClick = { vm.moveWeek(1) }, enabled = canNext, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_chevron_right), "다음 주", tint = if (canNext) W.Muted else W.Line, modifier = Modifier.size(20.dp))
        }
    }

    val daily = remember(data, start, campus.id, restaurant.id) { dailyKeys(data, start, campus.id, restaurant.id) }
    val days = weekDays(start)
    val dailyItems = remember(data, daily, restaurant.id) {
        days.firstNotNullOfOrNull { d -> data.entry(d, campus.id, restaurant.id) }?.let { shape(it, daily).daily }.orEmpty()
    }
    val t = today()
    val last = data.lastMenuDate(campus.id)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (dailyItems.isNotEmpty()) {
            item(key = "daily") {
                WCard(padding = PaddingValues(horizontal = 20.dp, vertical = 4.dp), spacing = 0) {
                    DailyMenuSection(dailyItems, divider = false)
                }
            }
        }
        items(days, key = { it.toString() }) { d ->
            val e = data.entry(d, campus.id, restaurant.id)
            WCard(padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp), spacing = 12) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN), style = T.Title, color = W.Ink)
                    Text("${d.monthValue}.${d.dayOfMonth}", style = T.Body, color = W.Muted2)
                    if (d == t) Chip("오늘", filled = true)
                }
                when {
                    e != null -> {
                        val shaped = shape(e, daily)
                        shaped.breakfast.forEach { BreakfastBox(it, compact = true) }
                        if (shaped.groups.isNotEmpty()) {
                            MenuGroups(shaped.groups.map { it.copy(label = null) }, limit = null, onMore = null)
                        } else if (shaped.breakfast.isEmpty()) {
                            Text("매일 나오는 메뉴만 있어요", style = T.Body, color = W.Muted2)
                        }
                    }
                    last != null && d.isAfter(last) -> Text("아직 식단이 올라오지 않았어요", style = T.Body, color = W.Muted2)
                    else -> Text("식단 정보가 없어요", style = T.Body, color = W.Muted2)
                }
            }
        }
    }
}
