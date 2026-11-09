"""python -m unittest discover -s scraper/tests -t ."""

from __future__ import annotations

import json
import tempfile
import unittest
from datetime import datetime
from pathlib import Path

from scraper.core import KST, build_result, parse_cell, save
from scraper.schools import donga

FIX = Path(__file__).parent / "fixtures"


def build(panels, hours=None):
    return build_result(
        school="donga",
        source=donga.SOURCE_URL,
        panels=panels,
        campus_ids=donga.CAMPUS_IDS,
        restaurant_ids=donga.RESTAURANT_IDS,
        hours=hours or {},
        now=datetime(2026, 11, 9, 7, 13, tzinfo=KST),
    )


def entry(result, date, campus, rid):
    for e in result["entries"]:
        if (e["date"], e["campus"], e["restaurant"]) == (date, campus, rid):
            return e
    return None


class ParseCellTest(unittest.TestCase):
    def test_set_meal_with_self_bar(self):
        items = parse_cell(["정식(7000)", "열무된장국", "돈육고추장불고기", "", "*셀프바", "통살오징어튀김", "배추김치"])
        self.assertEqual(len(items), 1)
        it = items[0].to_json()
        self.assertEqual(it["name"], "정식")
        self.assertEqual(it["price"], 7000)
        self.assertEqual(it["sides"], ["열무된장국", "돈육고추장불고기"])
        self.assertEqual(it["extras"], [{"label": "셀프바", "items": ["통살오징어튀김", "배추김치"]}])

    def test_breakfast_then_priced_items(self):
        items = [i.to_json() for i in parse_cell(
            ["천원의아침밥", "[09:00~선착순제공]", "치즈돈까스", "", "돼지국밥(6500)", "뚝)삼겹김치짜글이/계란후라이밥(6500)"]
        )]
        self.assertEqual([i["name"] for i in items], ["천원의아침밥", "돼지국밥", "뚝)삼겹김치짜글이/계란후라이밥"])
        self.assertEqual(items[0]["price"], 1000)
        self.assertEqual(items[0]["sides"], ["치즈돈까스"])
        self.assertEqual(items[0]["notes"], ["09:00~선착순제공"])
        self.assertEqual(items[2]["price"], 6500)

    def test_sub_venue(self):
        items = [i.to_json() for i in parse_cell(
            ["닭강정(4900)", "", "*학생회관 다방(1층카페)", "키토김밥(4500)", "크래미키토김밥(5500)"]
        )]
        self.assertEqual(len(items), 3)
        self.assertNotIn("venue", items[0])
        self.assertEqual(items[1]["venue"], "학생회관 다방(1층카페)")
        self.assertEqual(items[2]["venue"], "학생회관 다방(1층카페)")

    def test_event_block_and_star_note(self):
        items = [i.to_json() for i in parse_cell(
            ["<통일밥상>", "*10시40분부터 선착순 무료제공*", "왕만두조랭이떡국", "해주비빔밥", "", "등심돈까스(5000)"]
        )]
        self.assertEqual(items[0]["name"], "통일밥상")
        self.assertEqual(items[0]["notes"], ["10시40분부터 선착순 무료제공"])
        self.assertEqual(items[0]["price"], 0)
        self.assertEqual(items[0]["sides"], ["왕만두조랭이떡국", "해주비빔밥"])
        self.assertEqual(items[1]["name"], "등심돈까스")

    def test_truncated_price_is_still_an_item(self):
        items = [i.to_json() for i in parse_cell(["반계탕/공기밥(7000)", "바지락칼국수/미니공기밥(650"])]
        self.assertEqual(len(items), 2)
        self.assertEqual(items[1]["name"], "바지락칼국수/미니공기밥")
        self.assertNotIn("price", items[1])

    def test_unpriced_single_item(self):
        self.assertEqual(parse_cell(["행사식"])[0].to_json(), {"name": "행사식"})


class HtmlFixtureTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        panels, hours = donga.parse_html((FIX / "donga_menu.html").read_text(encoding="utf-8"))
        cls.panels, cls.hours = panels, hours
        cls.result = build(panels, hours)

    def test_three_seunghak_restaurants_on_monday(self):
        for rid in ("faculty", "student", "library"):
            self.assertIsNotNone(entry(self.result, "2026-11-09", "seunghak", rid), rid)

    def test_campuses_and_names(self):
        camps = {c["id"]: c for c in self.result["campuses"]}
        self.assertEqual(set(camps), {"seunghak", "gudeok-bumin"})
        names = [r["name"] for r in camps["seunghak"]["restaurants"]]
        self.assertEqual(names, ["교수회관", "학생회관", "도서관"])

    def test_hours(self):
        camps = {c["id"]: c for c in self.result["campuses"]}
        rest = {r["id"]: r for r in camps["seunghak"]["restaurants"]}
        self.assertEqual(rest["student"]["hours"]["open"], "09:00")
        self.assertEqual(rest["student"]["hours"]["breaks"], [{"from": "09:30", "to": "10:00"}, {"from": "14:30", "to": "15:00"}])
        self.assertEqual(rest["faculty"]["hours"]["breaks"], [])

    def test_weekend_empty(self):
        self.assertIsNone(entry(self.result, "2026-11-14", "seunghak", "student"))
        self.assertEqual(self.result["range"], {"from": "2026-11-09", "to": "2026-11-14"})

    def test_meal_types(self):
        e = entry(self.result, "2026-11-09", "seunghak", "student")
        self.assertEqual([m["type"] for m in e["meals"]], ["정식", "일품"])
        e = entry(self.result, "2026-11-10", "seunghak", "student")
        self.assertEqual([m["type"] for m in e["meals"]], ["정식", "양분식"])

    def test_ampersand_decoded(self):
        e = entry(self.result, "2026-11-09", "seunghak", "student")
        names = [i["name"] for m in e["meals"] for i in m["items"]]
        self.assertIn("폴드포크타코샐러드&또띠아", names)

    def test_save_writes_history(self):
        with tempfile.TemporaryDirectory() as d:
            stats = save(self.result, Path(d))
            self.assertTrue((Path(d) / "donga" / "days" / "2026-11-09.json").exists())
            hist = json.loads((Path(d) / "donga" / "history.json").read_text(encoding="utf-8"))
            self.assertEqual(hist["days"], ["2026-11-09", "2026-11-10"])
            self.assertEqual(stats["days_with_menu"], 2)


class TwoWeekCellsTest(unittest.TestCase):
    """실제 페이지에서 뽑은 2주치(11/2~11/13) 원문으로 전체 점검."""

    @classmethod
    def setUpClass(cls):
        cls.result = build(json.loads((FIX / "donga_cells_2026-11-02_13.json").read_text(encoding="utf-8")))

    def test_every_weekday_has_three_seunghak_restaurants(self):
        weekdays = ["2026-11-0%d" % d for d in (2, 3, 4, 5, 6, 9)] + ["2026-11-1%d" % d for d in (0, 1, 2, 3)]
        for date in weekdays:
            for rid in ("faculty", "student", "library"):
                self.assertIsNotNone(entry(self.result, date, "seunghak", rid), f"{date} {rid}")

    def test_no_item_with_empty_name(self):
        for e in self.result["entries"]:
            for m in e["meals"]:
                for it in m["items"]:
                    self.assertTrue(it["name"], e)
                    self.assertFalse(it["name"].startswith(("*", "[", "<")), it)

    def test_gudeok_student_hall_never_has_menu(self):
        camps = {c["id"]: c for c in self.result["campuses"]}
        rest = {r["id"]: r for r in camps["gudeok-bumin"]["restaurants"]}
        self.assertFalse(rest["student"]["hasMenu"])
        self.assertTrue(rest["bumin"]["hasMenu"])


if __name__ == "__main__":
    unittest.main()
