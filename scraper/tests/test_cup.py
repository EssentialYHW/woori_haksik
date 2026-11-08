"""부산가톨릭대 어댑터 테스트 (실제 API 응답 2주치)."""

from __future__ import annotations

import tempfile
import unittest
from datetime import datetime
from pathlib import Path

from scraper.core import KST, build_result, save
from scraper.schools import cup

FIX = Path(__file__).parent / "fixtures" / "cup_2026-11-02_13.json"


def build():
    panels, hours = cup.collect(FIX)
    return build_result(
        school="cup",
        source=cup.SOURCE_URL,
        panels=panels,
        campus_ids=cup.CAMPUS_IDS,
        restaurant_ids=cup.RESTAURANT_IDS,
        hours=hours,
        now=datetime(2026, 11, 9, 7, 13, tzinfo=KST),
    )


class CupTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.r = build()

    def entry(self, date, rid):
        for e in self.r["entries"]:
            if (e["date"], e["campus"], e["restaurant"]) == (date, "main", rid):
                return e
        return None

    def test_staff_every_weekday(self):
        for d in ["2026-11-02", "2026-11-03", "2026-11-04", "2026-11-05", "2026-11-06",
                  "2026-11-09", "2026-11-10", "2026-11-11", "2026-11-12", "2026-11-13"]:
            self.assertIsNotNone(self.entry(d, "staff"), d)

    def test_set_meal_shape(self):
        e = self.entry("2026-11-09", "staff")
        self.assertEqual(len(e["meals"]), 1)
        self.assertEqual(e["meals"][0]["type"], "정식")
        item = e["meals"][0]["items"][0]
        self.assertEqual(item["name"], "정식")
        self.assertNotIn("price", item)
        self.assertEqual(item["sides"][:3], ["백미밥/혼합잡곡밥", "시락국", "제육볶음"])
        self.assertEqual(len(item["sides"]), 8)

    def test_other_restaurants_have_no_menu(self):
        rest = {r["id"]: r for r in self.r["campuses"][0]["restaurants"]}
        self.assertEqual(set(rest), {"student", "staff", "veritas"})
        self.assertTrue(rest["staff"]["hasMenu"])
        self.assertFalse(rest["student"]["hasMenu"])
        self.assertFalse(rest["veritas"]["hasMenu"])
        self.assertEqual(rest["staff"]["hours"]["open"], "11:50")

    def test_single_campus_and_range(self):
        self.assertEqual([c["id"] for c in self.r["campuses"]], ["main"])
        self.assertEqual(self.r["range"], {"from": "2026-11-02", "to": "2026-11-16"})
        self.assertIsNone(self.entry("2026-11-07", "staff"))

    def test_priced_cell_is_kept_as_items(self):
        p = cup.panel_from_json("2026-11-09", {"f_memo4": "돈까스(5000)\r\n우동(4000)"})
        self.assertEqual(p["rows"][0][1][0], ["돈까스(5000)", "우동(4000)"])
        self.assertEqual(cup.panel_from_json("2026-11-09", {}), {"date": "2026-11-09", "campus": "부산가톨릭대학교", "empty": True})

    def test_save(self):
        with tempfile.TemporaryDirectory() as d:
            stats = save(self.r, Path(d))
            self.assertEqual(stats["days_with_menu"], 10)
            self.assertTrue((Path(d) / "cup" / "latest.json").exists())


if __name__ == "__main__":
    unittest.main()
