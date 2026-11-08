"""고려대(안암) 어댑터 테스트 (실제 식당 페이지 표 발췌)."""

from __future__ import annotations

import unittest
from datetime import datetime
from pathlib import Path

from scraper.core import KST, build_result
from scraper.schools import ku

FIX = Path(__file__).parent / "fixtures" / "ku_tables.html"


def build():
    panels, hours = ku.collect(FIX)
    return build_result(
        school="ku",
        source=ku.SOURCE_URL,
        panels=panels,
        campus_ids=ku.CAMPUS_IDS,
        restaurant_ids=ku.RESTAURANT_IDS,
        hours=hours,
        now=datetime(2026, 11, 9, 7, 13, tzinfo=KST),
    )


class KuTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.r = build()

    def entry(self, date, rid):
        for e in self.r["entries"]:
            if (e["date"], e["campus"], e["restaurant"]) == (date, "anam", rid):
                return e
        return None

    def items(self, date, rid, meal):
        e = self.entry(date, rid)
        for m in e["meals"]:
            if m["type"] == meal:
                return m["items"]
        return None

    def test_four_restaurants_on_monday(self):
        for rid in ("student", "aegineung", "sanhak", "dorm"):
            self.assertIsNotNone(self.entry("2026-11-09", rid), rid)
        self.assertEqual([r["name"] for r in self.r["campuses"][0]["restaurants"]], ["학생회관", "애기능생활관", "산학관", "안암학사"])

    def test_student_hall_breakfast_and_origin_lines_removed(self):
        b = self.items("2026-11-09", "student", "조식")
        self.assertEqual([i["name"] for i in b], ["천원의아침밥", "천원의아침밥"])
        self.assertEqual(b[0]["price"], 1000)
        self.assertEqual(b[0]["sides"][0], "마제덮밥")
        self.assertNotIn("(돈육:국내산)", b[0]["sides"])
        self.assertEqual(b[1]["notes"], ["테이크아웃"])
        lunch = self.items("2026-11-09", "student", "중식")
        self.assertEqual([i["name"] for i in lunch], ["한식반상", "일품반상"])
        self.assertEqual(lunch[0]["sides"][:3], ["부대찌개", "흑미밥", "해물파전"])
        self.assertTrue(all(not s.startswith("(") for i in lunch for s in i["sides"]))

    def test_aegineung_venues_prices_and_split(self):
        lunch = self.items("2026-11-09", "aegineung", "중식")
        self.assertEqual([(i["name"], i["price"]) for i in lunch], [("학생식당", 6500), ("교직원식당", 7500)])
        self.assertEqual(lunch[0]["sides"][:3], ["마제덮밥", "사모사", "뭇국"])
        self.assertEqual(lunch[0]["notes"], ["사이드메뉴: 크림새우"])
        bf = self.items("2026-11-09", "aegineung", "조식")
        self.assertEqual(bf[0]["name"], "천원의아침밥")
        self.assertEqual(bf[0]["sides"][0], "A 치킨마요컵밥 두유or생수 삶은계란")

    def test_sanhak_titles_and_rowspan(self):
        lunch = self.items("2026-11-09", "sanhak", "중식")
        self.assertEqual([i["name"] for i in lunch], ["중식A", "중식B"])
        dinner = self.items("2026-11-09", "sanhak", "석식")
        self.assertEqual(dinner[0]["name"], "석식")
        self.assertEqual([i["name"] for i in self.items("2026-11-13", "sanhak", "중식")], ["중식B"])

    def test_dorm_price_line_and_quotes(self):
        lunch = self.items("2026-11-09", "dorm", "중식")
        self.assertEqual((lunch[0]["name"], lunch[0]["price"]), ("중식", 6000))
        self.assertEqual(lunch[0]["sides"][0], "순두부찌개")
        wed = self.items("2026-11-11", "dorm", "중식")
        self.assertEqual(wed[0]["price"], 8000)
        self.assertEqual(wed[0]["sides"][:2], ["치즈함박스테이크", "&토마토파스타"])
        bf = self.items("2026-11-09", "dorm", "조식")
        self.assertEqual((bf[0]["name"], bf[0]["price"]), ("천원의아침밥", 1000))
        self.assertEqual(bf[0]["sides"], ["돈육불고기 컵밥 세트", "or 김치치즈 주먹밥 세트"])

    def test_empty_rows_and_weekend(self):
        lunch = self.items("2026-11-13", "student", "중식")
        self.assertEqual([i["name"] for i in lunch], ["한식반상"])
        self.assertIsNone(self.entry("2026-11-14", "student"))
        self.assertEqual(self.r["range"]["to"], "2026-11-14")


if __name__ == "__main__":
    unittest.main()
