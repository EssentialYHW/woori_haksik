package kr.woorihaksik.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.woorihaksik.app.ui.theme.W
import kr.woorihaksik.app.ui.theme.WooriTheme

private enum class Mode { BOOT, PICK, MAIN }

@Composable
fun WooriApp(vm: AppViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    // 학교를 고르기 전에는 검정, 고른 뒤에는 그 학교의 상징색. 색이 바뀔 때도 부드럽게.
    val target = if (s.pickingSchool || s.school == null) W.Ink else Color(s.school!!.color)
    val schoolColor by animateColorAsState(target, tween(300), label = "schoolColor")

    BackHandler(enabled = s.pickingSchool && s.school != null) { vm.closeSchoolPicker() }
    BackHandler(enabled = !s.pickingSchool && s.tab != Tab.TODAY) { vm.selectTab(Tab.TODAY) }

    val mode = when {
        s.booting -> Mode.BOOT
        s.pickingSchool || s.school == null -> Mode.PICK
        else -> Mode.MAIN
    }

    WooriTheme(schoolColor) {
        Surface(Modifier.fillMaxSize(), color = W.White, contentColor = W.Ink) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = { Motion.sheet(forward = targetState == Mode.MAIN) },
                label = "mode",
            ) { m ->
                when (m) {
                    // 저장된 학교를 확인하는 아주 짧은 순간: 학교 선택 화면이 깜빡이지 않게 비워 둔다.
                    Mode.BOOT -> Box(Modifier.fillMaxSize())
                    Mode.PICK -> SchoolSelectScreen(s, vm)
                    Mode.MAIN -> Column(Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = s.tab,
                            transitionSpec = { Motion.tab(direction()) },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            label = "tab",
                        ) { tab ->
                            Box(Modifier.fillMaxSize()) {
                                when (tab) {
                                    Tab.TODAY -> TodayScreen(s, vm)
                                    Tab.WEEKLY -> WeeklyScreen(s, vm)
                                    Tab.MY -> MyScreen(s, vm)
                                }
                            }
                        }
                        BottomBar(s.tab, vm::selectTab)
                    }
                }
            }
        }
    }
}
