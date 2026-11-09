package kr.woorihaksik.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.woorihaksik.app.R
import kr.woorihaksik.app.data.Entry
import kr.woorihaksik.app.data.MenuItem
import kr.woorihaksik.app.ui.theme.LocalSchoolColor
import kr.woorihaksik.app.ui.theme.T
import kr.woorihaksik.app.ui.theme.W
import kr.woorihaksik.app.ui.theme.onColorFor
import kr.woorihaksik.app.util.OpenStatus
import kr.woorihaksik.app.util.priceLabel

/** 밑줄 탭 (캠퍼스, 식당). */
@Composable
fun UnderlineTabs(
    items: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalSchoolColor.current
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items.forEach { (id, label) ->
                val on = id == selectedId
                Box(
                    Modifier
                        .height(48.dp)
                        .semantics { selected = on }
                        .clickable(role = Role.Tab) { onSelect(id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = T.Tab.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
                        color = if (on) accent else W.Muted2,
                    )
                    // 가로 스크롤 안에서도 글자 폭만큼 밑줄이 그려지도록 matchParentSize 안에 둔다.
                    Box(Modifier.matchParentSize(), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(if (on) accent else Color.Transparent),
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = W.Line, thickness = 1.dp)
    }
}

/** 운영 상태 줄: 초록 점 + 글씨 (운영 중일 때만 초록). */
@Composable
fun StatusLabel(status: OpenStatus, onDark: Boolean) {
    val color = when {
        status.isOpen && onDark -> W.Green
        status.isOpen -> W.GreenText
        onDark -> W.OnHeroMuted
        else -> W.Muted
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (status.isOpen) Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(status.label, style = T.Label.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

@Composable
fun FavoriteButton(on: Boolean, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(
            painterResource(if (on) R.drawable.ic_heart else R.drawable.ic_heart_outline),
            contentDescription = if (on) "즐겨찾기 해제" else "즐겨찾기 추가",
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** 한 식당의 하루 메뉴 목록. onDark = 학교색 카드 위. */
@Composable
fun MenuList(entry: Entry, onDark: Boolean, compact: Boolean = false) {
    val main = if (onDark) onColorFor(LocalSchoolColor.current) else W.Ink
    val sub = if (onDark) W.OnHeroMuted else W.Muted
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)) {
        entry.meals.forEachIndexed { mi, meal ->
            if (entry.meals.size > 1 || meal.type != "정식") {
                Text(
                    meal.type,
                    style = T.SmallStrong,
                    color = sub,
                    modifier = Modifier.padding(top = if (mi == 0) 0.dp else 6.dp),
                )
            }
            var lastVenue: String? = null
            meal.items.forEach { item ->
                if (item.venue != null && item.venue != lastVenue) {
                    Text(item.venue, style = T.SmallStrong, color = sub, modifier = Modifier.padding(top = 4.dp))
                }
                lastVenue = item.venue
                MenuRow(item, main, sub, compact)
            }
        }
    }
}

@Composable
private fun MenuRow(item: MenuItem, main: Color, sub: Color, compact: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.name, style = if (compact) T.Body else T.BodyStrong, color = main)
            if (item.sides.isNotEmpty()) {
                Text(item.sides.joinToString(" · "), style = T.Small, color = sub)
            }
            if (!compact) {
                item.extras.forEach { ex ->
                    Text("${ex.label} · " + ex.items.joinToString(" · "), style = T.Small, color = sub)
                }
            }
            item.notes.forEach { Text(it, style = T.Small, color = sub) }
        }
        priceLabel(item.price)?.let { Text(it, style = T.Price, color = main) }
    }
}

@Composable
fun GrayCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(W.Surface)
            .padding(16.dp),
    ) { content() }
}

@Composable
fun SchoolCard(modifier: Modifier = Modifier, radius: Int = 16, padding: Int = 20, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.dp))
            .background(LocalSchoolColor.current)
            .padding(padding.dp),
    ) { content() }
}

@Composable
fun CenterMessage(title: String, body: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = T.Title, textAlign = TextAlign.Center)
        if (body != null) Text(body, style = T.Body.copy(fontWeight = FontWeight.Normal), color = W.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LocalSchoolColor.current, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
    }
}

@Composable
fun BottomBar(tab: Tab, onTab: (Tab) -> Unit) {
    val accent = LocalSchoolColor.current
    Column(Modifier.background(W.White).navigationBarsPadding()) {
        HorizontalDivider(color = W.Line, thickness = 1.dp)
        Row(Modifier.fillMaxWidth().height(64.dp)) {
            listOf(
                Triple(Tab.TODAY, "학식", R.drawable.ic_meal),
                Triple(Tab.WEEKLY, "주간", R.drawable.ic_calendar),
                Triple(Tab.MY, "내 정보", R.drawable.ic_person),
            ).forEach { (t, label, icon) ->
                val on = t == tab
                val color = if (on) accent else W.Muted2
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height(64.dp)
                        .semantics { selected = on }
                        .clickable(role = Role.Tab) { onTab(t) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = T.Small.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium), color = color)
                }
            }
        }
    }
}

@Composable
fun SmallSpacer(w: Int) = Spacer(Modifier.width(w.dp))
