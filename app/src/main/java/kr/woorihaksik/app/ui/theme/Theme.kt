package kr.woorihaksik.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kr.woorihaksik.app.R

/** 시안(리볼트 레퍼런스)의 색. 학교색은 LocalSchoolColor 로 따로 들어온다. */
object W {
    val Ink = Color(0xFF111111)
    val Muted = Color(0xFF5F6670)        // 흰 바탕·회색 카드 위 보조 글씨 (대비 4.5:1 이상)
    val Muted2 = Color(0xFF6B7280)       // 하단 탭 비활성
    val Surface = Color(0xFFF5F6F7)      // 테두리 없는 회색 카드
    val Line = Color(0xFFEBEDF0)
    val Border = Color(0xFFD5D8DD)
    val Green = Color(0xFF00C471)        // 상태(운영 중) 점·스위치
    val GreenText = Color(0xFF007A52)    // 흰 바탕 위 초록 글씨
    val OnHeroMuted = Color(0xFFC3CEDD)  // 학교색 카드 위 보조 글씨
    val White = Color.White
}

val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
)

/** 화면에서 쓰는 글자 스타일. */
object T {
    private fun s(size: Int, weight: FontWeight, tracking: Double = 0.0, line: Int? = null) = TextStyle(
        fontFamily = Pretendard,
        fontWeight = weight,
        fontSize = size.sp,
        letterSpacing = tracking.em,
        lineHeight = (line ?: (size * 1.45).toInt()).sp,
    )

    val Display = s(28, FontWeight.ExtraBold, -0.04, 36)
    val Wordmark = s(21, FontWeight.ExtraBold, -0.04)
    val HeroTitle = s(22, FontWeight.ExtraBold, -0.03)
    val Section = s(18, FontWeight.ExtraBold, -0.03)
    val Title = s(17, FontWeight.Bold, -0.02)
    val Body = s(15, FontWeight.Medium)
    val BodyStrong = s(15, FontWeight.SemiBold)
    val Price = s(15, FontWeight.Bold)
    val Label = s(13, FontWeight.SemiBold)
    val Small = s(12, FontWeight.Medium)
    val SmallStrong = s(12, FontWeight.Bold)
    val Tab = s(15, FontWeight.Medium)
}

val LocalSchoolColor = staticCompositionLocalOf { W.Ink }

/** 학교색이 밝으면 검정 글씨, 어두우면 흰 글씨. */
fun onColorFor(c: Color): Color = if (c.luminance() > 0.45f) W.Ink else Color.White

@Composable
fun WooriTheme(schoolColor: Color, content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = schoolColor,
        onPrimary = onColorFor(schoolColor),
        background = W.White,
        onBackground = W.Ink,
        surface = W.White,
        onSurface = W.Ink,
        surfaceVariant = W.Surface,
        onSurfaceVariant = W.Muted,
        outline = W.Border,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalSchoolColor provides schoolColor, content = content)
    }
}
