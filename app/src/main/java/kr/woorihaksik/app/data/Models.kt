package kr.woorihaksik.app.data

import java.time.LocalDate
import java.time.OffsetDateTime

/** data/schools.json 의 학교 한 곳. color 는 0xAARRGGBB. */
data class School(
    val id: String,
    val name: String,
    val shortName: String,
    val region: String,
    val campusSummary: String,
    val color: Long,
    val colorName: String,
    val dataPath: String,
)

data class TimeRange(val from: String, val to: String)

data class Hours(val open: String, val close: String, val breaks: List<TimeRange>)

data class Restaurant(
    val id: String,
    val name: String,
    val hours: Hours?,
    val hasMenu: Boolean,
)

data class Campus(val id: String, val name: String, val restaurants: List<Restaurant>)

/** 셀프바처럼 메뉴에 딸린 추가 구성. */
data class Extra(val label: String, val items: List<String>)

data class MenuItem(
    val name: String,
    /** 원 단위. null = 가격 표기 없음, 0 = 무료. */
    val price: Int?,
    val sides: List<String>,
    val extras: List<Extra>,
    val notes: List<String>,
    /** 같은 식당 칸 안의 다른 매장 (예: 학생회관 다방). */
    val venue: String?,
)

data class Meal(val type: String, val items: List<MenuItem>)

/** 하루 × 캠퍼스 × 식당 하나의 식단. */
data class Entry(
    val date: LocalDate,
    val campusId: String,
    val restaurantId: String,
    val meals: List<Meal>,
)

data class MenuData(
    val schoolId: String,
    val source: String,
    val generatedAt: OffsetDateTime?,
    val from: LocalDate?,
    val to: LocalDate?,
    val campuses: List<Campus>,
    val entries: List<Entry>,
) {
    private val index: Map<Triple<LocalDate, String, String>, Entry> =
        entries.associateBy { Triple(it.date, it.campusId, it.restaurantId) }

    fun entry(date: LocalDate, campusId: String, restaurantId: String): Entry? =
        index[Triple(date, campusId, restaurantId)]

    /** 이 캠퍼스에서 식단이 올라온 마지막 날. 그 뒤는 '아직 안 올라옴'. */
    fun lastMenuDate(campusId: String): LocalDate? =
        entries.filter { it.campusId == campusId }.maxOfOrNull { it.date }
}
