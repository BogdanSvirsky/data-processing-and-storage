from datetime import date

import asyncpg

from app.entities import RoutePath, Schedule, Segment


class RouteRepository:
    def __init__(self, conn: asyncpg.Connection):
        self._conn = conn

    async def find_inbound_schedules(
            self, airport_code: str) -> list[Schedule]:
        rows = await self._conn.fetch(
            "SELECT days_of_week, route_no, departure_airport "
            "FROM bookings.routes WHERE arrival_airport = $1 "
            "ORDER BY scheduled_time + duration",
            airport_code,
        )
        return [
            Schedule(
                days_of_week=r["days_of_week"],
                flight_no=r["route_no"],
                origin=r["departure_airport"],
            )
            for r in rows
        ]

    async def find_outbound_schedules(
            self, airport_code: str) -> list[Schedule]:
        rows = await self._conn.fetch(
            "SELECT days_of_week, route_no, arrival_airport "
            "FROM bookings.routes WHERE departure_airport = $1 "
            "ORDER BY scheduled_time",
            airport_code,
        )
        return [
            Schedule(
                days_of_week=r["days_of_week"],
                flight_no=r["route_no"],
                destination=r["arrival_airport"],
            )
            for r in rows
        ]

    async def search_flight_paths(
        self,
        fare_conditions: str,
        from_codes: list[str],
        departure_date: date,
        max_connections: int | None,
        to_codes: list[str],
    ) -> list[RoutePath]:
        route_rows = await self._conn.fetch("""
            WITH RECURSIVE path AS (
                SELECT
                    t.flight_id,
                    t.departure_airport::text,
                    t.arrival_airport::text,
                    t.scheduled_departure_local,
                    t.scheduled_arrival_local,
                    0 AS connections,
                    ARRAY[t.departure_airport::text, t.arrival_airport::text] AS visited,
                    COALESCE(pr.price, 0) AS total_price,
                    ARRAY[t.flight_id] AS flight_ids,
                    ARRAY[COALESCE(pr.price, 0)] AS segment_prices
                FROM bookings.timetable t
                LEFT JOIN bookings.pricing_rules pr
                    ON pr.route_no = t.route_no AND pr.fare_conditions = $1
                WHERE t.departure_airport = ANY($2::text[])
                  AND t.scheduled_departure_local >= $3::date::timestamp
                  AND t.scheduled_departure_local < $3::date::timestamp + INTERVAL '1 day'
                  AND t.status != 'Cancelled'
                  AND EXISTS (
                      SELECT 1 FROM bookings.seats s
                      WHERE s.airplane_code = t.airplane_code AND s.fare_conditions = $1
                  )

                UNION ALL

                SELECT
                    t.flight_id,
                    t.departure_airport::text,
                    t.arrival_airport::text,
                    t.scheduled_departure_local,
                    t.scheduled_arrival_local,
                    p.connections + 1,
                    p.visited || t.arrival_airport::text,
                    p.total_price + COALESCE(pr.price, 0),
                    p.flight_ids || t.flight_id,
                    p.segment_prices || COALESCE(pr.price, 0)
                FROM path p
                JOIN bookings.timetable t
                    ON t.departure_airport = p.arrival_airport
                LEFT JOIN bookings.pricing_rules pr
                    ON pr.route_no = t.route_no AND pr.fare_conditions = $1
                WHERE t.scheduled_departure_local >= $3::date::timestamp
                  AND t.scheduled_departure_local < $3::date::timestamp + INTERVAL '1 day'
                  AND t.scheduled_departure_local > p.scheduled_arrival_local
                  AND NOT (t.arrival_airport = ANY(p.visited))
                  AND t.status != 'Cancelled'
                  AND ($4::int IS NULL OR p.connections + 1 <= $4)
                  AND EXISTS (
                      SELECT 1 FROM bookings.seats s
                      WHERE s.airplane_code = t.airplane_code AND s.fare_conditions = $1
                  )
            )
            SELECT connections, total_price, flight_ids, segment_prices
            FROM path
            WHERE arrival_airport = ANY($5::text[])
            ORDER BY connections ASC, total_price ASC
        """, fare_conditions, from_codes, departure_date, max_connections, to_codes)

        if not route_rows:
            return []

        all_flight_ids = list(
            {fid for r in route_rows for fid in r["flight_ids"]})

        flight_rows = await self._conn.fetch("""
            SELECT flight_id, route_no,
                   departure_airport::text AS departure_airport,
                   arrival_airport::text AS arrival_airport,
                   scheduled_departure_local, scheduled_arrival_local
            FROM bookings.timetable
            WHERE flight_id = ANY($1::int[])
        """, all_flight_ids)

        flights = {r["flight_id"]: r for r in flight_rows}

        result = []
        for r in route_rows:
            segments = [
                Segment(
                    flight_id=fid,
                    route_no=flights[fid]["route_no"],
                    departure_airport=flights[fid]["departure_airport"],
                    arrival_airport=flights[fid]["arrival_airport"],
                    scheduled_departure=flights[fid]["scheduled_departure_local"],
                    scheduled_arrival=flights[fid]["scheduled_arrival_local"],
                    price=price,
                )
                for fid, price in zip(r["flight_ids"], r["segment_prices"])
            ]
            result.append(RoutePath(
                connections=r["connections"],
                total_price=float(r["total_price"]),
                segments=segments,
            ))
        return result
