package kr.woorihaksik.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.woorihaksik.app.R
import kr.woorihaksik.app.ui.theme.T
import kr.woorihaksik.app.ui.theme.W
import kr.woorihaksik.app.util.shortDate
import kr.woorihaksik.app.util.today
import kr.woorihaksik.app.util.weekDays
import kr.woorihaksik.app.util.weekStart

@Composable
fun WeeklyScreen(s: UiState, vm: AppViewModel) {
    val school = s.school ?: return
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("주간 메뉴", style = T.Wordmark, modifier = Modifier.weight(1f))
            Text(
                listOfNotNull(school.shortName, s.campus?.name).joinToString(" · "),
                style = T.Label,
                color = W.Muted,
            )
        }
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
    run {
        UnderlineTabs(
            items = rests.map { it.id to it.name },
            selectedId = restaurant.id,
            onSelect = vm::selectWeeklyRestaurant,
        )

        // 주 이동: 데이터가 있는 범위 안에서만.
        val start = s.weekStart
        val minStart = data.from?.let(::weekStart)
        val maxStart = listOfNotNull(data.to, today()).maxOrNull()?.let(::weekStart)
        val canPrev = minStart == null || start.isAfter(minStart)
        val canNext = maxStart == null || start.isBefore(maxStart)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.moveWeek(-1) }, enabled = canPrev, modifier = Modifier.size(48.dp)) {
                Icon(painterResource(R.drawable.ic_chevron_left), "지난주", tint = if (canPrev) W.Ink else W.Border, modifier = Modifier.size(20.dp))
            }
            val end = start.plusDays(4)
            Text(
                "${start.monthValue}.${start.dayOfMonth} – ${end.monthValue}.${end.dayOfMonth}",
                style = T.BodyStrong.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = { vm.moveWeek(1) }, enabled = canNext, modifier = Modifier.size(48.dp)) {
                Icon(painterResource(R.drawable.ic_chevron_right), "다음 주", tint = if (canNext) W.Ink else W.Border, modifier = Modifier.size(20.dp))
            }
        }

        val t = today()
        val last = data.lastMenuDate(campus.id)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(weekDays(start), key = { it.toString() }) { d ->
                val e = data.entry(d, campus.id, restaurant.id)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(shortDate(d), style = T.Section)
                        if (d == t) Text("오늘", style = T.Label.copy(fontWeight = FontWeight.Bold), color = W.GreenText)
                    }
                    when {
                        e != null && d == t -> SchoolCard(radius = 12, padding = 16) { MenuList(e, onDark = true, compact = true) }
                        e != null -> GrayCard { MenuList(e, onDark = false, compact = true) }
                        last != null && d.isAfter(last) -> GrayCard {
                            Text("아직 식단이 올라오지 않았어요", style = T.Body.copy(fontWeight = FontWeight.Normal), color = W.Muted)
                        }
                        else -> GrayCard {
                            Text("식단 정보가 없어요", style = T.Body.copy(fontWeight = FontWeight.Normal), color = W.Muted)
                        }
                    }
                }
            }
        }
    }
}
