package kr.woorihaksik.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.woorihaksik.app.R
import kr.woorihaksik.app.data.Entry
import kr.woorihaksik.app.data.Repository
import kr.woorihaksik.app.data.Restaurant
import kr.woorihaksik.app.ui.theme.LocalSchoolColor
import kr.woorihaksik.app.ui.theme.T
import kr.woorihaksik.app.ui.theme.W
import kr.woorihaksik.app.ui.theme.onColorFor
import kr.woorihaksik.app.util.hoursLabel
import kr.woorihaksik.app.util.isWeekend
import kr.woorihaksik.app.util.longDate
import kr.woorihaksik.app.util.openStatus
import kr.woorihaksik.app.util.dowShort
import kr.woorihaksik.app.util.today
import kr.woorihaksik.app.util.updatedLabel
import kr.woorihaksik.app.util.weekDays
import kr.woorihaksik.app.util.weekStart

@Composable
fun TodayScreen(s: UiState, vm: AppViewModel) {
    val school = s.school ?: return
    val accent = LocalSchoolColor.current
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // 상단: 워드마크 + 학교 버튼
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("우리학식", style = T.Wordmark, color = accent, modifier = Modifier.weight(1f))
            Row(
                Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, W.Border, RoundedCornerShape(18.dp))
                    .clickable(role = Role.Button) { vm.openSchoolPicker() }
                    .semantics { contentDescription = "학교 변경, 현재 ${school.name}" }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(school.name, style = T.Label)
                Icon(painterResource(R.drawable.ic_chevron_down), null, Modifier.size(14.dp), tint = W.Ink)
            }
        }

        val data = s.data
        val campus = s.campus
        if (data != null && campus != null && data.campuses.size > 1) {
            UnderlineTabs(
                items = data.campuses.map { it.id to it.name },
                selectedId = campus.id,
                onSelect = vm::selectCampus,
            )
        } else {
            HorizontalDivider(color = W.Line, thickness = 1.dp)
        }

        DateStrip(s, vm)

        when {
            data == null && (s.loading || s.booting) -> Loading()
            data == null || campus == null -> {
                CenterMessage("식단을 불러오지 못했어요", s.error ?: "잠시 후 다시 시도해 주세요.")
                RetryButton(vm)
            }
            else -> DayContent(s, vm)
        }
    }
}

@Composable
private fun DateStrip(s: UiState, vm: AppViewModel) {
    val accent = LocalSchoolColor.current
    val start = weekStart(s.selectedDate)
    val t = today()
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { vm.moveDayStrip(-1) }, modifier = Modifier.size(40.dp)) {
            Icon(painterResource(R.drawable.ic_chevron_left), "지난주", tint = W.Muted, modifier = Modifier.size(18.dp))
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            weekDays(start).forEach { d ->
                val on = d == s.selectedDate
                Column(
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (on) accent else W.Surface)
                        .semantics { selected = on }
                        .clickable(role = Role.Tab) { vm.selectDate(d) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val fg = if (on) onColorFor(accent) else W.Ink
                    Text(if (d == t) "오늘" else dowShort(d), style = T.Small, color = fg)
                    Text("${d.dayOfMonth}", style = T.Title, color = fg)
                }
            }
        }
        IconButton(onClick = { vm.moveDayStrip(1) }, modifier = Modifier.size(40.dp)) {
            Icon(painterResource(R.drawable.ic_chevron_right), "다음 주", tint = W.Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DayContent(s: UiState, vm: AppViewModel) {
    val data = s.data ?: return
    val campus = s.campus ?: return
    val date = s.selectedDate

    // 식단이 한 번이라도 있는 식당만, 즐겨찾기 먼저.
    val rests = campus.restaurants
        .filter { it.hasMenu }
        .sortedBy { if (favKey(campus.id, it.id) in s.favorites) 0 else 1 }
    val rows: List<Pair<Restaurant, Entry?>> = rests.map { it to data.entry(date, campus.id, it.id) }
    val withMenu = rows.filter { it.second != null }

    // 큰 카드: 즐겨찾기 → 지금 운영 중 → 첫 번째 순.
    val hero = withMenu.firstOrNull { favKey(campus.id, it.first.id) in s.favorites }
        ?: withMenu.firstOrNull { openStatus(it.first.hours, date)?.isOpen == true }
        ?: withMenu.firstOrNull()
    val others = rows.filter { it.first.id != hero?.first?.id }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (s.origin == Repository.Origin.SAMPLE) {
            item { Notice("인터넷 데이터를 아직 못 받아서, 앱에 들어 있는 샘플 식단을 보여주고 있어요.") }
        }
        item {
            Text(longDate(date), style = T.Label, color = W.Muted)
        }
        if (hero == null) {
            item {
                val last = data.lastMenuDate(campus.id)
                when {
                    last != null && date.isAfter(last) ->
                        CenterMessage("아직 식단이 올라오지 않았어요", "학교는 보통 한 주씩 식단을 올려요. 올라오면 바로 보여드릴게요.")
                    isWeekend(date) -> CenterMessage("주말에는 식단이 없어요")
                    else -> CenterMessage("이 날은 식단 정보가 없어요", "휴일이거나 학교가 식단을 올리지 않은 날이에요.")
                }
            }
        } else {
            item(key = "hero") { HeroCard(hero.first, hero.second!!, s, vm, campus.id) }
            items(others, key = { it.first.id }) { (r, e) -> RestaurantSection(r, e, s, vm, campus.id) }
        }
        item { Footer(s, vm) }
    }
}

@Composable
private fun HeroCard(r: Restaurant, e: Entry, s: UiState, vm: AppViewModel, campusId: String) {
    val accent = LocalSchoolColor.current
    val fg = onColorFor(accent)
    val status = openStatus(r.hours, s.selectedDate)
    SchoolCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    status?.let { StatusLabel(it, onDark = true) }
                    Text("${r.name} 식당", style = T.HeroTitle, color = fg)
                    hoursLabel(r.hours)?.let { Text(it, style = T.Label.copy(fontWeight = FontWeight.Medium), color = W.OnHeroMuted) }
                }
                Box(Modifier.padding(top = 0.dp)) {
                    FavoriteButton(favKey(campusId, r.id) in s.favorites, fg) { vm.toggleFavorite(campusId, r.id) }
                }
            }
            HorizontalDivider(color = fg.copy(alpha = 0.18f), thickness = 1.dp)
            MenuList(e, onDark = true)
        }
    }
}

@Composable
private fun RestaurantSection(r: Restaurant, e: Entry?, s: UiState, vm: AppViewModel, campusId: String) {
    val status = openStatus(r.hours, s.selectedDate)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${r.name} 식당", style = T.Section, modifier = Modifier.weight(1f))
            status?.let { StatusLabel(it, onDark = false) }
            FavoriteButton(favKey(campusId, r.id) in s.favorites, W.Ink) { vm.toggleFavorite(campusId, r.id) }
        }
        GrayCard {
            if (e == null) {
                Text("이 날은 등록된 식단이 없어요", style = T.Body.copy(fontWeight = FontWeight.Normal), color = W.Muted)
            } else {
                MenuList(e, onDark = false)
            }
        }
    }
}

@Composable
private fun Notice(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(W.Surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) { Text(text, style = T.Small, color = W.Ink) }
}

@Composable
private fun Footer(s: UiState, vm: AppViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val updated = updatedLabel(s.data?.generatedAt)
            Text(
                if (updated != null) "$updated 업데이트" else "업데이트 시간 정보 없음",
                style = T.Small,
                color = W.Muted,
            )
            Text("학교 홈페이지 기준 비공식 정보예요", style = T.Small, color = W.Muted)
            s.error?.let { Text(it, style = T.Small, color = W.Ink) }
        }
        IconButton(onClick = vm::refresh, enabled = !s.loading, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_refresh), "새로고침", tint = if (s.loading) W.Border else W.Ink, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun RetryButton(vm: AppViewModel) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            "다시 시도",
            style = T.Label,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, W.Border, RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = vm::refresh)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}
