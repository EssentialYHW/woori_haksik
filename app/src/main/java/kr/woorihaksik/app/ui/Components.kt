package kr.woorihaksik.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.woorihaksik.app.R
import kr.woorihaksik.app.data.MenuItem
import kr.woorihaksik.app.ui.theme.LocalSchoolColor
import kr.woorihaksik.app.ui.theme.T
import kr.woorihaksik.app.ui.theme.W
import kr.woorihaksik.app.ui.theme.tintOf
import kr.woorihaksik.app.util.OpenStatus
import kr.woorihaksik.app.util.priceLabel

// ---------------------------------------------------------------- 틀

/** 회색 바탕 위 흰 카드. */
@Composable
fun WCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    spacing: Int = 16,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(W.Card)
            .padding(padding)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
        content = content,
    )
}

/** 화면 머리: 작은 설명 + 큰 제목(누르면 바꾸기). */
@Composable
fun ScreenHeader(caption: String?, title: String, onTitleClick: (() -> Unit)? = null, titleDescription: String? = null) {
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
        if (caption != null) Text(caption, style = T.Body, color = W.Muted)
        val rowMod = if (onTitleClick != null) {
            Modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onTitleClick)
                .semantics { if (titleDescription != null) contentDescription = titleDescription }
        } else {
            Modifier.heightIn(min = 44.dp)
        }
        Row(rowMod, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = T.ScreenTitle, color = W.Ink)
            if (onTitleClick != null) {
                Icon(painterResource(R.drawable.ic_chevron_down), null, Modifier.size(22.dp), tint = W.Muted)
            }
        }
    }
}

/** 카드 묶음 위의 작은 제목 (토스 설정 화면처럼). */
@Composable
fun SectionCaption(text: String) {
    Text(text, style = T.CaptionStrong, color = W.Muted, modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp))
}

// ---------------------------------------------------------------- 식당 카드 조각

@Composable
fun StatusLine(status: OpenStatus) {
    val color = if (status.isOpen) W.GreenText else W.Muted2
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (status.isOpen) Box(Modifier.size(6.dp).clip(CircleShape).background(W.Green))
        Text(status.label, style = T.Label, color = color)
    }
}

@Composable
fun FavoriteToggle(on: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(
            painterResource(if (on) R.drawable.ic_heart else R.drawable.ic_heart_outline),
            contentDescription = if (on) "즐겨찾기 해제" else "즐겨찾기 추가",
            tint = if (on) LocalSchoolColor.current else W.Faint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun Chip(text: String, filled: Boolean = false) {
    val school = LocalSchoolColor.current
    Text(
        text,
        style = T.SmallStrong,
        color = if (filled) kr.woorihaksik.app.ui.theme.onColorFor(school) else school,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (filled) school else tintOf(school))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** 천원의 아침밥 상자. */
@Composable
fun BreakfastBox(item: MenuItem, compact: Boolean = false) {
    val main = item.sides.firstOrNull() ?: item.name
    if (compact) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("천원의 아침밥")
            Text(main, style = T.BodyStrong, color = W.Ink, modifier = Modifier.weight(1f))
        }
        return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(W.Surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("천원의 아침밥")
            Text(main, style = T.BodyStrong, color = W.Ink, modifier = Modifier.weight(1f))
            priceLabel(item.price)?.let { Text(it, style = T.BodyStrong, color = W.Muted) }
        }
        item.notes.firstOrNull()?.let { Text(prettyNote(it), style = T.Caption, color = W.Muted) }
    }
}

@Composable
fun MenuItemRow(item: MenuItem, showDetails: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                item.name,
                style = if (item.sides.isNotEmpty()) T.BodyStrong else T.Body,
                color = W.Ink,
                modifier = Modifier.weight(1f),
            )
            priceLabel(item.price)?.let { Text(it, style = T.BodyStrong, color = W.Muted) }
        }
        if (showDetails) {
            val details = buildList {
                if (item.sides.isNotEmpty()) add(item.sides.joinToString(" · "))
                item.extras.forEach { add(it.label + " " + it.items.joinToString(" · ")) }
                item.notes.forEach { add(prettyNote(it)) }
            }
            if (details.isNotEmpty()) {
                Text(details.joinToString("\n"), style = T.Caption, color = W.Muted2)
            }
        }
    }
}

/** 메뉴 묶음들을 그린다. limit 개만 보여주고 나머지는 '더 보기'. */
@Composable
fun MenuGroups(groups: List<ShapedGroup>, limit: Int?, onMore: (() -> Unit)?) {
    val visible = limitGroups(groups, limit)
    val total = groups.sumOf { it.items.size }
    val shown = visible.sumOf { it.items.size }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        visible.forEach { g ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                g.label?.let { Text(it, style = T.CaptionStrong, color = W.Muted2) }
                var lastVenue: String? = null
                g.items.forEach { item ->
                    if (item.venue != null && item.venue != lastVenue) {
                        Text(item.venue, style = T.CaptionStrong, color = W.Muted2)
                    }
                    lastVenue = item.venue
                    MenuItemRow(item)
                }
            }
        }
        if (total > shown && onMore != null) {
            MoreButton(total - shown, onMore)
        }
    }
}

/** 앞에서부터 limit 개 메뉴만 남긴다 (화면 그리기 밖에서 계산). */
private fun limitGroups(groups: List<ShapedGroup>, limit: Int?): List<ShapedGroup> {
    if (limit == null) return groups
    val out = mutableListOf<ShapedGroup>()
    var left = limit
    for (g in groups) {
        if (left <= 0) break
        val take = g.items.take(left)
        out.add(g.copy(items = take))
        left -= take.size
    }
    return out
}

@Composable
fun MoreButton(count: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${count}개 더 보기", style = T.Label, color = W.Muted)
        Icon(painterResource(R.drawable.ic_chevron_down), null, Modifier.size(16.dp), tint = W.Muted)
    }
}

/** '매일 나오는 메뉴 N개' 접기/펼치기. */
@Composable
fun DailyMenuSection(items: List<MenuItem>, divider: Boolean) {
    if (items.isEmpty()) return
    var open by rememberSaveable(items.size, items.firstOrNull()?.name) { mutableStateOf(false) }
    Column {
        if (divider) HorizontalDivider(color = W.Surface, thickness = 1.dp, modifier = Modifier.padding(bottom = 4.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button) { open = !open }
                .semantics { contentDescription = if (open) "매일 나오는 메뉴 접기" else "매일 나오는 메뉴 ${items.size}개 펼치기" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("매일 나오는 메뉴 ${items.size}개", style = T.BodyStrong, color = W.Ink)
                Text(
                    items.take(3).joinToString(" · ") { it.name } + if (items.size > 3) " 외" else "",
                    style = T.Caption,
                    color = W.Muted2,
                    maxLines = 1,
                )
            }
            Icon(
                painterResource(R.drawable.ic_chevron_down),
                null,
                Modifier.size(20.dp).rotate(if (open) 180f else 0f),
                tint = W.Muted2,
            )
        }
        if (open) {
            Column(Modifier.padding(top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                var lastVenue: String? = null
                items.forEach { item ->
                    if (item.venue != null && item.venue != lastVenue) {
                        Text(item.venue, style = T.CaptionStrong, color = W.Muted2, modifier = Modifier.padding(top = 4.dp))
                    }
                    lastVenue = item.venue
                    MenuItemRow(item, showDetails = false)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 공통

/** 회색 트랙 위 흰 알약이 움직이는 선택 버튼 (식당 고르기). */
@Composable
fun Segmented(items: List<Pair<String, String>>, selectedId: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(W.Line)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { (id, label) ->
            val on = id == selectedId
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) W.Card else Color.Transparent)
                    .semantics { selected = on }
                    .clickable(role = Role.Tab) { onSelect(id) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = T.Label.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
                    color = if (on) W.Ink else W.Muted,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 설정 카드 안의 한 줄. */
@Composable
fun SettingRow(label: String, value: String? = null, sub: String? = null, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    val mod = Modifier
        .fillMaxWidth()
        .heightIn(min = if (sub != null) 64.dp else 56.dp)
        .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
        .padding(horizontal = 20.dp)
    Row(mod, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = T.Row, color = W.Ink)
            if (sub != null) Text(sub, style = T.Caption, color = W.Muted2)
        }
        if (value != null) Text(value, style = T.Body, color = W.Muted2)
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(16.dp), tint = W.Faint)
        }
    }
}

@Composable
fun CenterMessage(title: String, body: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = T.Title, color = W.Ink, textAlign = TextAlign.Center)
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
    Column(Modifier.background(W.Card).navigationBarsPadding()) {
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
