package kr.woorihaksik.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kr.woorihaksik.app.BuildConfig
import kr.woorihaksik.app.R
import kr.woorihaksik.app.data.Repository
import kr.woorihaksik.app.ui.theme.LocalSchoolColor
import kr.woorihaksik.app.ui.theme.T
import kr.woorihaksik.app.ui.theme.W
import kr.woorihaksik.app.ui.theme.onColorFor
import kr.woorihaksik.app.util.updatedLabel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyScreen(s: UiState, vm: AppViewModel) {
    val school = s.school ?: return
    val accent = LocalSchoolColor.current
    val uri = LocalUriHandler.current
    var campusDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("내 정보", style = T.Wordmark)
        }

        // 내 학교
        Row(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(W.Surface)
                .clickable(role = Role.Button) { vm.openSchoolPicker() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("내 학교", style = T.SmallStrong, color = W.Muted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(accent))
                    Text(school.name, style = T.Section, color = accent)
                }
                if (school.colorName.isNotEmpty()) Text("학교 색 · ${school.colorName}", style = T.Small, color = W.Muted)
            }
            Text("변경", style = T.Label)
            Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(16.dp), tint = W.Ink)
        }

        SectionTitle("학식")
        val data = s.data
        val campus = s.campus
        if (data != null && campus != null) {
            if (data.campuses.size > 1) {
                SettingRow("기본 캠퍼스", value = campus.name) { campusDialog = true }
            }
            Column(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("즐겨찾는 식당", style = T.Body)
                Text("학식 화면 맨 위에 크게 보여줘요", style = T.Small, color = W.Muted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    campus.restaurants.filter { it.hasMenu }.forEach { r ->
                        val on = favKey(campus.id, r.id) in s.favorites
                        Box(
                            Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (on) accent else W.White)
                                .border(BorderStroke(1.dp, if (on) accent else W.Border), RoundedCornerShape(24.dp))
                                .selectable(selected = on, role = Role.Checkbox) { vm.toggleFavorite(campus.id, r.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                r.name,
                                style = T.Label.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
                                color = if (on) onColorFor(accent) else W.Ink,
                            )
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = W.Line, thickness = 1.dp)
        }

        SectionTitle("알림")
        Row(
            Modifier.padding(horizontal = 20.dp).fillMaxWidth().heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("점심 메뉴 알림", style = T.Body)
                Text("평일 오전 11시 · 즐겨찾는 식당 (다음 버전에서 실제로 울려요)", style = T.Small, color = W.Muted)
            }
            Switch(
                checked = s.notify,
                onCheckedChange = vm::setNotify,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = W.Green,
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFC9CDD3),
                    uncheckedBorderColor = Color.Transparent,
                ),
            )
        }
        HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = W.Line, thickness = 1.dp)

        SectionTitle("정보")
        if (data != null && data.source.isNotEmpty()) {
            SettingRow("메뉴가 이상해요", value = "학교 식단표 열기") { uri.openUri(data.source) }
        }
        val updated = updatedLabel(data?.generatedAt)
        val originText = when (s.origin) {
            Repository.Origin.NETWORK -> "방금 받은 최신 데이터"
            Repository.Origin.CACHE -> "마지막으로 받아 둔 데이터"
            Repository.Origin.SAMPLE -> "앱에 들어 있는 샘플 데이터"
            null -> "데이터 없음"
        }
        Text(
            "우리학식 ${BuildConfig.VERSION_NAME} · 학교 홈페이지 식단을 모아 보여주는 비공식 앱이에요.\n" +
                originText + (if (updated != null) " · $updated 기준" else ""),
            style = T.Small.copy(fontWeight = FontWeight.Normal, lineHeight = T.Small.fontSize * 1.6),
            color = W.Muted,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }

    if (campusDialog && data != null && campus != null) {
        AlertDialog(
            onDismissRequest = { campusDialog = false },
            confirmButton = { TextButton(onClick = { campusDialog = false }) { Text("닫기", style = T.Label, color = accent) } },
            title = { Text("기본 캠퍼스", style = T.Title) },
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
                        ) {
                            RadioButton(selected = c.id == campus.id, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = accent))
                            Spacer(Modifier.size(12.dp))
                            Text(c.name, style = T.Body)
                        }
                    }
                }
            },
            containerColor = W.White,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = T.Section, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 4.dp))
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = T.Body, modifier = Modifier.weight(1f))
            Text(value, style = T.Label.copy(fontWeight = FontWeight.Medium), color = W.Muted)
            Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(16.dp), tint = W.Muted)
        }
        HorizontalDivider(color = W.Line, thickness = 1.dp)
    }
}
