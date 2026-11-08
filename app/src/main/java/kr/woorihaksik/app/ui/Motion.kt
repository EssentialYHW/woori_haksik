package kr.woorihaksik.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

/**
 * 앱 전체에서 쓰는 화면 전환.
 * 원칙: 짧게(200ms 안팎), 조금만 움직이고(화면 폭의 1/10 정도), 대부분은 흐려졌다 나타나기.
 * 휴대폰 설정의 '애니메이션 제거'를 켜면 안드로이드가 알아서 꺼 준다.
 */
object Motion {
    const val ENTER = 220
    const val EXIT = 140

    /** 하단 탭 이동: 누른 탭 방향으로 살짝 밀리며 바뀐다. dir = +1(오른쪽 탭) / -1(왼쪽 탭) */
    fun tab(dir: Int): ContentTransform =
        (fadeIn(tween(ENTER, delayMillis = 40, easing = LinearOutSlowInEasing)) +
            slideInHorizontally(tween(ENTER, easing = FastOutSlowInEasing)) { w -> dir * w / 12 }) togetherWith
            fadeOut(tween(EXIT))

    /** 날짜·주 이동: 다음 날은 오른쪽에서, 이전 날은 왼쪽에서 들어온다. */
    fun pager(dir: Int): ContentTransform =
        (fadeIn(tween(ENTER, delayMillis = 30)) +
            slideInHorizontally(tween(ENTER, easing = FastOutSlowInEasing)) { w -> dir * w / 8 }) togetherWith
            (fadeOut(tween(EXIT)) + slideOutHorizontally(tween(EXIT)) { w -> -dir * w / 16 })

    /** 방향이 없는 바뀜(캠퍼스, 식당 바꾸기): 흐려졌다 나타나기만. */
    fun fade(): ContentTransform = fadeIn(tween(ENTER, delayMillis = 40)) togetherWith fadeOut(tween(EXIT))

    /** dir 이 0 이면 fade, 아니면 pager. */
    fun pagerOrFade(dir: Int): ContentTransform = if (dir == 0) fade() else pager(dir)

    /** 학교 선택 ↔ 앱 본 화면: 아래에서 살짝 올라오며 나타난다. */
    fun sheet(forward: Boolean): ContentTransform =
        if (forward) {
            (fadeIn(tween(260, delayMillis = 60)) + slideInVertically(tween(260, easing = FastOutSlowInEasing)) { h -> h / 20 }) togetherWith
                fadeOut(tween(EXIT))
        } else {
            fadeIn(tween(ENTER, delayMillis = 60)) togetherWith
                (fadeOut(tween(EXIT)) + slideOutVertically(tween(EXIT)) { h -> h / 20 })
        }
}

/** AnimatedContent 의 transitionSpec 안에서 쓰기 편하게. */
fun <T : Comparable<T>> AnimatedContentTransitionScope<T>.direction(): Int =
    if (targetState > initialState) 1 else -1
