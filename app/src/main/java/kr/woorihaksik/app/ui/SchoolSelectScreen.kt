package kr.woorihaksik.app.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kr.woorihaksik.app.R
import kr.woorihaksik.app.data.School
import kr.woorihaksik.app.ui.theme.T
import kr.woorihaksik.app.ui.theme.W
import kr.woorihaksik.app.ui.theme.onColorFor

/** 첫 실행(또는 학교 변경) 화면. 학교를 고르기 전에는 검정 중립 톤. */
@Composable
fun SchoolSelectScreen(s: UiState, vm: AppViewModel) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var pickedId by rememberSaveable { mutableStateOf<String?>(null) }
    // 직접 고른 학교 → 지금 학교 → 지원 학교가 하나뿐이면 그 학교 순으로 미리 선택.
    val selectedId = pickedId ?: s.school?.id ?: s.schools.singleOrNull()?.id
    val filtered = s.schools.filter { school ->
        query.isBlank() || listOf(school.name, school.shortName, school.region).any { it.contains(query.trim(), ignoreCase = true) }
    }
    val selected: School? = s.schools.firstOrNull { it.id == selectedId }
    val ctaColor = selected?.let { Color(it.color) } ?: W.Ink

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("우리학식", style = T.Wordmark, modifier = Modifier.weight(1f))
            if (s.school != null) {
                TextButton(onClick = vm::closeSchoolPicker) { Text("닫기", style = T.Label, color = W.Ink) }
            }
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("어느 학교에\n다니세요?", style = T.Display)
            Text("학교를 고르면 오늘 학식부터 바로 보여드려요.", style = T.Body, color = W.Muted)
        }

        // 검색
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("학교 검색", style = T.Label, color = W.Muted)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(W.Surface)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(painterResource(R.drawable.ic_search), null, Modifier.size(20.dp), tint = W.Muted2)
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = T.Body.copy(color = W.Ink),
                    cursorBrush = SolidColor(W.Ink),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) Text("학교 이름을 입력하세요", style = T.Body, color = W.Muted2)
                            inner()
                        }
                    },
                )
            }
        }

        Text(
            "지원 중인 학교 ${s.schools.size}",
            style = T.Section,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp),
        )

        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (s.booting) {
                item { Loading() }
            }
            items(filtered, key = { it.id }) { school ->
                SchoolRow(school, school.id == selectedId) { pickedId = school.id }
            }
            if (!s.booting && filtered.isEmpty()) {
                item { CenterMessage("찾는 학교가 아직 없어요", "아래 버튼으로 우리 학교를 요청해 주세요.") }
            }
            item {
                OutlinedButton(
                    onClick = { Toast.makeText(context, "학교 추가 요청은 곧 열려요. 조금만 기다려 주세요!", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, W.Border),
                ) {
                    Text("우리 학교 추가 요청하기", style = T.Label, color = W.Ink)
                }
            }
        }

        Button(
            onClick = { selected?.let { vm.pickSchool(it.id) } },
            enabled = selected != null,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ctaColor,
                contentColor = onColorFor(ctaColor),
                disabledContainerColor = W.Surface,
                disabledContentColor = W.Muted2,
            ),
        ) {
            Text(if (selected != null) "${withEuro(selected.name)} 시작하기" else "학교를 골라 주세요", style = T.BodyStrong)
        }
    }
}

/** 받침에 따라 '로/으로' 붙이기 (ㄹ 받침은 '로'). 예: 동아대학교로, 부산대학교로, 한양대 서울캠퍼스로 */
fun withEuro(word: String): String {
    val last = word.lastOrNull() ?: return word
    if (last !in '가'..'힣') return word + "로"
    val jong = (last - '가') % 28
    return word + if (jong == 0 || jong == 8) "로" else "으로"
}

@Composable
private fun SchoolRow(school: School, selected: Boolean, onClick: () -> Unit) {
    val color = Color(school.color)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) color else W.Border, RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(color))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(school.name, style = T.Title)
            Text(listOf(school.region, school.campusSummary).filter { it.isNotEmpty() }.joinToString(" · "), style = T.Label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = W.Muted)
        }
        if (selected) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_check), null, Modifier.size(16.dp), tint = onColorFor(color))
            }
        }
    }
}
