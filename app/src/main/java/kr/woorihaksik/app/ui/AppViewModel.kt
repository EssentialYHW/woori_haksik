package kr.woorihaksik.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.woorihaksik.app.data.Campus
import kr.woorihaksik.app.data.MenuData
import kr.woorihaksik.app.data.Prefs
import kr.woorihaksik.app.data.Repository
import kr.woorihaksik.app.data.School
import kr.woorihaksik.app.util.defaultDate
import kr.woorihaksik.app.util.weekStart
import java.time.LocalDate

enum class Tab { TODAY, WEEKLY, MY }

data class UiState(
    val schools: List<School> = emptyList(),
    val school: School? = null,
    val pickingSchool: Boolean = false,
    val booting: Boolean = true,
    val data: MenuData? = null,
    val origin: Repository.Origin? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val campusId: String? = null,
    val selectedDate: LocalDate = defaultDate(),
    val weekStart: LocalDate = weekStart(defaultDate()),
    val weeklyRestaurantId: String? = null,
    val favorites: Set<String> = emptySet(),
    val notify: Boolean = false,
    val tab: Tab = Tab.TODAY,
) {
    val campus: Campus?
        get() = data?.campuses?.firstOrNull { it.id == campusId } ?: data?.campuses?.firstOrNull()
}

fun favKey(campusId: String, restaurantId: String) = "$campusId/$restaurantId"

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)
    private val prefs = Prefs(app)

    private val _state = MutableStateFlow(UiState(notify = prefs.notify))
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val schools = repo.localSchools()
            val saved = prefs.schoolId?.let { id -> schools.firstOrNull { it.id == id } }
            _state.update { it.copy(schools = schools, booting = false, pickingSchool = saved == null) }
            if (saved != null) openSchool(saved)

            repo.remoteSchools()?.let { remote ->
                _state.update { st ->
                    st.copy(
                        schools = remote,
                        school = st.school?.let { cur -> remote.firstOrNull { it.id == cur.id } ?: cur },
                    )
                }
            }
        }
    }

    // ---------------------------------------------------------------- 학교

    fun openSchoolPicker() = _state.update { it.copy(pickingSchool = true) }

    fun closeSchoolPicker() = _state.update { if (it.school != null) it.copy(pickingSchool = false) else it }

    fun pickSchool(id: String) {
        val st = _state.value
        val school = st.schools.firstOrNull { it.id == id } ?: return
        if (st.school?.id == id && st.data != null) {
            _state.update { it.copy(pickingSchool = false) }
        } else {
            openSchool(school)
        }
    }

    private fun openSchool(school: School) {
        prefs.schoolId = school.id
        val date = defaultDate()
        _state.update {
            it.copy(
                school = school,
                pickingSchool = false,
                favorites = prefs.favorites(school.id),
                data = null,
                origin = null,
                error = null,
                campusId = prefs.campus(school.id),
                selectedDate = date,
                weekStart = weekStart(date),
                weeklyRestaurantId = null,
                tab = Tab.TODAY,
            )
        }
        viewModelScope.launch {
            repo.localMenu(school)?.let { applyData(it.data, it.origin) }
            refresh()
        }
    }

    // ---------------------------------------------------------------- 데이터

    fun refresh() {
        val school = _state.value.school ?: return
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val remote = if (repo.hasRemote) repo.remoteMenu(school) else null
            if (remote != null && _state.value.school?.id == school.id) {
                applyData(remote, Repository.Origin.NETWORK)
            }
            _state.update {
                it.copy(
                    loading = false,
                    error = if (repo.hasRemote && remote == null) "최신 식단을 받지 못했어요. 인터넷 연결을 확인해 주세요." else null,
                )
            }
        }
    }

    private fun applyData(data: MenuData, origin: Repository.Origin) {
        _state.update { st ->
            val campusId = st.campusId?.takeIf { id -> data.campuses.any { it.id == id } }
                ?: data.campuses.firstOrNull()?.id
            st.copy(data = data, origin = origin, campusId = campusId)
        }
    }

    // ---------------------------------------------------------------- 화면 조작

    fun selectTab(tab: Tab) = _state.update { it.copy(tab = tab) }

    fun selectCampus(id: String) {
        val school = _state.value.school ?: return
        prefs.setCampus(school.id, id)
        _state.update { it.copy(campusId = id, weeklyRestaurantId = null) }
    }

    fun selectDate(date: LocalDate) = _state.update { it.copy(selectedDate = date) }

    /** 주간 화면의 주 이동. 학식 화면에서 고른 날짜는 그대로 둔다. */
    fun moveWeek(deltaWeeks: Long) = _state.update { it.copy(weekStart = it.weekStart.plusWeeks(deltaWeeks)) }

    /** 오늘 화면의 날짜 줄을 다른 주로 넘길 때. */
    fun moveDayStrip(deltaWeeks: Long) = _state.update {
        it.copy(selectedDate = weekStart(it.selectedDate).plusWeeks(deltaWeeks))
    }

    fun selectWeeklyRestaurant(id: String) = _state.update { it.copy(weeklyRestaurantId = id) }

    fun toggleFavorite(campusId: String, restaurantId: String) {
        val school = _state.value.school ?: return
        val key = favKey(campusId, restaurantId)
        val next = _state.value.favorites.let { if (key in it) it - key else it + key }
        prefs.setFavorites(school.id, next)
        _state.update { it.copy(favorites = next) }
    }

    fun setNotify(on: Boolean) {
        prefs.notify = on
        _state.update { it.copy(notify = on) }
    }
}
