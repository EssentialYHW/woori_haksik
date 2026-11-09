package kr.woorihaksik.app.data

import android.content.Context

/** 휴대폰에 저장하는 사용자 설정. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("woori", Context.MODE_PRIVATE)

    var schoolId: String?
        get() = sp.getString("school", null)
        set(value) = sp.edit().putString("school", value).apply()

    var notify: Boolean
        get() = sp.getBoolean("notify", false)
        set(value) = sp.edit().putBoolean("notify", value).apply()

    fun campus(schoolId: String): String? = sp.getString("campus_$schoolId", null)

    fun setCampus(schoolId: String, campusId: String) =
        sp.edit().putString("campus_$schoolId", campusId).apply()

    /** "캠퍼스id/식당id" 목록. */
    fun favorites(schoolId: String): Set<String> =
        sp.getStringSet("fav_$schoolId", emptySet())?.toSet().orEmpty()

    fun setFavorites(schoolId: String, keys: Set<String>) =
        sp.edit().putStringSet("fav_$schoolId", keys).apply()
}
