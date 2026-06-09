from datetime import datetime
from decimal import Decimal

import asyncpg

from app.entities import BookingResult
from app.exceptions import FlightNotAvailableError


class BookingRepository:
    def __init__(self, conn: asyncpg.Connection):
        self._conn = conn

    async def create_booking(
        self,
        passenger_id: str,
        passenger_name: str,
        flight_ids: list[int],
        fare_conditions: str,
    ) -> BookingResult:
        async with self._conn.transaction(isolation='repeatable_read'):
            rows = await self._conn.fetch(
                """
                SELECT f.flight_id, COALESCE(pr.price, 0) AS price
                FROM bookings.flights f
                JOIN bookings.routes r ON r.route_no = f.route_no
                    AND r.validity @> f.scheduled_departure
                LEFT JOIN bookings.pricing_rules pr
                    ON pr.route_no = f.route_no AND pr.fare_conditions = $2
                WHERE f.flight_id = ANY($1)
                  AND f.status IN ('Scheduled', 'On Time', 'Delayed', 'Boarding')
                FOR UPDATE OF f
                """,
                flight_ids,
                fare_conditions,
            )

            if len(rows) != len(flight_ids):
                raise FlightNotAvailableError

            total_amount = sum(Decimal(str(r["price"])) for r in rows)
            now = datetime.now()

            book_ref = await self._conn.fetchval(
                "SELECT upper(substr(md5(random()::text), 1, 6))"
            )
            ticket_no = await self._conn.fetchval(
                "SELECT '5552' || lpad(nextval('bookings.ticket_no_seq')::text, 9, '0')"
            )

            await self._conn.execute(
                "INSERT INTO bookings.bookings (book_ref, book_date, total_amount) "
                "VALUES ($1, $2, $3)",
                book_ref, now, total_amount,
            )
            await self._conn.execute(
                "INSERT INTO bookings.tickets "
                "(ticket_no, book_ref, passenger_id, passenger_name, outbound) "
                "VALUES ($1, $2, $3, $4, true)",
                ticket_no, book_ref, passenger_id, passenger_name,
            )
            for r in rows:
                await self._conn.execute(
                    "INSERT INTO bookings.segments "
                    "(ticket_no, flight_id, fare_conditions, price) "
                    "VALUES ($1, $2, $3, $4)",
                    ticket_no, r["flight_id"], fare_conditions, r["price"],
                )

            return BookingResult(
                book_ref=book_ref,
                ticket_no=ticket_no,
                total_amount=float(total_amount),
                book_date=now,
            )
