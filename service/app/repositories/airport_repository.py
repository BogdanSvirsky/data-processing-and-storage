from typing import Any

import asyncpg

from app.entities import Airport, City, Coordinates


class AirportRepository:
    def __init__(self, conn: asyncpg.Connection):
        self._conn = conn

    @staticmethod
    def _parse_coordinates(raw: Any) -> Coordinates:
        if isinstance(raw, str):
            raw = raw.strip("()")
            parts = raw.split(",")
            return Coordinates(lon=float(parts[0].strip()), lat=float(parts[1].strip()))
        if hasattr(raw, "x"):
            return Coordinates(lon=float(raw.x), lat=float(raw.y))
        if isinstance(raw, (tuple, list)):
            return Coordinates(lon=float(raw[0]), lat=float(raw[1]))
        return Coordinates(lon=0.0, lat=0.0)

    async def find_cities(self) -> list[City]:
        rows = await self._conn.fetch(
            "SELECT DISTINCT city ->> 'en' AS city, country ->> 'en' AS country "
            "FROM bookings.airports_data ORDER BY city"
        )
        return [City(city=r["city"], country=r["country"]) for r in rows]

    async def find_airports_by_city(self, city_name: str) -> list[Airport]:
        rows = await self._conn.fetch(
            "SELECT airport_code, airport_name ->> 'en' AS airport_name, "
            "city ->> 'en' AS city, country ->> 'en' AS country, coordinates, timezone "
            "FROM bookings.airports_data WHERE city ->> 'en' = $1 ORDER BY airport_code",
            city_name,
        )
        return [
            Airport(
                airport_code=r["airport_code"],
                airport_name=r["airport_name"],
                city=r["city"],
                country=r["country"],
                coordinates=self._parse_coordinates(r["coordinates"]),
                timezone=r["timezone"],
            )
            for r in rows
        ]

    async def find_all(self) -> list[Airport]:
        rows = await self._conn.fetch(
            "SELECT airport_code, airport_name ->> 'en' AS airport_name, "
            "city ->> 'en' AS city, country ->> 'en' AS country, coordinates, timezone "
            "FROM bookings.airports_data ORDER BY airport_code"
        )
        return [
            Airport(
                airport_code=r["airport_code"],
                airport_name=r["airport_name"],
                city=r["city"],
                country=r["country"],
                coordinates=self._parse_coordinates(r["coordinates"]),
                timezone=r["timezone"],
            )
            for r in rows
        ]

    async def find_airport_codes_by_city(self, city_name: str) -> list[str]:
        rows = await self._conn.fetch(
            "SELECT DISTINCT airport_code "
            "FROM bookings.airports_data "
            "WHERE city ->> 'en' = $1 OR city ->> 'ru' = $1",
            city_name,
        )
        return [r["airport_code"] for r in rows]
