package kr.woorihaksik.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kr.woorihaksik.app.BuildConfig
import kr.woorihaksik.app.ui.theme.LocalSchoolColor
import kr.woorihaksik.app.ui.theme.T
import kr.woorihaksik.app.ui.theme.W
import kr.woorihaksik.app.ui.theme.onColorFor
import kr.woorihaksik.app.util.updatedLabel

@Composable
fun MyScreen(s: UiState, vm: AppViewModel) {
    val school = s.school ?: return
    val accent = LocalSchoolColor.current
    val uri = LocalUriHandler.current
    // 화면 본문과 아래 팝업들이 같이 쓰도록 맨 위에서 꺼내 둔다.
    val data = s.data
    val campus = s.campus
    var campusDialog by remember { mutableStateOf(false) }
    var favDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(W.Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(caption = null, title = "내 정보")

        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)) {
            // 내 학교
            WCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(accent))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(school.name, style = T.Title, color = W.Ink)
                        Text(
                            listOfNotNull(campus?.name?.let { it + "캠퍼스" }, school.colorName.ifEmpty { null }).joinToString(" · "),
                            style = T.Caption,
                            color = W.Muted2,
                        )
                    }
                    TextButton(onClick = vm::openSchoolPicker) { Text("변경", style = T.Label, color = W.Muted) }
                }
            }

            if (data != null && campus != null) {
                SectionCaption("학식")
                WCard(padding = PaddingValues(vertical = 4.dp), spacing = 0) {
                    if (data.campuses.size > 1) {
                        SettingRow("기본 캠퍼스", value = campus.name, onClick = { campusDialog = true })
                    }
                    val favNames = campus.restaurants.filter { favKey(campus.id, it.id) in s.favorites }.map { it.name }
                    SettingRow(
                        "즐겨찾는 식당",
                        value = when {
                            favNames.isEmpty() -> "없음"
                            favNames.size == 1 -> favNames[0]
                            else -> "${favNames[0]} 외 ${favNames.size - 1}곳"
                        },
                        onClick = { favDialog = true },
                    )
                }
            }

            SectionCaption("알림")
            WCard(padding = PaddingValues(vertical = 4.dp), spacing = 0) {
                SettingRow(
                    "점심 메뉴 알림",
                    sub = "평일 오전 11:00 · 곧 지원돼요",
                    trailing = {
                        Switch(
                            checked = s.notify,
                            onCheckedChange = vm::setNotify,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accent,
                                checkedBorderColor = Color.Transparent,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = W.Border,
                                uncheckedBorderColor = Color.Transparent,
                            ),
                        )
                    },
                )
            }

            SectionCaption("정보")
            WCard(padding = PaddingValues(vertical = 4.dp), spacing = 0) {
                if (data != null && data.source.isNotEmpty()) {
                    SettingRow("메뉴가 이상해요", sub = "학교 식단표 원본 열기", onClick = { uri.openUri(data.source) })
                }
                SettingRow("버전", value = "${BuildConfig.VERSION_NAME} 프로토타입")
            }

            val updated = updatedLabel(data?.generatedAt)
            Text(
                "학교 홈페이지 식단을 모아 보여주는 비공식 앱이에요." + if (updated != null) " ${updated}에 받은 식단이에요." else "",
                style = T.Small,
                color = W.Muted,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp),
            )
        }
    }

    if (campusDialog && data != null && campus != null) {
        AlertDialog(
            onDismissRequest = { campusDialog = false },
            containerColor = W.Card,
            confirmButton = { TextButton(onClick = { campusDialog = false }) { Text("닫기", style = T.Label, color = accent) } },
            title = { Text("기본 캠퍼스", style = T.Title, color = W.Ink) },
            text = {
                Column {
                    data.campuses.forEach { c ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(selected = c.id == campus.id, role = Role.RadioButton) {
                                    vm.selectCampus(c.id)
                                    campusDialog = false
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = c.id == campus.id, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = accent))
                            Text(c.name + "캠퍼스", style = T.Row, color = W.Ink)
                        }
                    }
                }
            },
        )
    }

    if (favDialog && campus != null) {
        AlertDialog(
            onDismissRequest = { favDialog = false },
            containerColor = W.Card,
            confirmButton = { TextButton(onClick = { favDialog = false }) { Text("완료", style = T.Label, color = accent) } },
            title = { Text("즐겨찾는 식당", style = T.Title, color = W.Ink) },
            text = {
                Column {
                    Text("학식 화면 맨 위에 보여줘요", style = T.Caption, color = W.Muted2, modifier = Modifier.padding(bottom = 8.dp))
                    campus.restaurants.filter { it.hasMenu }.forEach { r ->
                        val on = favKey(campus.id, r.id) in s.favorites
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .toggleable(value = on, role = Role.Checkbox) { vm.toggleFavorite(campus.id, r.id) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Checkbox(
                                checked = on,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = accent, checkmarkColor = onColorFor(accent)),
                            )
                            Text(r.name, style = T.Row, color = W.Ink)
                        }
                    }
                }
            },
        )
    }
}
