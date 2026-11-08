"""고려대학교(안암) 어댑터.

식당마다 페이지가 따로 있고, 한 페이지에 그 주(월~일) 식단 표가 서버에서 그려져 나온다.
    GET  https://www.korea.ac.kr/ko/{page}/subview.do              → 이번 주
    POST https://www.korea.ac.kr/diet/ko/{no}/view.do               → 지난주/다음 주
         layout=<페이지 form 값>  monday=YYYY.MM.DD  week=pre|next
- 표 칸: 요일 | 식단구분(조식·중식·천원의아침…) | 식단제목(중식A…) | 식단내용 | 기타정보
- 식당마다 '식단내용' 적는 방식이 다르다.
    학생회관   : 한 줄에 반찬 하나, 원산지 줄 '(돈육:국내산)' 이 섞여 있음, 가격 없음
    애기능     : '[학생식당] 6,500₩' 머리줄 + 반찬을 띄어쓰기로 한 줄에, '(사이드메뉴: …)'
    산학관     : 식단제목 '중식A/중식B', 반찬을 띄어쓰기로 한 줄에, 가격 없음
    안암학사   : 한 줄에 반찬 하나, 마지막 줄 '₩6,000'
  → 아래 normalize() 가 이것들을 공통 '칸 줄 목록'으로 맞춘다.
- robots.txt: 이 경로들은 막혀 있지 않다. 식당 4곳 × (이번 주 + 다음 주), 요청 사이 2초.
- 수당삼양패컬티하우스 송림(page 503)은 고정 메뉴의 고급 식당이라 넣지 않았다.
"""

from __future__ import annotations

import re
import time

import requests
from bs4 import BeautifulSoup

from scraper.core import ScrapeError

SCHOOL_ID = "ku"
BASE = "https://www.korea.ac.kr"
SOURCE_URL = f"{BASE}/ko/508/subview.do"
USER_AGENT = "WooriHaksik/0.4 (unofficial student menu app; contact via GitHub)"

CAMPUS_NAME = "안암"
CAMPUS_IDS = {CAMPUS_NAME: ("anam", "안암")}

# (페이지 번호, 식단 번호, 식당 id, 화면 이름)
RESTAURANTS = [
    (508, 13, "student", "학생회관"),
    (504, 9, "aegineung", "애기능생활관"),
    (506, 11, "sanhak", "산학관"),
    (505, 10, "dorm", "안암학사"),
]
RESTAURANT_IDS = {"anam": {name: rid for _, _, rid, name in RESTAURANTS}}

# 각 식당 페이지 '이용시간' 기준 (조식~석식 전체 범위 + 식사 사이 쉬는 시간)
HOURS = {
    "anam": {
        "student": {"open": "08:00", "close": "14:00", "breaks": [{"from": "09:30", "to": "11:00"}]},
        "aegineung": {"open": "08:00", "close": "19:00", "breaks": [{"from": "09:30", "to": "11:00"}, {"from": "14:00", "to": "17:30"}]},
        "sanhak": {"open": "11:20", "close": "18:50", "breaks": [{"from": "13:50", "to": "17:30"}]},
        "dorm": {"open": "08:00", "close": "19:00", "breaks": [{"from": "09:30", "to": "11:30"}, {"from": "14:00", "to": "17:00"}]},
    }
}

MEAL_ORDER = ["조식", "중식", "석식"]
DATE_RE = re.compile(r"(\d{4})\.(\d{1,2})\.(\d{1,2})")
TYPE_SUB_RE = re.compile(r"^(.+?)\((.+)\)$")
VENUE_RE = re.compile(r"^\[(.+?)\]\s*([\d,]+)\s*₩?$")
PRICE_RE = re.compile(r"^(?:₩\s*([\d,]+)|([\d,]+)\s*₩)$")
SIDE_RE = re.compile(r"^\(\s*사이드\s*메뉴\s*:\s*(.+?)\s*\)$")
PAREN_RE = re.compile(r"^\(.*\)$")
OPTION_RE = re.compile(r"^[A-Z](?:\s|(?=[가-힣]))")
BR_RE = re.compile(r"<br\s*/?>", re.I)
EMPTY_TEXT = "등록된 식단"


# ---------------------------------------------------------------- 표 읽기


def _lines(td) -> list[str]:
    parts = BR_RE.split(td.decode_contents())
    out = []
    for p in parts:
        t = BeautifulSoup(p, "html.parser").get_text().strip().strip('"').strip()
        if t:
            out.append(t)
    return out


def parse_table(table) -> list[tuple[str, str, str, list[str]]]:
    """표 → [(날짜 ISO, 식단구분, 식단제목, 내용 줄 목록)]. 빈 칸('등록된 식단…')은 건너뛴다."""
    out = []
    date = None
    cur_type = None
    for tr in table.select("tbody tr"):
        th = tr.find("th")
        if th is not None:
            m = DATE_RE.search(th.get_text())
            if m:
                date = f"{int(m[1]):04d}-{int(m[2]):02d}-{int(m[3]):02d}"
                out.append((date, "", "", []))  # 식단이 비어 있는 날(주말)도 날짜 범위에 넣기 위한 표시
        tds = tr.find_all("td", recursive=False)
        if not tds or date is None:
            continue
        if any(EMPTY_TEXT in td.get_text() for td in tds):
            cur_type = tds[0].get_text(strip=True)
            continue
        if len(tds) >= 4:  # 구분 | 제목 | 내용 | 기타
            cur_type = tds[0].get_text(strip=True)
            title, content = tds[1].get_text(strip=True), tds[2]
        elif len(tds) == 3 and cur_type:  # 위 칸의 구분이 이어지는 줄
            title, content = tds[0].get_text(strip=True), tds[1]
        else:
            continue
        lines = _lines(content)
        if lines:
            out.append((date, cur_type, title, lines))
    return out


# ---------------------------------------------------------------- 내용 맞추기


def _split_dishes(line: str) -> list[str]:
    """'마제덮밥 사모사 뭇국 단무지' 처럼 띄어쓰기로 적은 줄은 반찬별로 나눈다. 'A 치킨…' 선택지 줄은 그대로."""
    if OPTION_RE.match(line) or line.lower().startswith("or "):
        return [line]  # 'A 치킨…', 'or 김치치즈 주먹밥 세트' 같은 선택지 줄
    toks = line.split()
    return toks if len(toks) >= 4 else [line]


def normalize(meal_type_raw: str, title: str, lines: list[str]) -> tuple[str, list[str]]:
    """한 줄(식단구분 하나) → (식사 구분, core.parse_cell 이 읽는 칸 줄 목록)."""
    m = TYPE_SUB_RE.match(meal_type_raw)
    base, sub = (m[1].strip(), m[2].strip()) if m else (meal_type_raw.strip(), None)
    breakfast_row = "천원" in meal_type_raw
    meal = "조식" if breakfast_row else base

    sets: list[dict] = []

    def cur() -> dict:
        if not sets:
            sets.append({"name": None, "price": None, "dishes": [], "notes": []})
        return sets[-1]

    for ln in lines:
        if (sm := SIDE_RE.match(ln)):
            cur()["notes"].append(f"사이드메뉴: {sm[1]}")
        elif PAREN_RE.match(ln):
            continue  # 원산지 표시
        elif (vm := VENUE_RE.match(ln)):
            sets.append({"name": vm[1].strip(), "price": int(vm[2].replace(",", "")), "dishes": [], "notes": []})
        elif (pm := PRICE_RE.match(ln)):
            cur()["price"] = int((pm[1] or pm[2]).replace(",", ""))
        else:
            cur()["dishes"].extend(_split_dishes(ln))

    out: list[str] = []
    for s in sets:
        is_breakfast = breakfast_row or (meal == "조식" and s["price"] == 1000)
        if is_breakfast:
            name = "천원의아침밥"
            if sub:
                s["notes"].insert(0, sub)  # 예: 테이크아웃
        elif s["name"]:
            name = s["name"]  # [학생식당] / [교직원식당]
        elif sub:
            name = sub  # 한식반상 / 일품반상
        elif title and title != base:
            name = title  # 중식A / 중식B
        else:
            name = base
        price = s["price"] if s["price"] is not None else (1000 if is_breakfast else None)
        if out:
            out.append("")
        out.append(f"{name}({price})" if price else name)
        out.extend(s["dishes"])
        out.extend(f"[{n}]" for n in s["notes"])
    return meal, out


def panels_from_rows(per_restaurant: dict[str, list[tuple[str, str, str, list[str]]]]) -> list[dict]:
    """식당별 표 행 → 날짜별 공통 패널 (열 = 식당, 행 = 조식·중식·석식)."""
    names = [name for _, _, _, name in RESTAURANTS]
    cells: dict[str, dict[str, dict[str, list[str]]]] = {}
    dates: set[str] = set()
    for name, rows in per_restaurant.items():
        for date, mtype, title, lines in rows:
            dates.add(date)
            if not lines:
                continue
            meal, cell = normalize(mtype, title, lines)
            slot = cells.setdefault(date, {}).setdefault(meal, {})
            if name in slot:
                slot[name] = slot[name] + [""] + cell
            else:
                slot[name] = cell
    panels = []
    for date in sorted(dates):
        by_meal = cells.get(date, {})
        meals = [m for m in MEAL_ORDER if m in by_meal] + [m for m in by_meal if m not in MEAL_ORDER]
        if not meals:
            panels.append({"date": date, "campus": CAMPUS_NAME, "empty": True})
            continue
        rows = [[m, [by_meal[m].get(n) for n in names]] for m in meals]
        panels.append({"date": date, "campus": CAMPUS_NAME, "headers": names, "rows": rows, "empty": False})
    return panels


# ---------------------------------------------------------------- 받기


def _table_and_form(html: str):
    soup = BeautifulSoup(html, "html.parser")
    table = soup.select_one("article table") or soup.select_one("table")
    form = soup.select_one("form#viewForm")
    params = {}
    if form is not None:
        for inp in form.select("input[name]"):
            params[inp["name"]] = inp.get("value", "")
    return table, params


def _get(session: requests.Session, method: str, url: str, **kw) -> str:
    last: Exception | None = None
    for attempt in range(3):
        try:
            r = session.request(method, url, timeout=25, **kw)
            r.raise_for_status()
            r.encoding = "utf-8"
            return r.text
        except requests.RequestException as e:
            last = e
            time.sleep(5 * (attempt + 1))
    raise ScrapeError(f"{url} 를 받지 못했습니다: {last}")


def collect(fixture=None) -> tuple[list[dict], dict]:
    """(패널 목록, 운영시간). fixture: 식당 표를 <section data-page=…> 로 모은 HTML (테스트용)."""
    per: dict[str, list] = {}
    if fixture is not None:
        soup = BeautifulSoup(fixture.read_text(encoding="utf-8"), "html.parser")
        for page, _, _, name in RESTAURANTS:
            sec = soup.select_one(f'section[data-page="{page}"] table')
            per[name] = parse_table(sec) if sec is not None else []
        return panels_from_rows(per), HOURS

    session = requests.Session()
    session.headers.update({"User-Agent": USER_AGENT, "Accept-Language": "ko-KR,ko;q=0.9"})
    for page, no, _, name in RESTAURANTS:
        html = _get(session, "GET", f"{BASE}/ko/{page}/subview.do")
        table, form = _table_and_form(html)
        if table is None:
            raise ScrapeError(f"{name}: 식단 표를 찾지 못했습니다 (페이지 구조 변경?)")
        rows = parse_table(table)
        time.sleep(2)
        if form.get("monday") and form.get("layout"):  # 다음 주도 올라와 있으면 같이
            nxt = _get(session, "POST", f"{BASE}/diet/ko/{no}/view.do",
                       data={"layout": form["layout"], "monday": form["monday"], "week": "next"})
            t2, _ = _table_and_form(nxt)
            if t2 is not None:
                rows += parse_table(t2)
            time.sleep(2)
        per[name] = rows
    panels = panels_from_rows(per)
    if not panels:
        raise ScrapeError("고려대 식단 날짜를 하나도 읽지 못했습니다")
    return panels, HOURS

