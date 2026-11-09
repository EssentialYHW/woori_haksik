"""학교와 상관없이 쓰는 공통 부분: 메뉴 칸 해석, 결과 저장.

학교 어댑터(schools/*.py)는 페이지를 읽어 아래 '중간 형태'를 만들면 된다.

    Panel = {
        "date": "2026-11-09",
        "campus": "승학캠퍼스",          # 학교 페이지에 적힌 캠퍼스 이름
        "headers": ["교수회관", ...],    # 식당 이름 (열)
        "rows": [["정식", [[줄, 줄, ...] | None, ...]], ...],   # 구분(행) × 식당(열) 칸의 줄 목록
        "empty": False,
    }

그 다음 build_result() 가 앱이 읽는 JSON 구조로 바꿔준다.
"""

from __future__ import annotations

import hashlib
import json
import re
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from pathlib import Path

KST = timezone(timedelta(hours=9))
SCHEMA_VERSION = 1

# "돼지국밥(6500)" / 원본이 잘린 "바지락칼국수(650" 둘 다 메뉴 줄로 본다.
ITEM_RE = re.compile(r"^(?P<name>.+?)\((?P<price>\d{1,6})(?P<close>\))?\s*$")
SPACES_RE = re.compile(r"\s+")


class ScrapeError(RuntimeError):
    """페이지 구조가 바뀌었거나 데이터를 못 읽었을 때."""


# ---------------------------------------------------------------- 메뉴 칸 해석


@dataclass
class MenuItem:
    name: str
    price: int | None = None
    sides: list[str] = field(default_factory=list)
    extras: list[dict] = field(default_factory=list)  # [{"label": "셀프바", "items": [...]}]
    notes: list[str] = field(default_factory=list)
    venue: str | None = None  # 같은 칸 안의 다른 매장 (예: 학생회관 다방)

    def to_json(self) -> dict:
        out: dict = {"name": self.name}
        if self.price is not None:
            out["price"] = self.price
        if self.sides:
            out["sides"] = self.sides
        extras = [e for e in self.extras if e["items"]]
        if extras:
            out["extras"] = extras
        if self.notes:
            out["notes"] = self.notes
        if self.venue:
            out["venue"] = self.venue
        return out


def _clean(line: str) -> str:
    return SPACES_RE.sub(" ", line).strip()


def _is_note(line: str) -> bool:
    if line.startswith("[") and line.endswith("]"):
        return True  # [09:00~선착순제공]
    return len(line) > 2 and line.startswith("*") and line.endswith("*")  # *10시40분부터 ... 무료제공*


def _is_item_line(line: str) -> bool:
    return bool(ITEM_RE.match(line)) and not line.startswith("*")


def parse_cell(lines: list[str]) -> list[MenuItem]:
    """한 칸(식당 × 구분)의 줄 목록을 메뉴 목록으로 바꾼다.

    규칙
    - '이름(가격)' 줄은 새 메뉴. 닫는 괄호가 없으면(원본 잘림) 가격은 비워 둔다.
    - 가격 없는 줄은 직전 메뉴의 구성(반찬). 빈 줄 바로 뒤라면 새 메뉴의 시작.
    - '[...]', '*...*' 줄은 안내 문구(notes).
    - '*이름' 줄 뒤에 가격 있는 메뉴가 오면 그 아래는 다른 매장(venue),
      가격 없는 줄만 오면 직전 메뉴의 추가 구성(예: 셀프바).
    - '<통일밥상>' 같은 꺾쇠 줄은 행사 메뉴 이름.
    """
    items: list[MenuItem] = []
    cur: MenuItem | None = None
    extra: dict | None = None
    venue: str | None = None
    pending_notes: list[str] = []
    block_start = True

    cleaned = [_clean(x) for x in lines]
    for i, line in enumerate(cleaned):
        if not line:
            block_start = True
            extra = None
            continue

        if _is_note(line):
            note = line.strip("[]*").strip()
            target = cur or (items[-1] if items else None)
            (target.notes if target else pending_notes).append(note)
            block_start = False
            continue

        if line.startswith("*"):
            label = line[1:].strip()
            rest = []
            for nxt in cleaned[i + 1 :]:
                if not nxt:
                    break
                rest.append(nxt)
            if any(_is_item_line(x) for x in rest):
                venue, cur, extra = label, None, None
            else:
                target = cur or (items[-1] if items else None)
                if target:
                    extra = {"label": label, "items": []}
                    target.extras.append(extra)
            block_start = False
            continue

        m = ITEM_RE.match(line)
        if m:
            price = int(m["price"]) if m["close"] and int(m["price"]) >= 100 else None
            cur = MenuItem(name=_clean(m["name"]), price=price, venue=venue)
            items.append(cur)
            extra = None
        elif line.startswith("<") and line.endswith(">"):
            cur = MenuItem(name=line[1:-1].strip(), venue=venue)
            items.append(cur)
            extra = None
        elif extra is not None:
            extra["items"].append(line)
        elif cur is not None and not block_start:
            cur.sides.append(line)
        else:
            cur = MenuItem(name=line, venue=venue)
            items.append(cur)

        if pending_notes and cur is not None:
            cur.notes[:0] = pending_notes
            pending_notes = []
        block_start = False

    for it in items:
        if it.price is None and "천원의" in it.name:
            it.price = 1000  # '천원의아침밥'은 가격 표기가 없지만 이름 그대로 1,000원
        elif it.price is None and any("무료" in n for n in it.notes):
            it.price = 0  # '선착순 무료제공' 행사
    return items


# ---------------------------------------------------------------- 결과 만들기


def slug(name: str) -> str:
    return "r-" + hashlib.sha1(name.encode("utf-8")).hexdigest()[:6]


def build_result(
    *,
    school: str,
    source: str,
    panels: list[dict],
    campus_ids: dict[str, tuple[str, str]],
    restaurant_ids: dict[str, dict[str, str]],
    hours: dict[str, dict[str, dict]],
    now: datetime,
) -> dict:
    """중간 형태(panels) → 앱이 읽는 latest.json 구조."""
    campuses: dict[str, dict] = {}
    entries: list[dict] = []
    dates: list[str] = []

    for p in panels:
        raw_campus = p["campus"]
        cid, cname = campus_ids.get(raw_campus, (slug(raw_campus), raw_campus))
        camp = campuses.setdefault(cid, {"id": cid, "name": cname, "restaurants": {}})
        dates.append(p["date"])
        if p.get("empty"):
            continue
        rmap = restaurant_ids.get(cid, {})
        for col, rname in enumerate(p["headers"]):
            rid = rmap.get(rname, slug(rname))
            if rid not in camp["restaurants"]:
                camp["restaurants"][rid] = {
                    "id": rid,
                    "name": rname,
                    "hours": hours.get(cid, {}).get(rid),
                    "hasMenu": False,
                }
            meals = []
            for mtype, cells in p["rows"]:
                cell = cells[col] if col < len(cells) else None
                if not cell:
                    continue
                items = [it.to_json() for it in parse_cell(cell)]
                if items:
                    meals.append({"type": mtype, "items": items})
            if meals:
                camp["restaurants"][rid]["hasMenu"] = True
                entries.append({"date": p["date"], "campus": cid, "restaurant": rid, "meals": meals})

    if not campuses:
        raise ScrapeError("캠퍼스/날짜 패널을 하나도 찾지 못했습니다 (페이지 구조 변경?)")

    order = {cid: i for i, cid in enumerate(campuses)}
    entries.sort(key=lambda e: (e["date"], order[e["campus"]]))
    return {
        "version": SCHEMA_VERSION,
        "school": school,
        "source": source,
        "generatedAt": now.isoformat(timespec="seconds"),
        "range": {"from": min(dates), "to": max(dates)} if dates else None,
        "campuses": [
            {"id": c["id"], "name": c["name"], "restaurants": list(c["restaurants"].values())}
            for c in campuses.values()
        ],
        "entries": entries,
    }


# ---------------------------------------------------------------- 저장


def _dump(path: Path, data, compact: bool = False) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if compact:  # 앱이 매번 받는 파일은 작게
        text = json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n"
    else:  # 기록 파일은 사람이 읽기 좋게
        text = json.dumps(data, ensure_ascii=False, indent=1) + "\n"
    if not path.exists() or path.read_text(encoding="utf-8") != text:
        path.write_text(text, encoding="utf-8")


def save(result: dict, out_dir: Path) -> dict:
    """latest.json + 날짜별 기록(days/) + 기록 목록(history.json) 저장."""
    school_dir = out_dir / result["school"]
    _dump(school_dir / "latest.json", result, compact=True)

    by_date: dict[str, list[dict]] = {}
    for e in result["entries"]:
        by_date.setdefault(e["date"], []).append(e)
    for date, entries in by_date.items():
        # 학교가 식단을 고치면 그날 기록도 최신으로 덮어쓴다.
        _dump(school_dir / "days" / f"{date}.json", {"date": date, "school": result["school"], "entries": entries})

    days = sorted(p.stem for p in (school_dir / "days").glob("*.json")) if (school_dir / "days").exists() else []
    _dump(school_dir / "history.json", {"school": result["school"], "days": days})
    return {"entries": len(result["entries"]), "days_with_menu": len(by_date), "history_days": len(days)}


def save_status(out_dir: Path, school: str, ok: bool, message: str, now: datetime, stats: dict | None = None) -> None:
    path = out_dir / school / "status.json"
    prev = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    status = {
        "school": school,
        "ok": ok,
        "message": message,
        "lastAttempt": now.isoformat(timespec="seconds"),
        "lastSuccess": now.isoformat(timespec="seconds") if ok else prev.get("lastSuccess"),
        "stats": stats or prev.get("stats"),
    }
    _dump(path, status)
