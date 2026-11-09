"""동아대학교 어댑터.

식단 페이지: https://www.donga.ac.kr/community/life-info/menu
- 2026년 사이트 개편 후 주소. 예전 /kor/CMS/DietMenuMgr/list.do 는 404.
- 페이지 한 장에 두 캠퍼스 × (이번 주 기준 앞뒤 4주) 날짜별 표가 모두 들어 있다.
- robots.txt: 이 경로는 허용, '?' 붙은 주소는 금지, Crawl-delay 10.
  → 쿼리 없이 이 주소 하나만, 하루 몇 번만 받는다.
"""

from __future__ import annotations

import json
import re
import time

import requests
from bs4 import BeautifulSoup

from scraper.core import ScrapeError

SCHOOL_ID = "donga"
SOURCE_URL = "https://www.donga.ac.kr/community/life-info/menu"
USER_AGENT = "WooriHaksik/0.1 (unofficial student menu app; contact via GitHub)"

# 학교 페이지의 캠퍼스 이름 → (앱에서 쓰는 id, 화면 이름)
CAMPUS_IDS = {
    "승학캠퍼스": ("seunghak", "승학"),
    "구덕/부민 캠퍼스": ("gudeok-bumin", "구덕·부민"),
}

# 캠퍼스별 식당 이름(표의 열 제목) → 앱 id
RESTAURANT_IDS = {
    "seunghak": {"교수회관": "faculty", "학생회관": "student", "도서관": "library"},
    "gudeok-bumin": {
        "구덕 제2캠퍼스 학생회관": "gudeok2",
        "학생회관": "student",
        "국제회관 기숙사": "dorm",
        "부민(교직원)": "bumin",
    },
}

# 운영시간 표의 이름 → (캠퍼스 id, 식당 id). 구덕 '학생회관'은 승학 학생회관과 이름이 같아 연결하지 않는다.
HOURS_NAMES = {
    "교수회관 식당": ("seunghak", "faculty"),
    "학생회관 식당": ("seunghak", "student"),
    "도서관 식당": ("seunghak", "library"),
    "국제회관 기숙사 식당": ("gudeok-bumin", "dorm"),
    "부민(교직원) 식당": ("gudeok-bumin", "bumin"),
}

BR_RE = re.compile(r"<br\s*/?>", re.I)
TIME_RANGE_RE = re.compile(r"(\d{1,2}:\d{2})\s*~\s*(\d{1,2}:\d{2})")


def fetch(retries: int = 3) -> str:
    last: Exception | None = None
    for attempt in range(retries):
        try:
            r = requests.get(
                SOURCE_URL,
                headers={"User-Agent": USER_AGENT, "Accept-Language": "ko-KR,ko;q=0.9"},
                timeout=30,
            )
            r.raise_for_status()
            r.encoding = "utf-8"
            return r.text
        except requests.RequestException as e:
            last = e
            time.sleep(10 * (attempt + 1))  # robots.txt Crawl-delay 10
    raise ScrapeError(f"식단 페이지를 받지 못했습니다: {last}")


def _cell_lines(td) -> list[str] | None:
    if td is None or td.select_one(".cal-none"):
        return None
    parts = BR_RE.split(td.decode_contents())
    return [BeautifulSoup(p, "html.parser").get_text().strip() for p in parts]


def _parse_hours(soup: BeautifulSoup) -> dict[str, dict[str, dict]]:
    hours: dict[str, dict[str, dict]] = {}
    for cafe in soup.select(".dau-cafe"):
        name_el = cafe.select_one(".dau-cafe-name")
        if not name_el:
            continue
        key = HOURS_NAMES.get(name_el.get_text(strip=True))
        if not key or key[1] in hours.get(key[0], {}):
            continue
        open_close, breaks = None, []
        for row in cafe.select(".dau-cafe-hours > div"):
            dt, dd = row.select_one("dt"), row.select_one("dd")
            if not dt or not dd:
                continue
            ranges = TIME_RANGE_RE.findall(dd.get_text(" "))
            if "영업" in dt.get_text() and ranges:
                open_close = ranges[0]
            elif "휴게" in dt.get_text():
                breaks = [{"from": a, "to": b} for a, b in ranges]
        if open_close:
            hours.setdefault(key[0], {})[key[1]] = {
                "open": open_close[0],
                "close": open_close[1],
                "breaks": breaks,
            }
    return hours


def parse_html(html: str) -> tuple[list[dict], dict]:
    """페이지 HTML → (패널 목록, 운영시간)."""
    soup = BeautifulSoup(html, "html.parser")
    widgets = soup.select('[data-cms-widget-key="content.calendar"]')
    if not widgets:
        raise ScrapeError("식단 위젯을 찾지 못했습니다 (페이지 구조 변경?)")

    panels: list[dict] = []
    for w in widgets:
        try:
            campus = json.loads(w.get("data-cms-widget-props", "{}")).get("campus")
        except json.JSONDecodeError:
            campus = None
        if not campus:
            raise ScrapeError("캠퍼스 이름을 읽지 못했습니다 (data-cms-widget-props)")
        for p in w.select(".cal-day-panel[data-date]"):
            date = p["data-date"]
            if p.select_one(".cal-noday"):
                panels.append({"date": date, "campus": campus, "empty": True})
                continue
            headers = [th.get_text(strip=True) for th in p.select("thead th")][1:]
            rows = []
            for tr in p.select("tbody tr"):
                th = tr.select_one("th")
                tds = tr.select("td")
                rows.append([th.get_text(strip=True) if th else "", [_cell_lines(td) for td in tds]])
            panels.append({"date": date, "campus": campus, "headers": headers, "rows": rows, "empty": False})

    if not panels:
        raise ScrapeError("날짜별 식단 표를 찾지 못했습니다 (페이지 구조 변경?)")
    return panels, _parse_hours(soup)
