from datetime import datetime

import asyncpg

from app.entities import CheckInResult
from app.exceptions import (
    SeatAlreadyTakenError,
    SeatNotAvailableError,
    TicketNotFoundError,
)


class CheckinRepository:
    def __init__(self, conn: asyncpg.Connection):
        self._conn = conn

    async def check_in(
        self,
        ticket_no: str,
        flight_id: int,
        seat_no: str,
    ) -> CheckInResult:
        async with self._conn.transaction(isolation='repeatable_read'):
            segment = await self._conn.fetchrow(
                "SELECT flight_id, fare_conditions FROM bookings.segments "
                "WHERE ticket_no = $1 AND flight_id = $2 FOR UPDATE",
                ticket_no,
                flight_id,)
            if not segment:
                raise TicketNotFoundError(
                    f"Ticket {ticket_no} not found for flight {flight_id}")

            await self._conn.fetchval(
                "SELECT 1 FROM bookings.flights WHERE flight_id = $1 FOR UPDATE",
                flight_id,)

            airplane_code = await self._conn.fetchval(
                """
                SELECT r.airplane_code
                FROM bookings.flights f
                JOIN bookings.routes r ON r.route_no = f.route_no
                    AND r.validity @> f.scheduled_departure
                WHERE f.flight_id = $1
                """,
                flight_id,)

            seat_ok = await self._conn.fetchval(
                "SELECT 1 FROM bookings.seats "
                "WHERE airplane_code = $1 AND seat_no = $2 AND fare_conditions = $3",
                airplane_code,
                seat_no,
                segment["fare_conditions"],)
            if not seat_ok:
                raise SeatNotAvailableError(
                    f"Seat {seat_no} not available or wrong fare class")

            taken = await self._conn.fetchval(
                "SELECT 1 FROM bookings.boarding_passes "
                "WHERE flight_id = $1 AND seat_no = $2 FOR UPDATE",
                flight_id,
                seat_no,)
            if taken:
                raise SeatAlreadyTakenError(f"Seat {seat_no} already taken")

            boarding_no = await self._conn.fetchval(
                "SELECT COALESCE(MAX(boarding_no), 0) + 1 FROM bookings.boarding_passes "
                "WHERE flight_id = $1",
                flight_id,)

            now = datetime.now()
            await self._conn.execute(
                "INSERT INTO bookings.boarding_passes "
                "(ticket_no, flight_id, seat_no, boarding_no, boarding_time) "
                "VALUES ($1, $2, $3, $4, $5)",
                ticket_no,
                flight_id,
                seat_no,
                boarding_no,
                now,)

            return CheckInResult(
                ticket_no=ticket_no,
                flight_id=flight_id,
                seat_no=seat_no,
                boarding_no=boarding_no,
                boarding_time=now,)
