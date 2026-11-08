package kr.woorihaksik.app.ui

import kr.woorihaksik.app.data.Entry
import kr.woorihaksik.app.data.MenuData
import kr.woorihaksik.app.data.MenuItem
import kr.woorihaksik.app.util.weekDays
import kr.woorihaksik.app.util.weekStart
import java.time.LocalDate

/**
 * 화면에 보여주기 좋게 메뉴를 나눈다.
 * - 천원의 아침밥: 따로 강조
 * - 매일 나오는 메뉴(그 주 모든 날 똑같은 메뉴): 한 줄로 접어 둔다
 * - 나머지(그날만 나오는 메뉴): 그대로 보여준다
 */
data class ShapedGroup(val label: String?, val items: List<MenuItem>)

data class ShapedMenu(
    val breakfast: List<MenuItem>,
    val groups: List<ShapedGroup>,
    val daily: List<MenuItem>,
) {
    val changingCount: Int get() = groups.sumOf { it.items.size }
}

fun itemKey(i: MenuItem): String = i.name + "|" + (i.price ?: -1) + "|" + i.sides.joinToString(",")

fun isBreakfast(i: MenuItem): Boolean = i.name.replace(" ", "").contains("천원의아침")

/** 그 주(월~금)에 식단이 3일 이상 있을 때, 모든 날에 똑같이 나온 메뉴. */
fun dailyKeys(data: MenuData, date: LocalDate, campusId: String, restaurantId: String): Set<String> {
    val days = weekDays(weekStart(date)).mapNotNull { data.entry(it, campusId, restaurantId) }
    if (days.size < 3) return emptySet()
    return days
        .map { e -> e.meals.flatMap { it.items }.filterNot(::isBreakfast).map(::itemKey).toSet() }
        .reduce { a, b -> a intersect b }
}

fun shape(entry: Entry, daily: Set<String>): ShapedMenu {
    val all = entry.meals.flatMap { it.items }
    val groups = entry.meals.mapNotNull { m ->
        val items = m.items.filter { !isBreakfast(it) && itemKey(it) !in daily }
        when {
            items.isEmpty() -> null
            // '정식' 칸에 '정식(7000)' 하나만 있으면 칸 이름을 또 쓰지 않는다.
            items.size == 1 && items[0].name == m.type -> ShapedGroup(null, items)
            else -> ShapedGroup(m.type, items)
        }
    }
    return ShapedMenu(
        breakfast = all.filter(::isBreakfast),
        groups = groups,
        daily = all.filter { !isBreakfast(it) && itemKey(it) in daily }.distinctBy(::itemKey),
    )
}

/** "[09:00~선착순제공]" → "09:00부터 선착순" */
fun prettyNote(note: String): String =
    note.replace("~선착순제공", "부터 선착순").replace("~ 선착순제공", "부터 선착순")
