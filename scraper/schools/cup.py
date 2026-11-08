"""부산가톨릭대학교 어댑터.

식단 페이지: https://www.cup.ac.kr/entrance/home/front/foodMenu.do
- 화면이 날짜마다 아래 주소로 POST 해서 JSON을 받아 그린다 (숨은 API).
    POST /entrance/home/front/meal/changeAjax.do   nowTime=YYYY-MM-DD
    → [] (식단 없음) 또는 [{"f_memo4": "...", ..., "f_memo12": "..."}]
- f_memo 번호 = 식당 × 구분
    4·5·6  학생식당      정식·일품·분식
    7·8·9  교직원 식당    정식·일품·분식   (베리타스센터 2층, 2026년 11월 현재 여기만 식단이 올라옴)
    10·11·12 베리타스센터 식당 정식·일품·분식
- 날짜를 넣으면 지난 식단도 다 나온다 (2026년 3월분까지 확인). 앞으로는 이번 주까지만 올라온다.
- robots.txt 가 없다. 하루 몇 번, 요청 사이 1.5초씩 쉬면서 받는다.
"""

from __future__ import annotations

import json
import re
import time
from datetime import date, datetime, timedelta

import requests

from scraper.core import KST, ScrapeError

SCHOOL_ID = "cup"
SOURCE_URL = "https://www.cup.ac.kr/entrance/home/front/foodMenu.do"
API_URL = "https://www.cup.ac.kr/entrance/home/front/meal/changeAjax.do"
USER_AGENT = "WooriHaksik/0.3 (unofficial student menu app; contact via GitHub)"

CAMPUS_NAME = "부산가톨릭대학교"
CAMPUS_IDS = {CAMPUS_NAME: ("main", "본교")}

# (열 제목 = 식당 이름, f_memo 번호 3개 = 정식·일품·분식)
RESTAURANTS = [
    ("학생식당", (4, 5, 6)),
    ("교직원 식당", (7, 8, 9)),
    ("베리타스센터 식당", (10, 11, 12)),
]
MEAL_TYPES = ["정식", "일품", "분식"]
RESTAURANT_IDS = {"main": {"학생식당": "student", "교직원 식당": "staff", "베리타스센터 식당": "veritas"}}

# 학교 '식당&매점' 안내 페이지 기준 (학생식당은 운영시간이 '-')
HOURS = {"main": {"staff": {"open": "11:50", "close": "13:30", "breaks": []}}}

PRICE_RE = re.compile(r"\(\d{3,6}\)\s*$")
DAYS_BACK = 28   # 지난 4주
DAYS_AHEAD = 13  # 다음 주 금요일까지 (올라와 있으면)
DELAY = 1.5


def _lines(text: str | None) -> list[str] | None:
    if not text or not text.strip():
        return None
    lines = [ln.strip() for ln in re.split(r"\r\n|\r|\n", text)]
    lines = [ln for ln in lines if ln]
    return lines or None


def _cell(text: str | None, meal_type: str) -> list[str] | None:
    """칸 하나 → 줄 목록. 가격 붙은 메뉴가 하나도 없으면 '정식 한 상'으로 보고 이름을 붙인다."""
    lines = _lines(text)
    if lines is None:
        return None
    if not any(PRICE_RE.search(ln) for ln in lines):
        return [meal_type] + lines  # → 이름 '정식', 구성 = 나머지 줄
    return lines


def panel_from_json(day: str, obj: dict | None) -> dict:
    """API 응답 하나(그날) → 공통 '중간 형태' 패널."""
    if not obj:
        return {"date": day, "campus": CAMPUS_NAME, "empty": True}
    rows = []
    for ti, meal_type in enumerate(MEAL_TYPES):
        rows.append([meal_type, [_cell(obj.get(f"f_memo{memos[ti]}"), meal_type) for _, memos in RESTAURANTS]])
    if all(c is None for _, cells in rows for c in cells):
        return {"date": day, "campus": CAMPUS_NAME, "empty": True}
    return {"date": day, "campus": CAMPUS_NAME, "headers": [n for n, _ in RESTAURANTS], "rows": rows, "empty": False}


def _weekdays(start: date, end: date) -> list[date]:
    out, d = [], start
    while d <= end:
        if d.weekday() < 5:
            out.append(d)
        d += timedelta(days=1)
    return out


def _post(day: str, session: requests.Session, retries: int = 3):
    last: Exception | None = None
    for attempt in range(retries):
        try:
            r = session.post(API_URL, data={"nowTime": day}, timeout=20)
            r.raise_for_status()
            data = json.loads(r.content.decode("utf-8"))
            return data[0] if data else None
        except (requests.RequestException, ValueError) as e:
            last = e
            time.sleep(5 * (attempt + 1))
    raise ScrapeError(f"{day} 식단을 받지 못했습니다: {last}")


def collect(fixture=None) -> tuple[list[dict], dict]:
    """(패널 목록, 운영시간). fixture: {날짜: 응답 객체|null} JSON 파일 경로 (테스트용)."""
    if fixture is not None:
        raw = json.loads(fixture.read_text(encoding="utf-8"))
        panels = [panel_from_json(d, o) for d, o in sorted(raw.items()) if not d.startswith("_")]
        return panels, HOURS

    today = datetime.now(KST).date()
    session = requests.Session()
    session.headers.update({"User-Agent": USER_AGENT, "Referer": SOURCE_URL, "Accept-Language": "ko-KR,ko;q=0.9"})
    panels = []
    for d in _weekdays(today - timedelta(days=DAYS_BACK), today + timedelta(days=DAYS_AHEAD)):
        panels.append(panel_from_json(d.isoformat(), _post(d.isoformat(), session)))
        time.sleep(DELAY)
    if not panels:
        raise ScrapeError("받은 날짜가 없습니다")
    return panels, HOURS
