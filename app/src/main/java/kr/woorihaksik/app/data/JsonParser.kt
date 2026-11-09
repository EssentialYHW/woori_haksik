package kr.woorihaksik.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.OffsetDateTime

/** 수집기가 만든 JSON(data/schools.json, data/<학교>/latest.json) 읽기. */
object JsonParser {

    fun schools(text: String): List<School> {
        val arr = JSONObject(text).getJSONArray("schools")
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            School(
                id = o.getString("id"),
                name = o.getString("name"),
                shortName = o.optString("shortName", o.getString("name")),
                region = o.optString("region"),
                campusSummary = o.optString("campusSummary"),
                color = parseColor(o.optString("color", "#111111")),
                colorName = o.optString("colorName"),
                dataPath = o.getString("dataPath"),
            )
        }
    }

    fun menu(text: String): MenuData {
        val root = JSONObject(text)
        val range = root.optJSONObject("range")
        val campuses = root.getJSONArray("campuses").objects().map { c ->
            Campus(
                id = c.getString("id"),
                name = c.getString("name"),
                restaurants = c.getJSONArray("restaurants").objects().map { r ->
                    Restaurant(
                        id = r.getString("id"),
                        name = r.getString("name"),
                        hours = r.optJSONObject("hours")?.let(::hours),
                        hasMenu = r.optBoolean("hasMenu", true),
                    )
                },
            )
        }
        val entries = root.getJSONArray("entries").objects().map { e ->
            Entry(
                date = LocalDate.parse(e.getString("date")),
                campusId = e.getString("campus"),
                restaurantId = e.getString("restaurant"),
                meals = e.getJSONArray("meals").objects().map { m ->
                    Meal(type = m.getString("type"), items = m.getJSONArray("items").objects().map(::item))
                },
            )
        }
        return MenuData(
            schoolId = root.getString("school"),
            source = root.optString("source"),
            generatedAt = runCatching { OffsetDateTime.parse(root.getString("generatedAt")) }.getOrNull(),
            from = range?.optString("from")?.takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
            to = range?.optString("to")?.takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) },
            campuses = campuses,
            entries = entries,
        )
    }

    private fun hours(o: JSONObject) = Hours(
        open = o.getString("open"),
        close = o.getString("close"),
        breaks = o.optJSONArray("breaks").objects().map { TimeRange(it.getString("from"), it.getString("to")) },
    )

    private fun item(o: JSONObject) = MenuItem(
        name = o.getString("name"),
        price = if (o.has("price") && !o.isNull("price")) o.getInt("price") else null,
        sides = o.optJSONArray("sides").strings(),
        extras = o.optJSONArray("extras").objects().map { Extra(it.getString("label"), it.optJSONArray("items").strings()) },
        notes = o.optJSONArray("notes").strings(),
        venue = if (o.has("venue") && !o.isNull("venue")) o.getString("venue") else null,
    )

    private fun parseColor(hex: String): Long {
        val clean = hex.removePrefix("#")
        val argb = if (clean.length == 6) "FF$clean" else clean
        return argb.toLongOrNull(16) ?: 0xFF111111
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else List(length()) { getJSONObject(it) }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else List(length()) { getString(it) }
}
