package kr.woorihaksik.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kr.woorihaksik.app.R

/**
 * v0.2 색 (레퍼런스: 토스 화면 구성 — 회색 바탕 + 흰 카드).
 * 학교색은 LocalSchoolColor 로 따로 들어오고, 강조가 꼭 필요한 곳에만 쓴다.
 */
object W {
    val Bg = Color(0xFFF2F4F6)          // 화면 바탕
    val Card = Color(0xFFFFFFFF)        // 카드
    val Ink = Color(0xFF191F28)         // 본문 글씨
    val Muted = Color(0xFF4E5968)       // 보조 글씨 (회색 바탕 위에서도 대비 충분)
    val Muted2 = Color(0xFF6B7684)      // 흰 카드 위 보조 글씨, 비활성 탭
    val Faint = Color(0xFFB0B8C1)       // 화살표, 꺼진 하트처럼 장식용
    val Surface = Color(0xFFF2F4F6)     // 카드 안의 회색 상자
    val Line = Color(0xFFE5E8EB)        // 구분선
    val Border = Color(0xFFD1D6DB)
    val Green = Color(0xFF00C471)       // 운영 중 점
    val GreenText = Color(0xFF007A52)   // 운영 중 글씨
    val White = Color.White
}

val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
)

/** 글자 스타일은 이것만 쓴다. (v0.1의 8가지 → 정리) */
object T {
    private fun s(size: Int, weight: FontWeight, tracking: Double = -0.01, line: Int? = null) = TextStyle(
        fontFamily = Pretendard,
        fontWeight = weight,
        fontSize = size.sp,
        letterSpacing = tracking.em,
        lineHeight = (line ?: (size * 1.45).toInt()).sp,
    )

    val Display = s(28, FontWeight.ExtraBold, -0.04, 36)   // 학교 선택 화면 질문
    val Wordmark = s(21, FontWeight.ExtraBold, -0.04)
    val ScreenTitle = s(26, FontWeight.ExtraBold, -0.03, 34)
    val CardTitle = s(19, FontWeight.Bold, -0.02, 26)
    val Title = s(17, FontWeight.Bold, -0.02)
    val Body = s(15, FontWeight.Medium)
    val BodyStrong = s(15, FontWeight.SemiBold)
    val Row = s(16, FontWeight.Medium)
    val Caption = s(13, FontWeight.Medium, 0.0, 19)
    val CaptionStrong = s(13, FontWeight.SemiBold, 0.0, 19)
    val Label = s(14, FontWeight.SemiBold)
    val Small = s(12, FontWeight.Medium, 0.0)
    val SmallStrong = s(12, FontWeight.Bold, 0.0)

    // v0.1 이름 호환 (학교 선택 화면에서 사용)
    val Section = s(18, FontWeight.ExtraBold, -0.03)
}

val LocalSchoolColor = staticCompositionLocalOf { W.Ink }

/** 학교색이 밝으면 검정 글씨, 어두우면 흰 글씨. */
fun onColorFor(c: Color): Color = if (c.luminance() > 0.45f) W.Ink else Color.White

/** 학교색을 흰 바탕에 아주 옅게 깐 색 (칩 배경). */
fun tintOf(c: Color, alpha: Float = 0.10f): Color = c.copy(alpha = alpha).compositeOver(Color.White)

@Composable
fun WooriTheme(schoolColor: Color, content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = schoolColor,
        onPrimary = onColorFor(schoolColor),
        background = W.Bg,
        onBackground = W.Ink,
        surface = W.Card,
        onSurface = W.Ink,
        surfaceVariant = W.Surface,
        onSurfaceVariant = W.Muted,
        outline = W.Border,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalSchoolColor provides schoolColor, content = content)
    }
}
