package kr.woorihaksik.app.util

import kr.woorihaksik.app.data.Hours
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

val KST: ZoneId = ZoneId.of("Asia/Seoul")

fun today(): LocalDate = LocalDate.now(KST)

fun nowKst(): LocalDateTime = LocalDateTime.now(KST)

fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun weekDays(start: LocalDate): List<LocalDate> = (0L..4L).map { start.plusDays(it) }

/** 주말에 열면 다음 주 월요일을 보여준다. */
fun defaultDate(t: LocalDate = today()): LocalDate = when (t.dayOfWeek) {
    DayOfWeek.SATURDAY -> t.plusDays(2)
    DayOfWeek.SUNDAY -> t.plusDays(1)
    else -> t
}

fun isWeekend(d: LocalDate) = d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY

fun dowShort(d: LocalDate): String = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)

/** 11월 9일 월요일 */
fun longDate(d: LocalDate): String =
    "${d.monthValue}월 ${d.dayOfMonth}일 ${d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)}"

/** 11.9 월 */
fun shortDate(d: LocalDate): String = "${d.monthValue}.${d.dayOfMonth} ${dowShort(d)}"

private val updatedFormat = DateTimeFormatter.ofPattern("M월 d일 a h:mm", Locale.KOREAN)

fun updatedLabel(t: OffsetDateTime?): String? = t?.atZoneSameInstant(KST)?.format(updatedFormat)

private val won = NumberFormat.getNumberInstance(Locale.KOREA)

fun priceLabel(price: Int?): String? = when {
    price == null -> null
    price == 0 -> "무료"
    else -> won.format(price.toLong()) + "원"
}

private fun parseTime(s: String): LocalTime? = runCatching {
    val (h, m) = s.trim().split(":").map { it.toInt() }
    LocalTime.of(h, m)
}.getOrNull()

/** "09:00" → "9:00" 처럼 보기 좋게. */
private fun pretty(s: String): String = parseTime(s)?.let { "${it.hour}:%02d".format(it.minute) } ?: s

fun hoursLabel(h: Hours?): String? {
    h ?: return null
    val base = "${pretty(h.open)} – ${pretty(h.close)}"
    if (h.breaks.isEmpty()) return base
    return base + " · 휴게 " + h.breaks.joinToString(", ") { "${pretty(it.from)} – ${pretty(it.to)}" }
}

data class OpenStatus(val label: String, val isOpen: Boolean)

/** 오늘 날짜일 때만 운영 상태를 계산한다. */
fun openStatus(h: Hours?, date: LocalDate, now: LocalDateTime = nowKst()): OpenStatus? {
    if (h == null || date != now.toLocalDate()) return null
    val open = parseTime(h.open) ?: return null
    val close = parseTime(h.close) ?: return null
    val t = now.toLocalTime()
    if (t.isBefore(open)) return OpenStatus("${pretty(h.open)} 오픈", false)
    if (!t.isBefore(close)) return OpenStatus("영업 종료", false)
    for (b in h.breaks) {
        val from = parseTime(b.from) ?: continue
        val to = parseTime(b.to) ?: continue
        if (!t.isBefore(from) && t.isBefore(to)) return OpenStatus("쉬는 시간 · ${pretty(b.to)}에 다시 열어요", false)
    }
    return OpenStatus("운영 중 · ${pretty(h.close)}까지", true)
}
