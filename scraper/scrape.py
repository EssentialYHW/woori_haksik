"""우리학식 수집기.

    python -m scraper.scrape                       # 동아대 사이트에서 받아 data/ 에 저장
    python -m scraper.scrape --html page.html      # 저장해 둔 페이지로 테스트
    python -m scraper.scrape --cells cells.json --out app/src/main/assets/sample   # 앱 샘플 데이터 만들기

실패하면 종료 코드 1 → GitHub Actions 가 실패 알림 메일을 보낸다.
"""

from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime
from pathlib import Path

from scraper.core import KST, ScrapeError, build_result, save, save_status
from scraper.schools import donga

ROOT = Path(__file__).resolve().parent.parent
SCHOOLS = {"donga": donga}


def run(school_id: str, out_dir: Path, html_file: Path | None, cells_file: Path | None) -> dict:
    mod = SCHOOLS[school_id]
    now = datetime.now(KST)
    try:
        if cells_file:
            panels, hours = json.loads(cells_file.read_text(encoding="utf-8")), {}
            # 샘플용: 운영시간은 테스트 HTML에서 가져온다.
            fixture = ROOT / "scraper" / "tests" / "fixtures" / "donga_menu.html"
            if fixture.exists():
                hours = mod.parse_html(fixture.read_text(encoding="utf-8"))[1]
        else:
            html = html_file.read_text(encoding="utf-8") if html_file else mod.fetch()
            panels, hours = mod.parse_html(html)
        result = build_result(
            school=mod.SCHOOL_ID,
            source=mod.SOURCE_URL,
            panels=panels,
            campus_ids=mod.CAMPUS_IDS,
            restaurant_ids=mod.RESTAURANT_IDS,
            hours=hours,
            now=now,
        )
        if not result["entries"]:
            raise ScrapeError("식단이 한 건도 없습니다 (방학이거나 페이지 구조 변경)")
        stats = save(result, out_dir)
        save_status(out_dir, mod.SCHOOL_ID, True, "ok", now, stats)
        return stats
    except ScrapeError as e:
        save_status(out_dir, mod.SCHOOL_ID, False, str(e), now)
        raise


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="우리학식 수집기")
    ap.add_argument("--school", default="donga", choices=sorted(SCHOOLS))
    ap.add_argument("--out", type=Path, default=ROOT / "data")
    ap.add_argument("--html", type=Path, help="저장해 둔 페이지 HTML로 테스트")
    ap.add_argument("--cells", type=Path, help="중간 형태(JSON) 패널로 만들기")
    args = ap.parse_args(argv)
    try:
        stats = run(args.school, args.out, args.html, args.cells)
    except ScrapeError as e:
        print(f"[실패] {args.school}: {e}", file=sys.stderr)
        return 1
    print(f"[완료] {args.school}: {stats}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
